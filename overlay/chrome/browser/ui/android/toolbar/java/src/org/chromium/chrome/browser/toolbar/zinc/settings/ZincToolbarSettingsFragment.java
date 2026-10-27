// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.chromium.base.supplier.MonotonicObservableSupplier;
import org.chromium.base.supplier.ObservableSuppliers;
import org.chromium.base.supplier.SettableMonotonicObservableSupplier;
import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.preferences.Pref;
import org.chromium.chrome.browser.settings.ChromeBaseSettingsFragment;
import org.chromium.chrome.browser.toolbar.R;
import org.chromium.chrome.browser.toolbar.settings.AddressBarPreference;
import org.chromium.chrome.browser.toolbar.zinc.ZincActionResources;
import org.chromium.chrome.browser.toolbar.zinc.ZincToolbarPrefs;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton.Side;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarConfig;
import org.chromium.chrome.browser.ui.extensions.ExtensionUi;
import org.chromium.components.browser_ui.settings.ChromeBaseListPreference;
import org.chromium.components.browser_ui.settings.ChromeBasePreference;
import org.chromium.components.browser_ui.settings.ChromeSwitchPreference;
import org.chromium.components.browser_ui.settings.SettingsUtils;
import org.chromium.components.user_prefs.UserPrefs;
import org.chromium.ui.base.LocalizationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings > Toolbar: choose how many buttons the Zinc toolbar has (up to four), which browser
 * function each one performs, and whether it sits left or right of the address box. Also controls
 * the extensions (puzzle) button that is used to pin and unpin extensions.
 */
