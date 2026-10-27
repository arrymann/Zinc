// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;

import java.util.Objects;

/** One customizable toolbar button: the function it performs and where it sits. */
@NullMarked
public final class ZincToolbarButton {
    /**
     * Which side of the address box the button is on. START is left of the address box in
     * left-to-right layouts and right of it in right-to-left layouts.
     */
    public enum Side {
        START("start"),
        END("end");

        private final String mKey;

        Side(String key) {
            mKey = key;
        }

        public String getKey() {
            return mKey;
        }

        public static @Nullable Side fromKey(@Nullable String key) {
            if (key == null) return null;
            for (Side side : values()) {
                if (side.mKey.equals(key)) return side;
            }
            return null;
        }
    }

    private final ZincAction mAction;
    private final Side mSide;

    public ZincToolbarButton(ZincAction action, Side side) {
        mAction = Objects.requireNonNull(action);
        mSide = Objects.requireNonNull(side);
    }

    public ZincAction getAction() {
        return mAction;
    }

    public Side getSide() {
        return mSide;
    }

    public ZincToolbarButton withAction(ZincAction action) {
        return new ZincToolbarButton(action, mSide);
    }

    public ZincToolbarButton withSide(Side side) {
        return new ZincToolbarButton(mAction, side);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ZincToolbarButton)) return false;
        ZincToolbarButton button = (ZincToolbarButton) other;
        return mAction == button.mAction && mSide == button.mSide;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAction, mSide);
    }

    @Override
    public String toString() {
        return mAction.getKey() + "@" + mSide.getKey();
    }
}
