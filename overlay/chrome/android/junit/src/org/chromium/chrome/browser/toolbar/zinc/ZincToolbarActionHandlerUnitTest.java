// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.view.View;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.toolbar.ToolbarTabController;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincAction;
import org.chromium.chrome.browser.ui.appmenu.AppMenuDelegate;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Unit tests for {@link ZincToolbarActionHandler}. */
@RunWith(BaseRobolectricTestRunner.class)
public class ZincToolbarActionHandlerUnitTest {
    private static final Set<ZincAction> DIRECT_ACTIONS =
            EnumSet.of(
                    ZincAction.BACK,
                    ZincAction.FORWARD,
                    ZincAction.RELOAD,
                    ZincAction.HOME,
                    ZincAction.TAB_SWITCHER);

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private AppMenuDelegate mAppMenuDelegate;
    @Mock private ToolbarTabController mTabController;
    @Mock private Runnable mOpenTabSwitcher;
    @Mock private View.OnLongClickListener mTabSwitcherLongClickListener;
    @Mock private View mButton;

    private ZincToolbarActionHandler mHandler;

    @Before
    public void setUp() {
        mHandler =
                new ZincToolbarActionHandler(
                        mAppMenuDelegate,
                        mTabController,
                        mOpenTabSwitcher,
                        mTabSwitcherLongClickListener);
    }

    @Test
    public void testEveryActionHasAHandler() {
        Set<Integer> menuIds = new HashSet<>();
        for (ZincAction action : ZincAction.values()) {
            int menuId = ZincToolbarActionHandler.getMenuItemId(action);
            if (DIRECT_ACTIONS.contains(action)) {
                assertTrue(action + " is handled directly", menuId == Resources.ID_NULL);
            } else {
                assertNotEquals(action + " needs a menu id", Resources.ID_NULL, menuId);
                assertTrue(action + " reuses another action's menu id", menuIds.add(menuId));
            }
        }
    }

    @Test
    public void testMenuActionsGoThroughTheAppMenuDelegate() {
        for (ZincAction action : ZincAction.values()) {
            if (DIRECT_ACTIONS.contains(action)) continue;
            clearInvocations(mAppMenuDelegate);
            mHandler.onActionClicked(action, mButton);
            verify(mAppMenuDelegate)
                    .onOptionsItemSelected(ZincToolbarActionHandler.getMenuItemId(action), null);
        }
    }

    @Test
    public void testNavigationActionsUseTheTabController() {
        mHandler.onActionClicked(ZincAction.BACK, mButton);
        verify(mTabController).back();
        mHandler.onActionClicked(ZincAction.FORWARD, mButton);
        verify(mTabController).forward();
        mHandler.onActionClicked(ZincAction.RELOAD, mButton);
        verify(mTabController).stopOrReloadCurrentTab(false);
        mHandler.onActionClicked(ZincAction.HOME, mButton);
        verify(mTabController).openHomepage();
        verify(mAppMenuDelegate, never()).onOptionsItemSelected(anyInt(), isNull());
    }

    @Test
    public void testTabSwitcher() {
        mHandler.onActionClicked(ZincAction.TAB_SWITCHER, mButton);
        verify(mOpenTabSwitcher).run();

        when(mTabSwitcherLongClickListener.onLongClick(mButton)).thenReturn(true);
        assertTrue(mHandler.onActionLongClicked(ZincAction.TAB_SWITCHER, mButton));
        verify(mTabSwitcherLongClickListener).onLongClick(mButton);
    }

    @Test
    public void testLongPressShortcuts() {
        assertTrue(mHandler.onActionLongClicked(ZincAction.RELOAD, mButton));
        verify(mTabController).stopOrReloadCurrentTab(true);

        assertTrue(mHandler.onActionLongClicked(ZincAction.HOME, mButton));
        verify(mTabController).openHomepageInNewTab(true);

        // Other buttons fall back to showing their tooltip.
        assertFalse(mHandler.onActionLongClicked(ZincAction.SHARE, mButton));
    }

    @Test
    public void testMissingTabSwitcherIsANoOp() {
        ZincToolbarActionHandler handler =
                new ZincToolbarActionHandler(mAppMenuDelegate, mTabController, null, null);
        handler.onActionClicked(ZincAction.TAB_SWITCHER, mButton);
        assertFalse(handler.onActionLongClicked(ZincAction.TAB_SWITCHER, mButton));
    }

    @Test
    public void testExtensionsActionsUseTheExtensionsMenus() {
        mHandler.onActionClicked(ZincAction.EXTENSIONS, mButton);
        verify(mAppMenuDelegate).onOptionsItemSelected(R.id.extensions_menu_menu_id, null);
        mHandler.onActionClicked(ZincAction.MANAGE_EXTENSIONS, mButton);
        verify(mAppMenuDelegate).onOptionsItemSelected(R.id.manage_extensions_menu_id, null);
        mHandler.onActionClicked(ZincAction.WEB_STORE, mButton);
        verify(mAppMenuDelegate).onOptionsItemSelected(R.id.extensions_webstore_menu_id, null);
    }
}
