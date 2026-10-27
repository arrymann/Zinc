// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Unit tests for {@link ZincToolbarConfig}. */
@RunWith(BlockJUnit4ClassRunner.class)
public class ZincToolbarConfigTest {
    private static ZincToolbarButton start(ZincAction action) {
        return new ZincToolbarButton(action, Side.START);
    }

    private static ZincToolbarButton end(ZincAction action) {
        return new ZincToolbarButton(action, Side.END);
    }

    private static ZincToolbarConfig config(ZincToolbarButton... buttons) {
        return ZincToolbarConfig.of(Arrays.asList(buttons));
    }

    private static List<ZincAction> actions(ZincToolbarConfig config) {
        List<ZincAction> actions = new ArrayList<>();
        for (ZincToolbarButton button : config.getButtons()) actions.add(button.getAction());
        return actions;
    }

    @Test
    public void testDefaultLayout() {
        ZincToolbarConfig config = ZincToolbarConfig.DEFAULT;
        assertEquals(2, config.getButtonCount());
        assertEquals(Arrays.asList(start(ZincAction.BACK)), config.getStartButtons());
        assertEquals(Arrays.asList(end(ZincAction.TAB_SWITCHER)), config.getEndButtons());
    }

    @Test
    public void testOfNormalizesStartBeforeEnd() {
        ZincToolbarConfig config =
                config(
                        end(ZincAction.SHARE),
                        start(ZincAction.BACK),
                        end(ZincAction.TAB_SWITCHER),
                        start(ZincAction.FORWARD));
        assertEquals(
                Arrays.asList(
                        start(ZincAction.BACK),
                        start(ZincAction.FORWARD),
                        end(ZincAction.SHARE),
                        end(ZincAction.TAB_SWITCHER)),
                config.getButtons());
        assertEquals(2, config.getStartCount());
    }

