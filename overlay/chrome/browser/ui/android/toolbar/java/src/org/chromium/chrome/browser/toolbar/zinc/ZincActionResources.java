// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import org.chromium.build.annotations.NullMarked;
import org.chromium.chrome.browser.toolbar.R;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;

/** Maps each {@link ZincAction} to its icon, label and view id. */
@NullMarked
public final class ZincActionResources {
    private ZincActionResources() {}

    /** Returns the toolbar icon for {@code action} in its default (off / idle) state. */
    public static @DrawableRes int getIcon(ZincAction action) {
        switch (action) {
            case BACK:
                return R.drawable.zinc_ic_back;
            case FORWARD:
                return R.drawable.zinc_ic_forward;
            case RELOAD:
                return R.drawable.zinc_ic_reload;
            case HOME:
                return R.drawable.zinc_ic_home;
            case NEW_TAB:
                return R.drawable.zinc_ic_new_tab;
            case NEW_INCOGNITO_TAB:
                return R.drawable.zinc_ic_new_incognito_tab;
            case TAB_SWITCHER:
                return R.drawable.zinc_ic_tab_switcher;
            case CLOSE_TAB:
                return R.drawable.zinc_ic_close_tab;
            case RECENT_TABS:
                return R.drawable.zinc_ic_recent_tabs;
            case SHARE:
                return R.drawable.zinc_ic_share;
            case BOOKMARK_PAGE:
                return R.drawable.zinc_ic_bookmark_page;
            case FIND_IN_PAGE:
                return R.drawable.zinc_ic_find_in_page;
            case DESKTOP_SITE:
                return R.drawable.zinc_ic_desktop_site;
            case TRANSLATE:
                return R.drawable.zinc_ic_translate;
            case READER_MODE:
                return R.drawable.zinc_ic_reader_mode;
            case PRINT:
                return R.drawable.zinc_ic_print;
            case ADD_TO_HOME_SCREEN:
                return R.drawable.zinc_ic_add_to_home_screen;
            case PAGE_INFO:
                return R.drawable.zinc_ic_page_info;
            case COPY_LINK:
                return R.drawable.zinc_ic_copy_link;
            case BOOKMARKS:
                return R.drawable.zinc_ic_bookmarks;
            case HISTORY:
                return R.drawable.zinc_ic_history;
            case DOWNLOADS:
                return R.drawable.zinc_ic_downloads;
            case QUICK_DELETE:
                return R.drawable.zinc_ic_quick_delete;
            case SETTINGS:
                return R.drawable.zinc_ic_settings;
            case EXTENSIONS:
                return R.drawable.zinc_ic_extensions;
            case MANAGE_EXTENSIONS:
                return R.drawable.zinc_ic_manage_extensions;
            case WEB_STORE:
                return R.drawable.zinc_ic_web_store;
        }
        throw new IllegalArgumentException("Unknown action " + action);
    }

