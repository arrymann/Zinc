// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc;

import android.content.SharedPreferences;

import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.ThreadUtils;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.preferences.ChromePreferenceKeys;
import org.chromium.chrome.browser.preferences.ChromeSharedPreferences;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarConfig;

/**
 * Persists the Zinc toolbar layout and notifies observers when it changes.
 *
 * <p>The layout lives in Chrome's app-level SharedPreferences (not the profile's PrefService) so
 * that the toolbar can be built with the right buttons before the native library is loaded, which
 * avoids the stock buttons flashing during startup.
 */
@NullMarked
public final class ZincToolbarPrefs {
    /** Notified on the UI thread whenever the stored toolbar layout changes. */
    public interface Observer {
        void onToolbarConfigChanged(ZincToolbarConfig config);
    }

    private static final ObserverList<Observer> sObservers = new ObserverList<>();

    // SharedPreferences only keeps a weak reference to its listeners, so hold this strongly.
    private static SharedPreferences.@Nullable OnSharedPreferenceChangeListener sListener;

    private ZincToolbarPrefs() {}

    /** Returns the stored toolbar layout, or the default layout if none is stored. */
    public static ZincToolbarConfig getConfig() {
        return ZincToolbarConfig.parse(
                ChromeSharedPreferences.getInstance()
                        .readString(ChromePreferenceKeys.ZINC_TOOLBAR_CONFIG, null));
    }

    /** Stores a new toolbar layout. Live toolbars update through {@link Observer}. */
    public static void setConfig(ZincToolbarConfig config) {
        ChromeSharedPreferences.getInstance()
                .writeString(ChromePreferenceKeys.ZINC_TOOLBAR_CONFIG, config.serialize());
    }

    /** Restores the default toolbar layout. */
    public static void resetConfig() {
        ChromeSharedPreferences.getInstance().removeKey(ChromePreferenceKeys.ZINC_TOOLBAR_CONFIG);
    }

    public static void addObserver(Observer observer) {
        ThreadUtils.assertOnUiThread();
        if (sListener == null) {
            sListener =
                    (prefs, key) -> {
                        // A null key means the preferences were cleared.
                        if (key != null && !ChromePreferenceKeys.ZINC_TOOLBAR_CONFIG.equals(key)) {
                            return;
                        }
                        ZincToolbarConfig config = getConfig();
                        for (Observer o : sObservers) o.onToolbarConfigChanged(config);
                    };
            ContextUtils.getAppSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(sListener);
        }
        sObservers.addObserver(observer);
    }

    public static void removeObserver(Observer observer) {
        ThreadUtils.assertOnUiThread();
        sObservers.removeObserver(observer);
        if (sObservers.isEmpty() && sListener != null) {
            ContextUtils.getAppSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(sListener);
            sListener = null;
        }
    }
}
