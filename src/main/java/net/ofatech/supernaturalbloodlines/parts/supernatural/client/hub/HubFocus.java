package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.List;

/** Keyboard navigation of the hub: moving the focus over the targets of a frame and between pages. Pure. */
public final class HubFocus {
    private HubFocus() {
    }

    /** The focus after moving by {@code delta}, wrapping; from none (-1) forward is the first and backward the last. -1 when there is nothing. */
    public static int move(int focus, int count, int delta) {
        if (count <= 0) {
            return -1;
        }
        if (focus < 0 || focus >= count) {
            return delta >= 0 ? 0 : count - 1;
        }
        return Math.floorMod(focus + delta, count);
    }

    /** The page {@code delta} ribbons away from {@code current} among the visible ones, wrapping. */
    public static HubPage page(List<HubPage> visible, HubPage current, int delta) {
        if (visible.isEmpty()) {
            return current;
        }
        int i = visible.indexOf(current);
        return visible.get(Math.floorMod((i < 0 ? 0 : i) + delta, visible.size()));
    }
}
