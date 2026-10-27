// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import org.chromium.build.annotations.NullMarked;

import java.util.List;

/**
 * Decides how much horizontal space the extensions area of the phone toolbar may use.
 *
 * <p>The customizable buttons and the app menu always keep their space. Pinned extensions get
 * whatever is left after reserving a minimum width for the address box; extensions that do not
 * fit overflow into the extensions (puzzle) menu instead of squeezing the address box.
 */
@NullMarked
public final class ZincToolbarWidthPlanner {
    /**
     * Fraction of the toolbar width that the address box keeps at minimum, so wide windows do not
     * end up with a tiny address box next to a long row of extensions.
     */
    static final float MIN_LOCATION_BAR_FRACTION = 0.35f;

    /** Something that occupies part of the extensions area, such as the pinned actions list. */
    public interface WidthConsumer {
        /**
         * Lays itself out within {@code availableWidth} and returns the width it actually uses,
         * which must be between 0 and {@code availableWidth}.
         */
        int consume(int availableWidth);
    }

    private ZincToolbarWidthPlanner() {}

    /**
     * Returns the minimum width reserved for the address box.
     *
     * @param toolbarWidth The full toolbar width.
     * @param minLocationBarWidth The absolute minimum address box width.
     */
    public static int minLocationBarWidth(int toolbarWidth, int minLocationBarWidth) {
        int proportional = Math.round(Math.max(0, toolbarWidth) * MIN_LOCATION_BAR_FRACTION);
        return Math.max(minLocationBarWidth, proportional);
    }

    /**
     * Returns the width available to the extensions area.
     *
     * @param toolbarWidth The full toolbar width.
     * @param reservedWidth Width taken by everything other than the address box and extensions:
     *     customizable buttons, the app menu and side padding.
     * @param minLocationBarWidth The absolute minimum address box width.
     */
    public static int extensionsBudget(
            int toolbarWidth, int reservedWidth, int minLocationBarWidth) {
        int locationBar = minLocationBarWidth(toolbarWidth, minLocationBarWidth);
        return Math.max(0, toolbarWidth - Math.max(0, reservedWidth) - locationBar);
    }

    /**
     * Offers the budget to each consumer in priority order, each one getting what the previous
     * ones left over. Returns the total width consumed. Misbehaving consumers that report more
     * than they were offered are clamped so the address box minimum is never violated.
     */
    public static int allocate(int budget, List<? extends WidthConsumer> consumers) {
        int remaining = Math.max(0, budget);
        for (WidthConsumer consumer : consumers) {
            int used = consumer.consume(remaining);
            remaining -= Math.max(0, Math.min(used, remaining));
        }
        return Math.max(0, budget) - remaining;
    }
}
