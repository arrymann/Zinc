# How Zinc works

Zinc changes three things in Chromium for Android: it compiles in the
desktop extensions platform, it makes the phone toolbar customizable and puts
it at the bottom, and it shows pinned extensions in that toolbar. This page
explains each part and where its code lives.

## Desktop extensions on a phone

Chromium has been porting its extensions system to Android for "desktop
Android" devices. In the pinned release (M157) the port is far along: the
extensions service, `chrome://extensions`, Web Store installs, extension
popups, the extensions (puzzle) menu, pinning, context menus and keyboard
shortcuts all exist. They are compiled when `enable_extensions_core` is true,
which `is_desktop_android = true` turns on through
`enable_desktop_android_extensions` (`extensions/buildflags/buildflags.gni`).

Zinc builds with `is_desktop_android = true` (see `args/`) rather than
flipping the lower-level flag alone, because that is the configuration
upstream builds and tests. Two things keep it a phone browser:

- Whether a device *is* a desktop is decided at runtime:
  `DeviceInfo.isDesktop()` requires the `android.hardware.type.pc` system
  feature, so phones still get the phone toolbar, layouts and behaviour.
- Patch `0002` defaults the features that the build flag enables for desktop
  UI (WebUI NTP, WebUI theming, the side panel) back to off.

The remaining runtime gates are already satisfied on phones:
`ExtensionUi.isEnabled()` returns true whenever the extensions backend is
compiled, and `ChromeTabbedActivity` creates a `ChromeAndroidTask` on every
device, which the extensions toolbar needs.

The Chrome Web Store decides whether to offer "Add to Chrome" from the user
agent, so patch `0003` makes `DesktopSiteUtils.shouldOverrideDesktopSite()`
request the desktop site for `chromewebstore.google.com` (and the legacy
`chrome.google.com/webstore`).

## The toolbar

![Toolbar layout](images/toolbar-layout.svg)

The phone toolbar (`ToolbarPhone`, layout `toolbar_phone.xml`) is the same
view whether the address bar is at the top or the bottom;
`ToolbarPositionController` moves the whole control container. Patch `0004`
changes the default position to the bottom, both in Java
(`AddressBarPreference.DEFAULT_POSITION_AND_SOURCE`) and in the native
local-state pref `omnibox.is_in_bottom_position`.

Patch `0005` adds three views to `toolbar_phone.xml`:

```
ToolbarPhone (FrameLayout)
├── zinc_start_buttons        LinearLayout, gravity start   ← START buttons
├── location_bar              the address box
└── toolbar_buttons           LinearLayout, gravity end
    ├── (stock tab switcher, hidden)
    ├── zinc_end_buttons      LinearLayout                  ← END buttons
    ├── extensions_toolbar_container_stub                   ← pinned extensions + puzzle
    └── menu_button                                         ← ⋮
```

`ToolbarPhone` lays out the address box between whatever is on its left and
right. Zinc adds the start buttons' width to the left bound
(`getBoundsAfterAccountingForLeftButton()`); the end buttons and extensions
are inside `toolbar_buttons`, whose width already forms the right bound. The
stock home button, tab switcher and adaptive "shortcut" button are hidden
because the customizable buttons replace them (`ZINC_REPLACES_STOCK_BUTTONS`).

Other `ToolbarPhone` details handled by the patch:

- **URL focus**: the start buttons hide with the other toolbar buttons and all
  Zinc views fade in both the legacy animator and the transition-based
  animation path.
- **Texture capture**: the compositor draws a bitmap of the toolbar while
  scrolling and in the tab switcher (`drawWithoutBackground()`); the Zinc
  buttons and extensions are drawn into it.
- **Theme**: tint, incognito ripple and the incognito tab count follow the
  toolbar like the stock buttons do.

### Customizable buttons

All Zinc toolbar code is in the overlay package
`org.chromium.chrome.browser.toolbar.zinc`
(`overlay/chrome/browser/ui/android/toolbar/java/src/...`):