@NullMarked
public class ZincToolbarSettingsFragment extends ChromeBaseSettingsFragment
        implements ZincToolbarPrefs.Observer {
    @VisibleForTesting static final String PREF_PREVIEW = "zinc_toolbar_preview";
    @VisibleForTesting static final String PREF_BUTTON_COUNT = "zinc_button_count";
    @VisibleForTesting static final String PREF_START_COUNT = "zinc_start_count";
    @VisibleForTesting static final String PREF_BUTTON_PREFIX = "zinc_button_";
    @VisibleForTesting static final String PREF_RESET = "zinc_reset";
    @VisibleForTesting static final String PREF_EXTENSIONS_SECTION = "zinc_extensions_section";
    @VisibleForTesting
    static final String PREF_SHOW_EXTENSIONS_BUTTON = "zinc_show_extensions_button";
    @VisibleForTesting static final String PREF_EXTENSIONS_HELP = "zinc_extensions_help";
    @VisibleForTesting static final String PREF_ADDRESS_BAR_POSITION = "zinc_address_bar_position";

    /** The button rows are ordered right after "Buttons left of the address bar" (order 3). */
    private static final int BUTTON_ROWS_FIRST_ORDER = 10;

    private final SettableMonotonicObservableSupplier<String> mPageTitle =
            ObservableSuppliers.createMonotonic();
    private final List<Preference> mButtonRows = new ArrayList<>();
    private ZincToolbarConfig mConfig = ZincToolbarConfig.DEFAULT;
    private @Nullable AlertDialog mDialog;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        SettingsUtils.addPreferencesFromResource(this, R.xml.zinc_toolbar_settings);
        mPageTitle.set(getTitle(getContext()));

        ChromeBaseListPreference countPref = findPreference(PREF_BUTTON_COUNT);
        countPref.setOnPreferenceChangeListener(
                (pref, newValue) -> {
                    updateConfig(mConfig.withButtonCount(Integer.parseInt((String) newValue)));
                    return true;
                });

        ChromeBaseListPreference startCountPref = findPreference(PREF_START_COUNT);
        startCountPref.setOnPreferenceChangeListener(
                (pref, newValue) -> {
                    updateConfig(mConfig.withStartCount(Integer.parseInt((String) newValue)));
                    return true;
                });

        findPreference(PREF_RESET)
                .setOnPreferenceClickListener(
                        pref -> {
                            ZincToolbarPrefs.resetConfig();
                            return true;
                        });

        setUpExtensionsSection();

        mConfig = ZincToolbarPrefs.getConfig();
        ZincToolbarPrefs.addObserver(this);
        bindConfig();
    }

    @Override
    public void onResume() {
        super.onResume();
        // The address bar position may have been changed on its own page.
        Preference positionPref = findPreference(PREF_ADDRESS_BAR_POSITION);
        positionPref.setSummary(
                AddressBarPreference.isToolbarConfiguredToShowOnTop()
                        ? R.string.address_bar_settings_top
                        : R.string.address_bar_settings_bottom);
    }

    @Override
    public void onDestroy() {
        ZincToolbarPrefs.removeObserver(this);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        super.onDestroy();
    }

    @Override
    public void onToolbarConfigChanged(ZincToolbarConfig config) {
        mConfig = config;
        bindConfig();
    }

    @Override
    public MonotonicObservableSupplier<String> getPageTitle() {
        return mPageTitle;
    }

    @Override
    public @AnimationType int getAnimationType() {
        return AnimationType.PROPERTY;
    }

    @Override
    public @Nullable String getMainMenuKey() {
        return "zinc_toolbar";
    }

    public static String getTitle(Context context) {
        return context.getString(R.string.zinc_settings_title);
    }

    private void setUpExtensionsSection() {
        boolean extensionsAvailable = ExtensionUi.isEnabled(getProfile());
        ChromeSwitchPreference showButtonPref = findPreference(PREF_SHOW_EXTENSIONS_BUTTON);
        if (!extensionsAvailable) {
            PreferenceScreen screen = getPreferenceScreen();
            screen.removePreference(findPreference(PREF_EXTENSIONS_SECTION));
            screen.removePreference(showButtonPref);
            screen.removePreference(findPreference(PREF_EXTENSIONS_HELP));
            return;
        }
        showButtonPref.setChecked(
                UserPrefs.get(getProfile()).getBoolean(Pref.PIN_EXTENSIONS_MENU_BUTTON));
        showButtonPref.setOnPreferenceChangeListener(
                (pref, newValue) -> {
                    UserPrefs.get(getProfile())
                            .setBoolean(Pref.PIN_EXTENSIONS_MENU_BUTTON, (boolean) newValue);
                    return true;
                });
    }

    private void updateConfig(ZincToolbarConfig config) {
        if (config.equals(mConfig)) return;
        mConfig = config;
        ZincToolbarPrefs.setConfig(config);
        bindConfig();
    }

    /** Refreshes every preference from {@link #mConfig}. */
    private void bindConfig() {
        ZincToolbarPreviewPreference preview = findPreference(PREF_PREVIEW);
        preview.setConfig(mConfig);

        int count = mConfig.getButtonCount();
        ChromeBaseListPreference countPref = findPreference(PREF_BUTTON_COUNT);
        countPref.setEntries(numberEntries(0, ZincToolbarConfig.MAX_BUTTONS));
        countPref.setEntryValues(numberEntries(0, ZincToolbarConfig.MAX_BUTTONS));
        countPref.setValue(Integer.toString(count));

        ChromeBaseListPreference startCountPref = findPreference(PREF_START_COUNT);
        startCountPref.setTitle(
                isRtl()
                        ? R.string.zinc_settings_start_count_rtl
                        : R.string.zinc_settings_start_count);
        startCountPref.setDialogTitle(startCountPref.getTitle());
        startCountPref.setEntries(numberEntries(0, count));
        startCountPref.setEntryValues(numberEntries(0, count));
        startCountPref.setValue(Integer.toString(mConfig.getStartCount()));
        startCountPref.setEnabled(count > 0);

        bindButtonRows();
    }

    private void bindButtonRows() {
        PreferenceScreen screen = getPreferenceScreen();
        for (Preference row : mButtonRows) screen.removePreference(row);
        mButtonRows.clear();

        Context context = getContext();
        for (int i = 0; i < mConfig.getButtonCount(); i++) {
            final int index = i;
            ZincToolbarButton button = mConfig.getButton(i);
            ChromeBasePreference row = new ChromeBasePreference(context);
            row.setKey(PREF_BUTTON_PREFIX + i);
            row.setOrder(BUTTON_ROWS_FIRST_ORDER + i);
            row.setIcon(ZincActionResources.getIcon(button.getAction()));
            row.setTitle(ZincActionResources.getLabel(button.getAction()));
            row.setSummary(
                    context.getString(
                            R.string.zinc_settings_button_summary,
                            i + 1,
                            context.getString(getSideLabel(button.getSide()))));
            row.setOnPreferenceClickListener(
                    pref -> {
                        showButtonOptions(index);
                        return true;
                    });
            screen.addPreference(row);
            mButtonRows.add(row);
        }
    }

    /** Shows what can be done with the button at {@code index}. */
    private void showButtonOptions(int index) {
        ZincToolbarButton button = mConfig.getButton(index);
        Context context = getContext();

        List<CharSequence> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        labels.add(context.getString(R.string.zinc_settings_change_function));
        actions.add(() -> showActionPicker(index));

        Side otherSide = button.getSide() == Side.START ? Side.END : Side.START;
        boolean otherSideIsLeft = (otherSide == Side.START) != isRtl();
        labels.add(
                context.getString(
                        otherSideIsLeft
                                ? R.string.zinc_settings_place_left
                                : R.string.zinc_settings_place_right));
        actions.add(() -> updateConfig(mConfig.withSide(index, otherSide)));

        // "Move left" / "Move right" walk the button through the bar, crossing the address box.
        // In right-to-left layouts the start of the bar is on the right.
        ZincToolbarConfig towardStart = mConfig.moveTowardStart(index);
        ZincToolbarConfig towardEnd = mConfig.moveTowardEnd(index);
        int moveTowardStartLabel =
                isRtl() ? R.string.zinc_settings_move_right : R.string.zinc_settings_move_left;
        int moveTowardEndLabel =
                isRtl() ? R.string.zinc_settings_move_left : R.string.zinc_settings_move_right;
        if (towardStart != mConfig) {
            labels.add(context.getString(moveTowardStartLabel));
            actions.add(() -> updateConfig(towardStart));
        }
        if (towardEnd != mConfig) {
            labels.add(context.getString(moveTowardEndLabel));
            actions.add(() -> updateConfig(towardEnd));
        }

        labels.add(context.getString(R.string.zinc_settings_remove_button));
        actions.add(() -> updateConfig(mConfig.remove(index)));

        showDialog(
                new AlertDialog.Builder(context, R.style.ThemeOverlay_BrowserUI_AlertDialog)
                        .setTitle(ZincActionResources.getLabel(button.getAction()))
                        .setItems(
                                labels.toArray(new CharSequence[0]),
                                (dialog, which) -> actions.get(which).run())
                        .setNegativeButton(android.R.string.cancel, null));
    }

    /** Shows every browser function, grouped by category, to pin to the button at index. */
    private void showActionPicker(int index) {
        Context context = getContext();
        ZincAction current = mConfig.getButton(index).getAction();
        ZincActionPickerAdapter adapter =
                new ZincActionPickerAdapter(context, current, mConfig.getButtons());
        showDialog(
                new AlertDialog.Builder(context, R.style.ThemeOverlay_BrowserUI_AlertDialog)
                        .setTitle(R.string.zinc_settings_change_function)
                        .setAdapter(
                                adapter,
                                (dialog, which) -> {
                                    ZincAction action = adapter.getActionAt(which);
                                    if (action != null) {
                                        updateConfig(mConfig.withAction(index, action));
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null));
    }

    private void showDialog(AlertDialog.Builder builder) {
        if (mDialog != null) mDialog.dismiss();
        mDialog = builder.create();
        mDialog.show();
    }

    private int getSideLabel(Side side) {
        boolean left = (side == Side.START) != isRtl();
        return left ? R.string.zinc_settings_side_left : R.string.zinc_settings_side_right;
    }

    private static boolean isRtl() {
        return LocalizationUtils.isLayoutRtl();
    }

    private static CharSequence[] numberEntries(int from, int to) {
        CharSequence[] entries = new CharSequence[to - from + 1];
        for (int i = from; i <= to; i++) entries[i - from] = Integer.toString(i);
        return entries;
    }
}
