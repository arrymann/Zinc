// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.widget.ImageViewCompat;

import org.chromium.base.Callback;
import org.chromium.base.supplier.MonotonicObservableSupplier;
import org.chromium.base.supplier.NullableObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.tab.CurrentTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.theme.ThemeUtils;
import org.chromium.chrome.browser.toolbar.R;
import org.chromium.chrome.browser.toolbar.top.NavigationPopup;
import org.chromium.chrome.browser.toolbar.top.NavigationPopup.HistoryDelegate;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarConfig;
import org.chromium.chrome.browser.ui.android.bars_common.TabSwitcherDrawable;
import org.chromium.chrome.browser.ui.android.bars_common.TabSwitcherDrawable.TabSwitcherDrawableLocation;
import org.chromium.chrome.browser.ui.theme.BrandedColorScheme;
import org.chromium.content_public.browser.NavigationHandle;
import org.chromium.content_public.browser.WebContents;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the customizable buttons of the Zinc phone toolbar.
 *
 * <p>The toolbar view hosts two containers: one before the address box and one after it (ahead of
 * the extensions and the app menu). This class fills them according to {@link ZincToolbarPrefs},
 * keeps every button's icon, enabled state and description in sync with the current tab, and
 * forwards clicks to an {@link ActionDelegate} that knows how to run each browser function.
 *
 * <p>Buttons are created as soon as the toolbar is inflated (before native is ready) so the layout
 * does not jump at startup; they become interactive once {@link #initializeWithNative} is called.
 */
@NullMarked
public class ZincToolbarButtonsCoordinator implements ZincToolbarPrefs.Observer {
    /** Runs the browser function behind a button. Implemented by the activity layer. */
    public interface ActionDelegate {
        /** Called when the button for {@code action} is clicked. */
        void onActionClicked(ZincAction action, View button);

        /**
         * Called when the button for {@code action} is long-pressed and the toolbar did not
         * handle it itself.
         *
         * @return Whether the long press was consumed. If not, the button's tooltip is shown.
         */
        boolean onActionLongClicked(ZincAction action, View button);
    }

    private final Context mContext;
    private final ViewGroup mStartContainer;
    private final ViewGroup mEndContainer;
    private final Runnable mOnLayoutChanged;
    private final Map<ZincAction, ImageButton> mButtons = new EnumMap<>(ZincAction.class);
    private final Callback<Integer> mTabCountObserver = count -> updateTabSwitcherButton();
    private final TabObserver mTabObserver =
            new TabObserver() {
                @Override
                public void onLoadStarted(Tab tab, boolean toDifferentDocument) {
                    if (toDifferentDocument) updateButtonStates();
                }

                @Override
                public void onLoadStopped(Tab tab, boolean toDifferentDocument) {
                    updateButtonStates();
                }

                @Override
                public void onUrlUpdated(Tab tab) {
                    updateButtonStates();
                }

                @Override
                public void onDidFinishNavigationInPrimaryMainFrame(
                        Tab tab, NavigationHandle navigation) {
                    updateButtonStates();
                }

                @Override
                public void onNavigationEntriesDeleted(Tab tab) {
                    updateButtonStates();
                }

                @Override
                public void onContentChanged(Tab tab) {
                    updateButtonStates();
                }
            };

    private ZincToolbarConfig mConfig;
    private @Nullable ActionDelegate mDelegate;
    private @Nullable NullableObservableSupplier<Tab> mTabSupplier;
    private @Nullable CurrentTabObserver mCurrentTabObserver;
    private @Nullable HistoryDelegate mHistoryDelegate;
    private @Nullable MonotonicObservableSupplier<Integer> mTabCountSupplier;
    private @Nullable TabSwitcherDrawable mTabSwitcherDrawable;
    private @Nullable ColorStateList mTint;
    private @BrandedColorScheme int mBrandedColorScheme = BrandedColorScheme.APP_DEFAULT;
    private @DrawableRes int mButtonBackground;
    private boolean mIsIncognito;
    private boolean mIsBookmarked;
    private boolean mBookmarkEditingAllowed = true;
    private boolean mInteractive;
    private boolean mObservingPrefs;
    private boolean mDestroyed;

    /**
     * @param context The toolbar's context.
     * @param startContainer The container laid out before the address box.
     * @param endContainer The container laid out after the address box.
     * @param onLayoutChanged Called when buttons were added or removed so the toolbar can
     *     re-layout the address box.
     */
    public ZincToolbarButtonsCoordinator(
            Context context,
            ViewGroup startContainer,
            ViewGroup endContainer,
            Runnable onLayoutChanged) {
        mContext = context;
        mStartContainer = startContainer;
        mEndContainer = endContainer;
        mOnLayoutChanged = onLayoutChanged;
        mConfig = ZincToolbarPrefs.getConfig();
        rebuildButtons();

        // Only follow settings changes while the toolbar is on screen, so a toolbar that is
        // inflated but never shown (e.g. a discarded warm-up view hierarchy) is not leaked.
        mStartContainer.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View view) {
                        startObservingPrefs();
                    }

                    @Override
                    public void onViewDetachedFromWindow(View view) {
                        stopObservingPrefs();
                    }
                });
        if (mStartContainer.isAttachedToWindow()) startObservingPrefs();
    }

    /**
     * Supplies the delegate used to show navigation history when Back or Forward is long-pressed.
     */
    public void setHistoryDelegate(HistoryDelegate historyDelegate) {
        mHistoryDelegate = historyDelegate;
    }

    /** Makes the buttons interactive. Must be called once, after native initialization. */
    public void initializeWithNative(
            NullableObservableSupplier<Tab> tabSupplier, ActionDelegate delegate) {
        assert mDelegate == null : "initializeWithNative() called twice";
        mDelegate = delegate;
        mTabSupplier = tabSupplier;
        mCurrentTabObserver =
                new CurrentTabObserver(tabSupplier, mTabObserver, tab -> updateButtonStates());
        mInteractive = true;
        updateButtonStates();
    }

    /** Supplies the number of tabs shown on the tab switcher button. */
    public void setTabCountSupplier(MonotonicObservableSupplier<Integer> tabCountSupplier) {
        if (mTabCountSupplier != null) mTabCountSupplier.removeObserver(mTabCountObserver);
        mTabCountSupplier = tabCountSupplier;
        mTabCountSupplier.addSyncObserverAndPostIfNonNull(mTabCountObserver);
    }

    /** Applies the toolbar's current icon tint. */
    public void setTint(
            @Nullable ColorStateList tint, @BrandedColorScheme int brandedColorScheme) {
        mTint =
                tint != null
                        ? tint
                        : ThemeUtils.getThemedToolbarIconTint(mContext, brandedColorScheme);
        mBrandedColorScheme = brandedColorScheme;
        for (ImageButton button : mButtons.values()) applyTint(button);
        if (mTabSwitcherDrawable != null) {
            mTabSwitcherDrawable.setTint(
                    ThemeUtils.getThemedToolbarIconTint(mContext, brandedColorScheme));
            mTabSwitcherDrawable.setNotificationBackground(brandedColorScheme);
        }
    }

    /** Applies the toolbar's ripple background, which differs between regular and incognito. */
    public void setButtonBackground(@DrawableRes int backgroundResource) {
        mButtonBackground = backgroundResource;
        for (ImageButton button : mButtons.values()) {
            button.setBackgroundResource(backgroundResource);
        }
    }

    /** Updates the incognito state shown by the tab switcher button. */
    public void setIncognito(boolean isIncognito) {
        if (mIsIncognito == isIncognito) return;
        mIsIncognito = isIncognito;
        updateTabSwitcherButton();
    }

    /** Updates the bookmark star. Called by the toolbar when the current page's state changes. */
    public void setBookmarkState(boolean isBookmarked, boolean editingAllowed) {
        mIsBookmarked = isBookmarked;
        mBookmarkEditingAllowed = editingAllowed;
        updateButtonStates();
    }

    /** Returns the layout currently shown. */
    public ZincToolbarConfig getConfig() {
        return mConfig;
    }

    public ViewGroup getStartContainer() {
        return mStartContainer;
    }

    public ViewGroup getEndContainer() {
        return mEndContainer;
    }

    /** Returns whether any button sits before the address box. */
    public boolean hasStartButtons() {
        return mConfig.getStartCount() > 0;
    }

    /** Returns whether any button sits after the address box. */
    public boolean hasEndButtons() {
        return mConfig.getButtonCount() > mConfig.getStartCount();
    }

    /**
     * Returns the width the start buttons occupy. Computed from the buttons' fixed layout sizes
     * rather than the last measure pass, so it is correct even right after the layout changed.
     */
    public int getStartWidth() {
        return getContainerWidth(mStartContainer);
    }

    /** Returns the width the end buttons occupy. See {@link #getStartWidth()}. */
    public int getEndWidth() {
        return getContainerWidth(mEndContainer);
    }

    /** Returns the view for {@code action}, or null if that action is not pinned. */
    public @Nullable View getButtonView(ZincAction action) {
        return mButtons.get(action);
    }

    @Override
    public void onToolbarConfigChanged(ZincToolbarConfig config) {
        if (mDestroyed || config.equals(mConfig)) return;
        mConfig = config;
        rebuildButtons();
        mOnLayoutChanged.run();
    }

    public void destroy() {
        if (mDestroyed) return;
        mDestroyed = true;
        stopObservingPrefs();
        if (mCurrentTabObserver != null) {
            mCurrentTabObserver.destroy();
            mCurrentTabObserver = null;
        }
        if (mTabCountSupplier != null) {
            mTabCountSupplier.removeObserver(mTabCountObserver);
            mTabCountSupplier = null;
        }
        mDelegate = null;
        mTabSupplier = null;
    }

    private void startObservingPrefs() {
        if (mObservingPrefs || mDestroyed) return;
        mObservingPrefs = true;
        ZincToolbarPrefs.addObserver(this);
        // Pick up changes made while the toolbar was detached.
        onToolbarConfigChanged(ZincToolbarPrefs.getConfig());
    }

    private void stopObservingPrefs() {
        if (!mObservingPrefs) return;
        mObservingPrefs = false;
        ZincToolbarPrefs.removeObserver(this);
    }

    private void rebuildButtons() {
        mStartContainer.removeAllViews();
        mEndContainer.removeAllViews();
        mButtons.clear();
        mTabSwitcherDrawable = null;

        addButtons(mConfig.getStartButtons(), mStartContainer);
        addButtons(mConfig.getEndButtons(), mEndContainer);
        mStartContainer.setVisibility(hasStartButtons() ? View.VISIBLE : View.GONE);
        mEndContainer.setVisibility(hasEndButtons() ? View.VISIBLE : View.GONE);

        updateTabSwitcherButton();
        updateButtonStates();
    }

    private void addButtons(List<ZincToolbarButton> buttons, ViewGroup container) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        for (ZincToolbarButton config : buttons) {
            ZincAction action = config.getAction();
            ImageButton button =
                    (ImageButton) inflater.inflate(R.layout.zinc_toolbar_button, container, false);
            button.setId(ZincActionResources.getViewId(action));
            if (mButtonBackground != 0) button.setBackgroundResource(mButtonBackground);
            if (action == ZincAction.TAB_SWITCHER) {
                mTabSwitcherDrawable =
                        TabSwitcherDrawable.createTabSwitcherDrawable(
                                mContext,
                                mBrandedColorScheme,
                                TabSwitcherDrawableLocation.TAB_TOOLBAR);
                // The drawable tints itself, including the tab count text.
                ImageViewCompat.setImageTintList(button, null);
                button.setImageDrawable(mTabSwitcherDrawable);
            } else {
                button.setImageResource(ZincActionResources.getIcon(action));
                applyTint(button);
            }
            String label = mContext.getString(ZincActionResources.getLabel(action));
            button.setContentDescription(label);
            TooltipCompat.setTooltipText(button, label);
            button.setOnClickListener(view -> onButtonClicked(action, view));
            button.setOnLongClickListener(view -> onButtonLongClicked(action, view));
            container.addView(button);
            mButtons.put(action, button);
        }
    }

    private void applyTint(ImageButton button) {
        if (mTint == null || button.getDrawable() == mTabSwitcherDrawable) return;
        ImageViewCompat.setImageTintList(button, mTint);
    }

    private void onButtonClicked(ZincAction action, View view) {
        if (!mInteractive || mDelegate == null) return;
        mDelegate.onActionClicked(action, view);
    }

    private boolean onButtonLongClicked(ZincAction action, View view) {
        if (!mInteractive || mDelegate == null) return false;
        if ((action == ZincAction.BACK || action == ZincAction.FORWARD)
                && showNavigationPopup(action == ZincAction.FORWARD, view)) {
            return true;
        }
        return mDelegate.onActionLongClicked(action, view);
    }

    @SuppressWarnings("NullAway") // Supplier<@Nullable> - https://crbug.com/455874046
    private boolean showNavigationPopup(boolean forward, View anchor) {
        Tab tab = getCurrentTab();
        if (tab == null || mHistoryDelegate == null || mTabSupplier == null) return false;
        WebContents webContents = tab.getWebContents();
        if (webContents == null) return false;
        if (forward ? !tab.canGoForward() : !tab.canGoBack()) return false;
        new NavigationPopup(
                        tab.getProfile(),
                        mContext,
                        webContents.getNavigationController(),
                        forward
                                ? NavigationPopup.Type.TABLET_FORWARD
                                : NavigationPopup.Type.TABLET_BACK,
                        mTabSupplier,
                        mHistoryDelegate)
                .show(anchor);
        return true;
    }

    private @Nullable Tab getCurrentTab() {
        if (mTabSupplier == null) return null;
        Tab tab = mTabSupplier.get();
        return tab != null && tab.isInitialized() && !tab.isDestroyed() ? tab : null;
    }

    private void updateTabSwitcherButton() {
        if (mTabSwitcherDrawable == null) return;
        int count = 0;
        if (mTabCountSupplier != null) {
            Integer supplied = mTabCountSupplier.get();
            if (supplied != null) count = supplied;
        }
        mTabSwitcherDrawable.updateForTabCount(count, mIsIncognito);
        ImageButton button = mButtons.get(ZincAction.TAB_SWITCHER);
        if (button == null) return;
        String description =
                mContext.getResources()
                        .getQuantityString(
                                R.plurals.accessibility_toolbar_btn_tabswitcher_toggle_default,
                                count,
                                count);
        button.setContentDescription(description);
        TooltipCompat.setTooltipText(button, description);
    }

    /** Refreshes enabled states and stateful icons from the current tab. */
    private void updateButtonStates() {
        if (mDestroyed) return;
        Tab tab = getCurrentTab();
        for (Map.Entry<ZincAction, ImageButton> entry : mButtons.entrySet()) {
            ZincAction action = entry.getKey();
            ImageButton button = entry.getValue();
            boolean enabled = !mInteractive || !action.requiresTab() || tab != null;
            switch (action) {
                case BACK:
                    enabled &= tab == null || tab.canGoBack();
                    break;
                case FORWARD:
                    enabled &= tab == null || tab.canGoForward();
                    break;
                case RELOAD:
                    updateReloadButton(button, tab != null && tab.isLoading());
                    break;
                case BOOKMARK_PAGE:
                    enabled &= mBookmarkEditingAllowed;
                    updateToggleButton(
                            button,
                            mIsBookmarked,
                            R.drawable.zinc_ic_bookmark_page_filled,
                            ZincActionResources.getIcon(action),
                            R.string.zinc_action_bookmark_page_edit,
                            ZincActionResources.getLabel(action));
                    break;
                case DESKTOP_SITE:
                    updateToggleButton(
                            button,
                            isUsingDesktopSite(tab),
                            R.drawable.zinc_ic_desktop_site_filled,
                            ZincActionResources.getIcon(action),
                            R.string.zinc_action_desktop_site_on,
                            ZincActionResources.getLabel(action));
                    break;
                default:
                    break;
            }
            button.setEnabled(enabled);
        }
    }

    private void updateReloadButton(ImageButton button, boolean isLoading) {
        int icon =
                isLoading
                        ? R.drawable.zinc_ic_stop
                        : ZincActionResources.getIcon(ZincAction.RELOAD);
        int label =
                isLoading
                        ? R.string.zinc_action_stop
                        : ZincActionResources.getLabel(ZincAction.RELOAD);
        button.setImageResource(icon);
        applyTint(button);
        String description = mContext.getString(label);
        button.setContentDescription(description);
        TooltipCompat.setTooltipText(button, description);
    }

    private void updateToggleButton(
            ImageButton button,
            boolean isOn,
            int onIcon,
            int offIcon,
            int onLabel,
            int offLabel) {
        button.setImageResource(isOn ? onIcon : offIcon);
        applyTint(button);
        String description = mContext.getString(isOn ? onLabel : offLabel);
        button.setContentDescription(description);
        TooltipCompat.setTooltipText(button, description);
    }

    private static int getContainerWidth(ViewGroup container) {
        if (container.getVisibility() == View.GONE) return 0;
        int width = container.getPaddingLeft() + container.getPaddingRight();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            width +=
                    Math.max(params.width, child.getMeasuredWidth())
                            + params.leftMargin
                            + params.rightMargin;
        }
        return width;
    }

    private static boolean isUsingDesktopSite(@Nullable Tab tab) {
        if (tab == null) return false;
        WebContents webContents = tab.getWebContents();
        return webContents != null
                && webContents.getNavigationController().getUseDesktopUserAgent();
    }
}
