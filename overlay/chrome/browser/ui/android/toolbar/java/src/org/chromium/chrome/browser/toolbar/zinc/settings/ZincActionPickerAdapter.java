// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.toolbar.R;
import org.chromium.chrome.browser.toolbar.zinc.ZincActionResources;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lists every pinnable browser function, grouped under category headers, for the "Change
 * function" dialog. Functions already on another button are marked, since picking one swaps the
 * two buttons.
 */
@NullMarked
class ZincActionPickerAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ACTION = 1;

    private final Context mContext;
    private final ZincAction mCurrentAction;
    private final Map<ZincAction, Integer> mPinnedPositions = new EnumMap<>(ZincAction.class);
    /** Either a {@link ZincAction.Category} header or a {@link ZincAction}. */
    private final List<Object> mItems = new ArrayList<>();

    ZincActionPickerAdapter(
            Context context, ZincAction currentAction, List<ZincToolbarButton> buttons) {
        mContext = context;
        mCurrentAction = currentAction;
        for (int i = 0; i < buttons.size(); i++) {
            mPinnedPositions.put(buttons.get(i).getAction(), i + 1);
        }
        for (ZincAction.Category category : ZincAction.Category.values()) {
            mItems.add(category);
            for (ZincAction action : ZincAction.values()) {
                if (action.getCategory() == category) mItems.add(action);
            }
        }
    }

    /** Returns the action at {@code position}, or null for a category header. */
    @Nullable ZincAction getActionAt(int position) {
        Object item = mItems.get(position);
        return item instanceof ZincAction ? (ZincAction) item : null;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof ZincAction ? VIEW_TYPE_ACTION : VIEW_TYPE_HEADER;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == VIEW_TYPE_ACTION;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Object item = mItems.get(position);
        if (!(item instanceof ZincAction)) {
            TextView header =
                    convertView != null
                            ? (TextView) convertView
                            : (TextView)
                                    LayoutInflater.from(mContext)
                                            .inflate(
                                                    R.layout.zinc_action_picker_header,
                                                    parent,
                                                    false);
            header.setText(ZincActionResources.getCategoryLabel((ZincAction.Category) item));
            return header;
        }

        ZincAction action = (ZincAction) item;
        View view =
                convertView != null
                        ? convertView
                        : LayoutInflater.from(mContext)
                                .inflate(R.layout.zinc_action_picker_item, parent, false);
        ImageView icon = view.findViewById(R.id.zinc_action_icon);
        TextView label = view.findViewById(R.id.zinc_action_label);
        TextView note = view.findViewById(R.id.zinc_action_note);
        ImageView check = view.findViewById(R.id.zinc_action_check);

        icon.setImageResource(ZincActionResources.getIcon(action));
        label.setText(ZincActionResources.getLabel(action));

        boolean isCurrent = action == mCurrentAction;
        Integer pinnedPosition = mPinnedPositions.get(action);
        if (!isCurrent && pinnedPosition != null) {
            note.setText(mContext.getString(R.string.zinc_settings_swap_note, pinnedPosition));
            note.setVisibility(View.VISIBLE);
        } else {
            note.setVisibility(View.GONE);
        }
        check.setVisibility(isCurrent ? View.VISIBLE : View.INVISIBLE);
        view.setSelected(isCurrent);
        return view;
    }
}