    /** Returns the user-visible name of {@code action}, also used as its accessibility label. */
    public static @StringRes int getLabel(ZincAction action) {
        switch (action) {
            case BACK:
                return R.string.zinc_action_back;
            case FORWARD:
                return R.string.zinc_action_forward;
            case RELOAD:
                return R.string.zinc_action_reload;
            case HOME:
                return R.string.zinc_action_home;
            case NEW_TAB:
                return R.string.zinc_action_new_tab;
            case NEW_INCOGNITO_TAB:
                return R.string.zinc_action_new_incognito_tab;
            case TAB_SWITCHER:
                return R.string.zinc_action_tab_switcher;
            case CLOSE_TAB:
                return R.string.zinc_action_close_tab;
            case RECENT_TABS:
                return R.string.zinc_action_recent_tabs;
            case SHARE:
                return R.string.zinc_action_share;
            case BOOKMARK_PAGE:
                return R.string.zinc_action_bookmark_page;
            case FIND_IN_PAGE:
                return R.string.zinc_action_find_in_page;
            case DESKTOP_SITE:
                return R.string.zinc_action_desktop_site;
            case TRANSLATE:
                return R.string.zinc_action_translate;
            case READER_MODE:
                return R.string.zinc_action_reader_mode;
            case PRINT:
                return R.string.zinc_action_print;
            case ADD_TO_HOME_SCREEN:
                return R.string.zinc_action_add_to_home_screen;
            case PAGE_INFO:
                return R.string.zinc_action_page_info;
            case COPY_LINK:
                return R.string.zinc_action_copy_link;
            case BOOKMARKS:
                return R.string.zinc_action_bookmarks;
            case HISTORY:
                return R.string.zinc_action_history;
            case DOWNLOADS:
                return R.string.zinc_action_downloads;
            case QUICK_DELETE:
                return R.string.zinc_action_quick_delete;
            case SETTINGS:
                return R.string.zinc_action_settings;
            case EXTENSIONS:
                return R.string.zinc_action_extensions;
            case MANAGE_EXTENSIONS:
                return R.string.zinc_action_manage_extensions;
            case WEB_STORE:
                return R.string.zinc_action_web_store;
        }
        throw new IllegalArgumentException("Unknown action " + action);
    }

    /** Returns the heading used to group actions of {@code category} in the picker. */
    public static @StringRes int getCategoryLabel(ZincAction.Category category) {
        switch (category) {
            case NAVIGATION:
                return R.string.zinc_action_category_navigation;
            case TABS:
                return R.string.zinc_action_category_tabs;
            case PAGE:
                return R.string.zinc_action_category_page;
            case BROWSER:
                return R.string.zinc_action_category_browser;
            case EXTENSIONS:
                return R.string.zinc_action_category_extensions;
        }
        throw new IllegalArgumentException("Unknown category " + category);
    }

    /** Returns a stable view id for the toolbar button of {@code action}. */
    public static @IdRes int getViewId(ZincAction action) {
        switch (action) {
            case BACK:
                return R.id.zinc_button_back;
            case FORWARD:
                return R.id.zinc_button_forward;
            case RELOAD:
                return R.id.zinc_button_reload;
            case HOME:
                return R.id.zinc_button_home;
            case NEW_TAB:
                return R.id.zinc_button_new_tab;
            case NEW_INCOGNITO_TAB:
                return R.id.zinc_button_new_incognito_tab;
            case TAB_SWITCHER:
                return R.id.zinc_button_tab_switcher;
            case CLOSE_TAB:
                return R.id.zinc_button_close_tab;
            case RECENT_TABS:
                return R.id.zinc_button_recent_tabs;
            case SHARE:
                return R.id.zinc_button_share;
            case BOOKMARK_PAGE:
                return R.id.zinc_button_bookmark_page;
            case FIND_IN_PAGE:
                return R.id.zinc_button_find_in_page;
            case DESKTOP_SITE:
                return R.id.zinc_button_desktop_site;
            case TRANSLATE:
                return R.id.zinc_button_translate;
            case READER_MODE:
                return R.id.zinc_button_reader_mode;
            case PRINT:
                return R.id.zinc_button_print;
            case ADD_TO_HOME_SCREEN:
                return R.id.zinc_button_add_to_home_screen;
            case PAGE_INFO:
                return R.id.zinc_button_page_info;
            case COPY_LINK:
                return R.id.zinc_button_copy_link;
            case BOOKMARKS:
                return R.id.zinc_button_bookmarks;
            case HISTORY:
                return R.id.zinc_button_history;
            case DOWNLOADS:
                return R.id.zinc_button_downloads;
            case QUICK_DELETE:
                return R.id.zinc_button_quick_delete;
            case SETTINGS:
                return R.id.zinc_button_settings;
            case EXTENSIONS:
                return R.id.zinc_button_extensions;
            case MANAGE_EXTENSIONS:
                return R.id.zinc_button_manage_extensions;
            case WEB_STORE:
                return R.id.zinc_button_web_store;
        }
        throw new IllegalArgumentException("Unknown action " + action);
    }
}
