// Copyright 2026 The Zinc Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.zinc.core;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;
import org.chromium.chrome.browser.toolbar.zinc.core.ZincToolbarButton.Side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The user's Zinc toolbar layout: up to {@link #MAX_BUTTONS} buttons, each pinned to a browser
 * function and placed on either side of the address box.
 *
 * <p>The toolbar reads, in visual order: the {@link Side#START} buttons, the address box, the
 * {@link Side#END} buttons, the pinned extensions, then the app menu. {@link #getButtons()} is
 * always in that visual order, i.e. every START button precedes every END button, which lets the
 * settings UI expose simple "move left" / "move right" operations that carry a button across the
 * address box when it reaches the edge of its group.
 *
 * <p>Instances are immutable; every edit returns a new config. Each action appears at most once.
 */
@NullMarked
public final class ZincToolbarConfig {
    /** The maximum number of customizable buttons on the toolbar. */
    public static final int MAX_BUTTONS = 4;

    private static final String VERSION_PREFIX = "v1:";
    private static final String BUTTON_SEPARATOR = ",";
    private static final String SIDE_SEPARATOR = "@";

    /** The layout used until the user customizes the toolbar. */
    public static final ZincToolbarConfig DEFAULT =
            new ZincToolbarConfig(
                    Arrays.asList(
                            new ZincToolbarButton(ZincAction.BACK, Side.START),
                            new ZincToolbarButton(ZincAction.TAB_SWITCHER, Side.END)));

    /** A toolbar with no customizable buttons: just the address box, extensions and menu. */
    public static final ZincToolbarConfig EMPTY =
            new ZincToolbarConfig(Collections.<ZincToolbarButton>emptyList());

    /** Preferred actions, in order, when buttons are added without choosing a function. */
    private static final List<ZincAction> FILL_ORDER =
            Arrays.asList(
                    ZincAction.BACK,
                    ZincAction.TAB_SWITCHER,
                    ZincAction.NEW_TAB,
                    ZincAction.FORWARD,
                    ZincAction.RELOAD,
                    ZincAction.HOME,
                    ZincAction.SHARE,
                    ZincAction.BOOKMARK_PAGE);

    private final List<ZincToolbarButton> mButtons;

    private ZincToolbarConfig(List<ZincToolbarButton> normalizedButtons) {
        mButtons = Collections.unmodifiableList(new ArrayList<>(normalizedButtons));
    }

    /**
     * Creates a config from the given buttons. START buttons are moved ahead of END buttons while
     * keeping their relative order.
     *
     * @throws IllegalArgumentException if there are more than {@link #MAX_BUTTONS} buttons or an
     *     action appears more than once.
     */
    public static ZincToolbarConfig of(List<ZincToolbarButton> buttons) {
        if (buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("At most " + MAX_BUTTONS + " buttons allowed");
        }
        Set<ZincAction> seen = EnumSet.noneOf(ZincAction.class);
        for (ZincToolbarButton button : buttons) {
            if (!seen.add(button.getAction())) {
                throw new IllegalArgumentException("Duplicate action " + button.getAction());
            }
        }
        return new ZincToolbarConfig(normalize(buttons));
    }

    /**
     * Parses a config produced by {@link #serialize()}. Parsing is lenient so that a corrupt or
     * newer value never breaks the toolbar: unknown actions, unknown sides and duplicates are
     * dropped, extra buttons are ignored, and an unreadable value yields {@link #DEFAULT}.
     */
    public static ZincToolbarConfig parse(@Nullable String serialized) {
        if (serialized == null) return DEFAULT;
        String value = serialized.trim();
        if (!value.startsWith(VERSION_PREFIX)) return DEFAULT;

        String body = value.substring(VERSION_PREFIX.length()).trim();
        if (body.isEmpty()) return EMPTY;

        List<ZincToolbarButton> buttons = new ArrayList<>();
        Set<ZincAction> seen = EnumSet.noneOf(ZincAction.class);
        for (String token : body.split(BUTTON_SEPARATOR)) {
            if (buttons.size() == MAX_BUTTONS) break;
            String[] parts = token.trim().split(SIDE_SEPARATOR, -1);
            if (parts.length != 2) continue;
            ZincAction action = ZincAction.fromKey(parts[0].trim());
            Side side = Side.fromKey(parts[1].trim());
            if (action == null || side == null || !seen.add(action)) continue;
            buttons.add(new ZincToolbarButton(action, side));
        }
        return new ZincToolbarConfig(normalize(buttons));
    }

    /** Returns a stable string form of this config, suitable for {@link #parse(String)}. */
    public String serialize() {
        StringBuilder builder = new StringBuilder(VERSION_PREFIX);
        for (int i = 0; i < mButtons.size(); i++) {
            if (i > 0) builder.append(BUTTON_SEPARATOR);
            ZincToolbarButton button = mButtons.get(i);
            builder.append(button.getAction().getKey())
                    .append(SIDE_SEPARATOR)
                    .append(button.getSide().getKey());
        }
        return builder.toString();
    }

    /** Returns all buttons in visual order (START buttons, then END buttons). */
    public List<ZincToolbarButton> getButtons() {
        return mButtons;
    }

    public int getButtonCount() {
        return mButtons.size();
    }

    public ZincToolbarButton getButton(int index) {
        return mButtons.get(index);
    }

    /** Returns the buttons before the address box, in visual order. */
    public List<ZincToolbarButton> getStartButtons() {
        return mButtons.subList(0, getStartCount());
    }

    /** Returns the buttons after the address box, in visual order. */
    public List<ZincToolbarButton> getEndButtons() {
        return mButtons.subList(getStartCount(), mButtons.size());
    }

    /** Returns how many buttons sit before the address box. */
    public int getStartCount() {
        int count = 0;
        while (count < mButtons.size() && mButtons.get(count).getSide() == Side.START) count++;
        return count;
    }

    public boolean contains(ZincAction action) {
        return indexOf(action) >= 0;
    }

    /** Returns the visual index of the button for {@code action}, or -1 if it is not pinned. */
    public int indexOf(ZincAction action) {
        for (int i = 0; i < mButtons.size(); i++) {
            if (mButtons.get(i).getAction() == action) return i;
        }
        return -1;
    }

    /** Returns the actions that are not pinned to any button, in declaration order. */
    public List<ZincAction> getUnusedActions() {
        List<ZincAction> unused = new ArrayList<>();
        for (ZincAction action : ZincAction.values()) {
            if (!contains(action)) unused.add(action);
        }
        return unused;
    }

    /**
     * Returns a config with exactly {@code count} buttons (clamped to [0, {@link #MAX_BUTTONS}]).
     * Shrinking removes buttons from the visual end of the toolbar. Growing appends buttons after
     * the address box, using the most useful functions that are not pinned yet.
     */
    public ZincToolbarConfig withButtonCount(int count) {
        int target = Math.max(0, Math.min(MAX_BUTTONS, count));
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        while (buttons.size() > target) buttons.remove(buttons.size() - 1);
        while (buttons.size() < target) {
            ZincAction next = nextUnusedAction(buttons);
            buttons.add(new ZincToolbarButton(next, Side.END));
        }
        return new ZincToolbarConfig(normalize(buttons));
    }

    /**
     * Returns a config where the first {@code startCount} buttons (clamped to [0, button count])
     * are placed before the address box and the rest after it, keeping the visual order.
     */
    public ZincToolbarConfig withStartCount(int startCount) {
        int split = Math.max(0, Math.min(mButtons.size(), startCount));
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons.size());
        for (int i = 0; i < mButtons.size(); i++) {
            buttons.add(mButtons.get(i).withSide(i < split ? Side.START : Side.END));
        }
        return new ZincToolbarConfig(buttons);
    }

    /**
     * Returns a config where the button at {@code index} performs {@code action}. If another
     * button already performs that action, the two buttons swap functions so each action stays
     * unique.
     */
    public ZincToolbarConfig withAction(int index, ZincAction action) {
        checkIndex(index);
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        int existing = indexOf(action);
        if (existing == index) return this;
        if (existing >= 0) {
            ZincAction previous = buttons.get(index).getAction();
            buttons.set(existing, buttons.get(existing).withAction(previous));
        }
        buttons.set(index, buttons.get(index).withAction(action));
        return new ZincToolbarConfig(buttons);
    }

    /** Returns a config where the button at {@code index} is moved to {@code side}. */
    public ZincToolbarConfig withSide(int index, Side side) {
        checkIndex(index);
        if (mButtons.get(index).getSide() == side) return this;
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        ZincToolbarButton moved = buttons.remove(index).withSide(side);
        // Keep the button next to the address box so it visually "hops" across it.
        if (side == Side.START) {
            buttons.add(getStartCount(), moved);
        } else {
            buttons.add(getStartCount() - 1, moved);
        }
        return new ZincToolbarConfig(buttons);
    }

    /**
     * Moves the button at {@code index} one step towards the start of the toolbar. A START button
     * swaps with its neighbour; the first END button crosses the address box and becomes the last
     * START button. Moving the very first button is a no-op.
     */
    public ZincToolbarConfig moveTowardStart(int index) {
        checkIndex(index);
        ZincToolbarButton button = mButtons.get(index);
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        if (button.getSide() == Side.END && index == getStartCount()) {
            buttons.set(index, button.withSide(Side.START));
        } else if (index > 0) {
            Collections.swap(buttons, index, index - 1);
        } else {
            return this;
        }
        return new ZincToolbarConfig(buttons);
    }

    /**
     * Moves the button at {@code index} one step towards the end of the toolbar. An END button
     * swaps with its neighbour; the last START button crosses the address box and becomes the
     * first END button. Moving the very last button is a no-op.
     */
    public ZincToolbarConfig moveTowardEnd(int index) {
        checkIndex(index);
        ZincToolbarButton button = mButtons.get(index);
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        if (button.getSide() == Side.START && index == getStartCount() - 1) {
            buttons.set(index, button.withSide(Side.END));
        } else if (index < mButtons.size() - 1) {
            Collections.swap(buttons, index, index + 1);
        } else {
            return this;
        }
        return new ZincToolbarConfig(buttons);
    }

    /** Returns a config without the button at {@code index}. */
    public ZincToolbarConfig remove(int index) {
        checkIndex(index);
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        buttons.remove(index);
        return new ZincToolbarConfig(buttons);
    }

    /**
     * Returns a config with a new button for {@code action} at the outer edge of {@code side}.
     *
     * @throws IllegalStateException if the toolbar is full or the action is already pinned.
     */
    public ZincToolbarConfig add(ZincAction action, Side side) {
        if (mButtons.size() >= MAX_BUTTONS) throw new IllegalStateException("Toolbar is full");
        if (contains(action)) throw new IllegalStateException(action + " is already pinned");
        List<ZincToolbarButton> buttons = new ArrayList<>(mButtons);
        ZincToolbarButton button = new ZincToolbarButton(action, side);
        if (side == Side.START) {
            buttons.add(0, button);
        } else {
            buttons.add(button);
        }
        return new ZincToolbarConfig(buttons);
    }

    private static ZincAction nextUnusedAction(List<ZincToolbarButton> buttons) {
        Set<ZincAction> used = EnumSet.noneOf(ZincAction.class);
        for (ZincToolbarButton button : buttons) used.add(button.getAction());
        for (ZincAction action : FILL_ORDER) {
            if (!used.contains(action)) return action;
        }
        for (ZincAction action : ZincAction.values()) {
            if (!used.contains(action)) return action;
        }
        throw new IllegalStateException("No unused actions left");
    }

    private static List<ZincToolbarButton> normalize(List<ZincToolbarButton> buttons) {
        List<ZincToolbarButton> normalized = new ArrayList<>(buttons.size());
        for (ZincToolbarButton button : buttons) {
            if (button.getSide() == Side.START) normalized.add(button);
        }
        for (ZincToolbarButton button : buttons) {
            if (button.getSide() == Side.END) normalized.add(button);
        }
        return normalized;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= mButtons.size()) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of range [0, " + mButtons.size() + ")");
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ZincToolbarConfig)) return false;
        return mButtons.equals(((ZincToolbarConfig) other).mButtons);
    }

    @Override
    public int hashCode() {
        return mButtons.hashCode();
    }

    @Override
    public String toString() {
        return serialize();
    }
}