| Class | Role |
| --- | --- |
| `core.ZincAction` | Every pinnable browser function, grouped in categories. Pure Java. |
| `core.ZincToolbarConfig` | The layout: up to 4 buttons, each an action and a side. Immutable; all edits (count, side, move, swap) keep it valid. Pure Java. |
| `core.ZincToolbarWidthPlanner` | How much width pinned extensions may take. Pure Java. |
| `ZincToolbarPrefs` | Stores the config in SharedPreferences (key `Chrome.Zinc.ToolbarConfig`) and notifies observers. |
| `ZincToolbarButtonsCoordinator` | Creates the buttons in the two containers, keeps icons and enabled state in sync with the current tab, forwards clicks. |
| `ZincActionResources` | Icon, label and view id for each action. |
| `settings.*` | Settings > Toolbar: preview, button count, side split, per-button editing, extensions button switch. |

The config is stored as a versioned string, for example
`v1:back@start,tab_switcher@end`. Parsing is lenient: unknown actions,
duplicates and extra buttons are dropped, and anything unreadable falls back
to the default, so a bad value can never break the toolbar. Storing it in
SharedPreferences (not the profile's PrefService) lets the buttons be created
when the toolbar is inflated, before native code is loaded, so the stock
buttons never flash at startup.

`ZincToolbarButtonsCoordinator` is created by `ToolbarPhone` on inflation and
becomes interactive when `ToolbarManager.initializeWithNative()` calls
`initializeWithNative()` with the current-tab supplier and a
`ZincToolbarActionHandler` (in `chrome/android`, overlay). It observes the
current tab to update Back/Forward enabled state, Reload ↔ Stop and the
desktop-site toggle, and `ToolbarPhone.updateBookmarkButton()` updates the
bookmark star. Config changes from Settings arrive through
`ZincToolbarPrefs` and rebuild the buttons immediately.

`ZincToolbarActionHandler` runs each function the same way the app menu does:
it calls `AppMenuDelegate.onOptionsItemSelected()` with the menu item id
(`R.id.share_menu_id`, `R.id.find_in_page_id`, ...), so a button behaves
exactly like the menu item, including policies, incognito handling and
metrics. Back, Forward, Reload, Home and the tab switcher use the same
controllers as the stock buttons. Long presses: Back and Forward show the
navigation history popup, Reload does a hard reload, Home opens the homepage
in a new tab, and the tab switcher shows its tab menu.

### Pinned extensions

Upstream's `ExtensionsToolbarCoordinator` (pinned action list, request-access
button, puzzle menu with pin toggles, context menus with Pin/Unpin, drag to
reorder) is only attached to the tablet toolbar. Patch `0005`:

- adds `extensions_toolbar_container_stub` to `toolbar_phone.xml` between the
  end buttons and the app menu;
- makes `ToolbarManager` pass the toolbar layout as a plain `ViewGroup` instead
  of casting it to `ToolbarTablet`, so the coordinator is created for phones;
- implements `ToolbarPhone.setExtensionsToolbarCoordinator()`.

Phones are narrow, so `ToolbarPhone.onMeasure()` gives the extensions only the
width that is left after the customizable buttons, the menu and a minimum
address box width (`zinc_min_location_bar_width`, 144 dp, or 35% of the bar on
wide screens). That budget is offered to the extension width consumers in the
same priority order as on tablets: the popped-out action, the puzzle button,
the request-access button, then as many pinned actions as fit. The rest stay
reachable from the puzzle menu. Whether the puzzle button is always visible is
the upstream `extensions.pin_extensions_menu_button` pref (default on), which
Settings > Toolbar exposes as *Show Extensions button*.

## Tests

- `standalone/` compiles `zinc.core` with stub annotations and runs its JUnit
  tests with Gradle, without Chromium. The same test files are listed in the
  toolbar's `junit` target, so they also run as part of Chromium's
  `chrome_junit_tests`.
- `ZincToolbarActionHandlerUnitTest` (Robolectric, in Chromium) checks that
  every action has a handler and is dispatched correctly.
- `DesktopSiteUtilsUnitTest` gains tests for the Web Store override.
- `scripts/verify_patches.sh` applies everything to a sparse clone of the
  pinned Chromium and runs `tools/check_zinc_tree.py`, which checks that the
  files listed in GN, the strings in `android_chrome_strings.grd` and the
  resources used by Zinc all exist.
