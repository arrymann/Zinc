// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.HashSet;
import java.util.Set;

/** Unit tests for {@link ZincAction}. */
@RunWith(BlockJUnit4ClassRunner.class)
public class ZincActionTest {
    @Test
    public void testKeysAreUniqueAndRoundTrip() {
        Set<String> keys = new HashSet<>();
        for (ZincAction action : ZincAction.values()) {
            assertTrue("Duplicate key " + action.getKey(), keys.add(action.getKey()));
            assertEquals(action, ZincAction.fromKey(action.getKey()));
        }
    }

    @Test
    public void testKeysAreSerializationSafe() {
        for (ZincAction action : ZincAction.values()) {
            assertTrue(action.getKey().matches("[a-z_]+"));
        }
    }

    @Test
    public void testUnknownKey() {
        assertNull(ZincAction.fromKey(null));
        assertNull(ZincAction.fromKey(""));
        assertNull(ZincAction.fromKey("BACK"));
    }

    @Test
    public void testRequiresTab() {
        assertTrue(ZincAction.BACK.requiresTab());
        assertTrue(ZincAction.SHARE.requiresTab());
        assertTrue(ZincAction.CLOSE_TAB.requiresTab());
        assertFalse(ZincAction.NEW_TAB.requiresTab());
        assertFalse(ZincAction.TAB_SWITCHER.requiresTab());
        assertFalse(ZincAction.SETTINGS.requiresTab());
        assertFalse(ZincAction.EXTENSIONS.requiresTab());
    }

    @Test
    public void testCoversBasicBrowserFunctions() {
        // Every category offers something to pin.
        for (ZincAction.Category category : ZincAction.Category.values()) {
            boolean found = false;
            for (ZincAction action : ZincAction.values()) {
                found |= action.getCategory() == category;
            }
            assertTrue("No actions in " + category, found);
        }
    }
}
