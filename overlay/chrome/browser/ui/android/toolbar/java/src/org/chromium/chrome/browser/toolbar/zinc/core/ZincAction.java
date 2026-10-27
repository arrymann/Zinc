// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;

/**
 * The browser functions that can be pinned to a Zinc toolbar button.
 *
 * <p>Each action has a stable {@link #getKey() key} that is used when persisting the toolbar
 * configuration, so keys must never be renamed or reused. New actions can be appended freely;
 * configurations that reference an unknown key simply drop that button when parsed.
 *
 * <p>This class is pure Java so that it can be unit tested outside of a Chromium checkout.
 */
@NullMarked
public enum ZincAction {
    BACK("back", Category.NAVIGATION),
    FORWARD("forward", Category.NAVIGATION),
    RELOAD("reload", Category.NAVIGATION),
    HOME("home", Category.NAVIGATION),

    NEW_TAB("new_tab", Category.TABS),
    NEW_INCOGNITO_TAB("new_incognito_tab", Category.TABS),
    TAB_SWITCHER("tab_switcher", Category.TABS),
    CLOSE_TAB("close_tab", Category.TABS),
    RECENT_TABS("recent_tabs", Category.TABS),

    SHARE("share", Category.PAGE),
    BOOKMARK_PAGE("bookmark_page", Category.PAGE),
    FIND_IN_PAGE("find_in_page", Category.PAGE),
    DESKTOP_SITE("desktop_site", Category.PAGE),
    TRANSLATE("translate", Category.PAGE),
    READER_MODE("reader_mode", Category.PAGE),
    PRINT("print", Category.PAGE),
    ADD_TO_HOME_SCREEN("add_to_home_screen", Category.PAGE),
    PAGE_INFO("page_info", Category.PAGE),
    COPY_LINK("copy_link", Category.PAGE),

    BOOKMARKS("bookmarks", Category.BROWSER),
    HISTORY("history", Category.BROWSER),
    DOWNLOADS("downloads", Category.BROWSER),
    QUICK_DELETE("quick_delete", Category.BROWSER),
    SETTINGS("settings", Category.BROWSER),

    EXTENSIONS("extensions", Category.EXTENSIONS),
    MANAGE_EXTENSIONS("manage_extensions", Category.EXTENSIONS),
    WEB_STORE("web_store", Category.EXTENSIONS);

    /** Groups actions in the settings picker. */
    public enum Category {
        NAVIGATION,
        TABS,
        PAGE,
        BROWSER,
        EXTENSIONS
    }

    private final String mKey;
    private final Category mCategory;

    ZincAction(String key, Category category) {
        mKey = key;
        mCategory = category;
    }

    /** Returns the stable identifier used to persist this action. */
    public String getKey() {
        return mKey;
    }

    public Category getCategory() {
        return mCategory;
    }

    /**
     * Returns whether this action operates on the current tab, and is therefore unavailable when
     * there is no tab (for example while the tab switcher is showing with no tabs open).
     */
    public boolean requiresTab() {
        switch (mCategory) {
            case NAVIGATION:
            case PAGE:
                return true;
            default:
                return this == CLOSE_TAB;
        }
    }

    /** Returns the action with the given persisted key, or null if the key is unknown. */
    public static @Nullable ZincAction fromKey(@Nullable String key) {
        if (key == null) return null;
        for (ZincAction action : values()) {
            if (action.mKey.equals(key)) return action;
        }
        return null;
    }
}
