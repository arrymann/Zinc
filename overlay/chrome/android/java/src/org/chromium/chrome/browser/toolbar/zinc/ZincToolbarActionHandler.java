// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnLongClickListener;

import androidx.annotation.IdRes;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.toolbar.ToolbarTabController;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;
import org.chromium.chrome.browser.ui.appmenu.AppMenuDelegate;

/**
 * Runs the browser function behind each Zinc toolbar button.
 *
 * <p>Wherever possible a button triggers the same menu action as the corresponding app menu item,
 * so it behaves exactly like the built-in feature (including metrics, policies and incognito
 * handling). Navigation and the tab switcher use the same controllers as the stock buttons.
 */
@NullMarked
public class ZincToolbarActionHandler implements ZincToolbarButtonsCoordinator.ActionDelegate {
    private final AppMenuDelegate mAppMenuDelegate;
    private final ToolbarTabController mTabController;
    private final @Nullable Runnable mOpenTabSwitcher;
    private final @Nullable OnLongClickListener mTabSwitcherLongClickListener;

    /**
     * @param appMenuDelegate Handles app menu actions, e.g. the activity.
     * @param tabController Navigates the current tab.
     * @param openTabSwitcher Opens the tab switcher, if one is available.
     * @param tabSwitcherLongClickListener Shows the tab switcher's long-press menu, if any.
     */
    public ZincToolbarActionHandler(
            AppMenuDelegate appMenuDelegate,
            ToolbarTabController tabController,
            @Nullable Runnable openTabSwitcher,
            @Nullable OnLongClickListener tabSwitcherLongClickListener) {
        mAppMenuDelegate = appMenuDelegate;
        mTabController = tabController;
        mOpenTabSwitcher = openTabSwitcher;
        mTabSwitcherLongClickListener = tabSwitcherLongClickListener;
    }

    @Override
    public void onActionClicked(ZincAction action, View button) {
        switch (action) {
            case BACK:
                mTabController.back();
                return;
            case FORWARD:
                mTabController.forward();
                return;
            case RELOAD:
                mTabController.stopOrReloadCurrentTab(/* ignoreCache= */ false);
                return;
            case HOME:
                mTabController.openHomepage();
                return;
            case TAB_SWITCHER:
                if (mOpenTabSwitcher != null) mOpenTabSwitcher.run();
                return;
            default:
                int menuItemId = getMenuItemId(action);
                assert menuItemId != Resources.ID_NULL : "No menu action for " + action;
                mAppMenuDelegate.onOptionsItemSelected(menuItemId, /* menuItemData= */ null);
        }
    }

    @Override
    public boolean onActionLongClicked(ZincAction action, View button) {
        switch (action) {
            case TAB_SWITCHER:
                return mTabSwitcherLongClickListener != null
                        && mTabSwitcherLongClickListener.onLongClick(button);
            case RELOAD:
                // Long-press reload performs a hard reload, like Shift+Reload on desktop.
                mTabController.stopOrReloadCurrentTab(/* ignoreCache= */ true);
                return true;
            case HOME:
                mTabController.openHomepageInNewTab(/* foregroundNewTab= */ true);
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the app menu item id that performs {@code action}, or {@link Resources#ID_NULL} for
     * actions that are handled directly by this class.
     */
    static @IdRes int getMenuItemId(ZincAction action) {
        switch (action) {
            case NEW_TAB:
                return R.id.new_tab_menu_id;
            case NEW_INCOGNITO_TAB:
                return R.id.new_incognito_tab_menu_id;
            case CLOSE_TAB:
                return R.id.close_tab;
            case RECENT_TABS:
                return R.id.recent_tabs_menu_id;
            case SHARE:
                return R.id.share_menu_id;
            case BOOKMARK_PAGE:
                return R.id.bookmark_this_page_id;
            case FIND_IN_PAGE:
                return R.id.find_in_page_id;
            case DESKTOP_SITE:
                return R.id.request_desktop_site_id;
            case TRANSLATE:
                return R.id.translate_id;
            case READER_MODE:
                return R.id.reader_mode_menu_id;
            case PRINT:
                return R.id.print_id;
            case ADD_TO_HOME_SCREEN:
                return R.id.universal_install;
            case PAGE_INFO:
                return R.id.info_menu_id;
            case COPY_LINK:
                return R.id.copy_link_menu_id;
            case BOOKMARKS:
                return R.id.all_bookmarks_menu_id;
            case HISTORY:
                return R.id.open_history_menu_id;
            case DOWNLOADS:
                return R.id.downloads_menu_id;
            case QUICK_DELETE:
                return R.id.quick_delete_menu_id;
            case SETTINGS:
                return R.id.preferences_id;
            case EXTENSIONS:
                return R.id.extensions_menu_menu_id;
            case MANAGE_EXTENSIONS:
                return R.id.manage_extensions_menu_id;
            case WEB_STORE:
                return R.id.extensions_webstore_menu_id;
            case BACK:
            case FORWARD:
            case RELOAD:
            case HOME:
            case TAB_SWITCHER:
                return Resources.ID_NULL;
        }
        return Resources.ID_NULL;
    }
}
