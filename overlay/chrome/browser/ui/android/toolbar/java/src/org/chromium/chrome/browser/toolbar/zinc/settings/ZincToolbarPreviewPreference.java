// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.toolbar.R;
import org.chromium.chrome.browser.toolbar.zinc.ZincActionResources;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarConfig;

import java.util.List;

/**
 * A miniature of the Zinc toolbar at the top of its settings page: start buttons, the address
 * box, end buttons, the extensions button and the app menu, in that order.
 */
@NullMarked
public class ZincToolbarPreviewPreference extends Preference {
    private ZincToolbarConfig mConfig = ZincToolbarConfig.DEFAULT;

    public ZincToolbarPreviewPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.zinc_toolbar_preview_preference);
    }

    /** Shows {@code config} in the preview. */
    public void setConfig(ZincToolbarConfig config) {
        if (config.equals(mConfig)) return;
        mConfig = config;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LinearLayout startSlots = (LinearLayout) holder.findViewById(R.id.zinc_preview_start);
        LinearLayout endSlots = (LinearLayout) holder.findViewById(R.id.zinc_preview_end);
        fillSlots(startSlots, mConfig.getStartButtons());
        fillSlots(endSlots, mConfig.getEndButtons());

        CharSequence description =
                getContext()
                        .getString(
                                R.string.zinc_settings_preview_description,
                                mConfig.getStartCount(),
                                mConfig.getButtonCount() - mConfig.getStartCount());
        holder.itemView.setContentDescription(description);
    }

    private void fillSlots(LinearLayout container, List<ZincToolbarButton> buttons) {
        container.removeAllViews();
        container.setVisibility(buttons.isEmpty() ? View.GONE : View.VISIBLE);
        for (ZincToolbarButton button : buttons) {
            int icon = ZincActionResources.getIcon(button.getAction());
            container.addView(createSlot(container, icon));
        }
    }

    private ImageView createSlot(ViewGroup parent, @DrawableRes int icon) {
        ImageView slot =
                (ImageView)
                        LayoutInflater.from(getContext())
                                .inflate(R.layout.zinc_toolbar_preview_slot, parent, false);
        slot.setImageResource(icon);
        return slot;
    }
}