    @Test
    public void testOfRejectsTooManyButtons() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        config(
                                start(ZincAction.BACK),
                                start(ZincAction.FORWARD),
                                end(ZincAction.RELOAD),
                                end(ZincAction.HOME),
                                end(ZincAction.SHARE)));
    }

    @Test
    public void testOfRejectsDuplicateActions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> config(start(ZincAction.BACK), end(ZincAction.BACK)));
    }

    @Test
    public void testSerializeRoundTrip() {
        ZincToolbarConfig config =
                config(
                        start(ZincAction.HOME),
                        end(ZincAction.NEW_TAB),
                        end(ZincAction.EXTENSIONS),
                        end(ZincAction.TAB_SWITCHER));
        String serialized = config.serialize();
        assertEquals("v1:home@start,new_tab@end,extensions@end,tab_switcher@end", serialized);
        assertEquals(config, ZincToolbarConfig.parse(serialized));
    }

    @Test
    public void testEmptyConfigRoundTrip() {
        assertEquals("v1:", ZincToolbarConfig.EMPTY.serialize());
        assertSame(ZincToolbarConfig.EMPTY, ZincToolbarConfig.parse("v1:"));
        assertEquals(0, ZincToolbarConfig.parse(" v1:  ").getButtonCount());
    }

    @Test
    public void testParseFallsBackToDefault() {
        assertSame(ZincToolbarConfig.DEFAULT, ZincToolbarConfig.parse(null));
        assertSame(ZincToolbarConfig.DEFAULT, ZincToolbarConfig.parse(""));
        assertSame(ZincToolbarConfig.DEFAULT, ZincToolbarConfig.parse("garbage"));
        assertSame(ZincToolbarConfig.DEFAULT, ZincToolbarConfig.parse("v2:back@start"));
    }

    @Test
    public void testParseIsLenient() {
        ZincToolbarConfig config =
                ZincToolbarConfig.parse(
                        "v1:back@start, unknown@end ,share@middle,back@end,reload,"
                                + "forward@end,home@start,new_tab@end,history@end");
        // Unknown action, unknown side, duplicate and malformed entries are dropped, and only
        // the first four valid buttons are kept.
        assertEquals(
                Arrays.asList(
                        start(ZincAction.BACK),
                        start(ZincAction.HOME),
                        end(ZincAction.FORWARD),
                        end(ZincAction.NEW_TAB)),
                config.getButtons());
    }

    @Test
    public void testWithButtonCountGrowsWithUnusedActionsAfterAddressBox() {
        ZincToolbarConfig config = ZincToolbarConfig.DEFAULT.withButtonCount(4);
        assertEquals(
                Arrays.asList(
                        start(ZincAction.BACK),
                        end(ZincAction.TAB_SWITCHER),
                        end(ZincAction.NEW_TAB),
                        end(ZincAction.FORWARD)),
                config.getButtons());
    }

    @Test
    public void testWithButtonCountShrinksFromVisualEnd() {
        ZincToolbarConfig config =
                config(
                                start(ZincAction.BACK),
                                start(ZincAction.FORWARD),
                                end(ZincAction.SHARE),
                                end(ZincAction.TAB_SWITCHER))
                        .withButtonCount(2);
        assertEquals(
                Arrays.asList(start(ZincAction.BACK), start(ZincAction.FORWARD)),
                config.getButtons());
    }

    @Test
    public void testWithButtonCountClamps() {
        assertEquals(0, ZincToolbarConfig.DEFAULT.withButtonCount(-3).getButtonCount());
        assertEquals(
                ZincToolbarConfig.MAX_BUTTONS,
                ZincToolbarConfig.DEFAULT.withButtonCount(99).getButtonCount());
    }

    @Test
    public void testWithButtonCountFillsWhenPreferredActionsAreTaken() {
        ZincToolbarConfig config =
                config(
                                start(ZincAction.NEW_TAB),
                                end(ZincAction.FORWARD),
                                end(ZincAction.BACK))
                        .withButtonCount(4);
        assertEquals(ZincAction.TAB_SWITCHER, config.getButton(3).getAction());
    }

    @Test
    public void testWithStartCountKeepsVisualOrder() {
        ZincToolbarConfig config =
                config(
                        start(ZincAction.BACK),
                        end(ZincAction.FORWARD),
                        end(ZincAction.SHARE),
                        end(ZincAction.TAB_SWITCHER));
        ZincToolbarConfig split = config.withStartCount(3);
        assertEquals(actions(config), actions(split));
        assertEquals(3, split.getStartCount());
        assertEquals(0, config.withStartCount(-1).getStartCount());
        assertEquals(4, config.withStartCount(10).getStartCount());
    }

    @Test
    public void testWithActionReplaces() {
        ZincToolbarConfig config = ZincToolbarConfig.DEFAULT.withAction(0, ZincAction.HOME);
        assertEquals(
                Arrays.asList(start(ZincAction.HOME), end(ZincAction.TAB_SWITCHER)),
                config.getButtons());
    }

    @Test
    public void testWithActionSwapsDuplicates() {
        ZincToolbarConfig config =
                ZincToolbarConfig.DEFAULT.withAction(0, ZincAction.TAB_SWITCHER);
        assertEquals(
                Arrays.asList(start(ZincAction.TAB_SWITCHER), end(ZincAction.BACK)),
                config.getButtons());
        assertSame(config, config.withAction(0, ZincAction.TAB_SWITCHER));
    }

    @Test
    public void testWithSideHopsAcrossAddressBox() {
        ZincToolbarConfig config =
                config(
                        start(ZincAction.BACK),
                        start(ZincAction.FORWARD),
                        end(ZincAction.SHARE),
                        end(ZincAction.TAB_SWITCHER));
        assertEquals(
                Arrays.asList(
                        start(ZincAction.FORWARD),
                        end(ZincAction.BACK),
                        end(ZincAction.SHARE),
                        end(ZincAction.TAB_SWITCHER)),
                config.withSide(0, Side.END).getButtons());
        assertEquals(
                Arrays.asList(
                        start(ZincAction.BACK),
                        start(ZincAction.FORWARD),
                        start(ZincAction.TAB_SWITCHER),
                        end(ZincAction.SHARE)),
                config.withSide(3, Side.START).getButtons());
        assertSame(config, config.withSide(0, Side.START));
    }

    @Test
    public void testMoveTowardStartWalksAcrossAddressBox() {
        ZincToolbarConfig config =
                config(start(ZincAction.BACK), end(ZincAction.SHARE), end(ZincAction.HOME));

        // HOME swaps with SHARE inside the END group.
        config = config.moveTowardStart(2);
        assertEquals(
                Arrays.asList(start(ZincAction.BACK), end(ZincAction.HOME), end(ZincAction.SHARE)),
                config.getButtons());

        // HOME is now the first END button, so it crosses the address box.
        config = config.moveTowardStart(1);
        assertEquals(
                Arrays.asList(
                        start(ZincAction.BACK), start(ZincAction.HOME), end(ZincAction.SHARE)),
                config.getButtons());

        // HOME swaps with BACK inside the START group, then cannot move further.
        config = config.moveTowardStart(1);
        assertEquals(ZincAction.HOME, config.getButton(0).getAction());
        assertSame(config, config.moveTowardStart(0));
    }

    @Test
    public void testMoveTowardStartWithNoStartButtons() {
        ZincToolbarConfig config = config(end(ZincAction.SHARE), end(ZincAction.HOME));
        ZincToolbarConfig moved = config.moveTowardStart(0);
        assertEquals(
                Arrays.asList(start(ZincAction.SHARE), end(ZincAction.HOME)), moved.getButtons());
    }

    @Test
    public void testMoveTowardEndWalksAcrossAddressBox() {
        ZincToolbarConfig config =
                config(start(ZincAction.BACK), start(ZincAction.HOME), end(ZincAction.SHARE));

        config = config.moveTowardEnd(0);
        assertEquals(
                Arrays.asList(
                        start(ZincAction.HOME), start(ZincAction.BACK), end(ZincAction.SHARE)),
                config.getButtons());

        config = config.moveTowardEnd(1);
        assertEquals(
                Arrays.asList(start(ZincAction.HOME), end(ZincAction.BACK), end(ZincAction.SHARE)),
                config.getButtons());

        config = config.moveTowardEnd(1);
        assertEquals(
                Arrays.asList(start(ZincAction.HOME), end(ZincAction.SHARE), end(ZincAction.BACK)),
                config.getButtons());
        assertSame(config, config.moveTowardEnd(2));
    }

    @Test
    public void testMoveTowardEndWithNoEndButtons() {
        ZincToolbarConfig config = config(start(ZincAction.SHARE), start(ZincAction.HOME));
        ZincToolbarConfig moved = config.moveTowardEnd(1);
        assertEquals(
                Arrays.asList(start(ZincAction.SHARE), end(ZincAction.HOME)), moved.getButtons());
    }

    @Test
    public void testMoveRoundTripRestoresLayout() {
        ZincToolbarConfig config =
                config(
                        start(ZincAction.BACK),
                        start(ZincAction.FORWARD),
                        end(ZincAction.SHARE),
                        end(ZincAction.TAB_SWITCHER));
        assertEquals(config, config.moveTowardEnd(1).moveTowardStart(1));
        assertEquals(config, config.moveTowardStart(2).moveTowardEnd(2));
    }

    @Test
    public void testAddAndRemove() {
        ZincToolbarConfig config =
                ZincToolbarConfig.DEFAULT
                        .add(ZincAction.HOME, Side.START)
                        .add(ZincAction.SHARE, Side.END);
        assertEquals(
                Arrays.asList(
                        start(ZincAction.HOME),
                        start(ZincAction.BACK),
                        end(ZincAction.TAB_SWITCHER),
                        end(ZincAction.SHARE)),
                config.getButtons());
        assertThrows(IllegalStateException.class, () -> config.add(ZincAction.RELOAD, Side.END));

        ZincToolbarConfig removed = config.remove(1);
        assertFalse(removed.contains(ZincAction.BACK));
        assertEquals(3, removed.getButtonCount());
        assertThrows(
                IllegalStateException.class, () -> removed.add(ZincAction.HOME, Side.END));
    }

    @Test
    public void testIndexChecks() {
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> ZincToolbarConfig.DEFAULT.withAction(2, ZincAction.HOME));
        assertThrows(IndexOutOfBoundsException.class, () -> ZincToolbarConfig.EMPTY.remove(0));
        assertThrows(
                IndexOutOfBoundsException.class, () -> ZincToolbarConfig.DEFAULT.moveTowardEnd(-1));
    }

    @Test
    public void testUnusedActions() {
        List<ZincAction> unused = ZincToolbarConfig.DEFAULT.getUnusedActions();
        assertEquals(ZincAction.values().length - 2, unused.size());
        assertFalse(unused.contains(ZincAction.BACK));
        assertFalse(unused.contains(ZincAction.TAB_SWITCHER));
        assertTrue(unused.contains(ZincAction.EXTENSIONS));
    }

    @Test
    public void testButtonsAreImmutable() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> ZincToolbarConfig.DEFAULT.getButtons().add(start(ZincAction.HOME)));
    }
}
