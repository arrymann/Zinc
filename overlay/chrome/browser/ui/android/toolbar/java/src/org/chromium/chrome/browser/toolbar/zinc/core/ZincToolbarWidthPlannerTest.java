// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarWidthPlanner.WidthConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Unit tests for {@link ZincToolbarWidthPlanner}. */
@RunWith(BlockJUnit4ClassRunner.class)
public class ZincToolbarWidthPlannerTest {
    /** Consumes a fixed width if it fits, mimicking a single toolbar button. */
    private static WidthConsumer fixed(int width, List<Integer> offers) {
        return available -> {
            offers.add(available);
            return width <= available ? width : 0;
        };
    }

    /** Consumes as many fixed-size items as fit, mimicking the pinned actions list. */
    private static WidthConsumer repeating(int itemWidth, int itemCount) {
        return available -> Math.min(itemCount, available / itemWidth) * itemWidth;
    }

    @Test
    public void testMinLocationBarWidthUsesLargerOfAbsoluteAndProportional() {
        assertEquals(144, ZincToolbarWidthPlanner.minLocationBarWidth(400, 144));
        assertEquals(350, ZincToolbarWidthPlanner.minLocationBarWidth(1000, 144));
        assertEquals(144, ZincToolbarWidthPlanner.minLocationBarWidth(-5, 144));
    }

    @Test
    public void testExtensionsBudget() {
        // 412px toolbar, 3 buttons + menu (4 * 48) reserved, 144 minimum address box.
        assertEquals(76, ZincToolbarWidthPlanner.extensionsBudget(412, 192, 144));
        // Never negative, even when the buttons alone overflow.
        assertEquals(0, ZincToolbarWidthPlanner.extensionsBudget(300, 400, 144));
        assertEquals(0, ZincToolbarWidthPlanner.extensionsBudget(0, 0, 144));
    }

    @Test
    public void testAllocateInPriorityOrder() {
        List<Integer> offers = new ArrayList<>();
        List<WidthConsumer> consumers =
                Arrays.asList(fixed(48, offers), fixed(48, offers), fixed(48, offers));
        int used = ZincToolbarWidthPlanner.allocate(100, consumers);
        assertEquals(96, used);
        assertEquals(Arrays.asList(100, 52, 4), offers);
    }

    @Test
    public void testAllocateOverflowsActionsIntoMenu() {
        // Puzzle button first, then as many 40px extension icons as fit.
        List<Integer> offers = new ArrayList<>();
        int used =
                ZincToolbarWidthPlanner.allocate(
                        180, Arrays.asList(fixed(48, offers), repeating(40, 5)));
        assertEquals(48 + 3 * 40, used);
    }

    @Test
    public void testAllocateClampsMisbehavingConsumers() {
        WidthConsumer greedy = available -> available + 500;
        WidthConsumer negative = available -> -20;
        List<Integer> offers = new ArrayList<>();
        assertEquals(
                60,
                ZincToolbarWidthPlanner.allocate(
                        60, Arrays.asList(negative, greedy, fixed(10, offers))));
        assertEquals(Collections.singletonList(0), offers);
    }

    @Test
    public void testAllocateWithNegativeBudget() {
        List<Integer> offers = new ArrayList<>();
        List<WidthConsumer> consumers = Collections.singletonList(fixed(5, offers));
        assertEquals(0, ZincToolbarWidthPlanner.allocate(-10, consumers));
        assertEquals(Collections.singletonList(0), offers);
    }
}
