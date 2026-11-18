package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Keyboard order of the hub: Tab moves nav, then stage, then panel, then actions. A target's zone comes from its id, so a page needs no
 * change. Number keys 0 (Home) and 1 to 9 jump to a page. Pure.
 */
public final class FocusOrder {
    public enum Zone {
        NAV, STAGE, PANEL, ACTIONS
    }

    static final int KEY_1 = 49;
    static final int KEY_0 = 48;
    static final int KEY_9 = 57;
    static final int KP_1 = 321;
    static final int KP_0 = 320;
    static final int KP_9 = 329;

    private FocusOrder() {
    }

    public static Zone zoneOf(String id) {
        if (id.startsWith("tab:") || id.startsWith("seal:") || id.equals("theme") || id.equals("close")) {
            return Zone.NAV;
        }
        if (id.startsWith("detail:") || id.startsWith("action:")) {
            return Zone.ACTIONS;
        }
        if (id.startsWith("panel:")) {
            return Zone.PANEL;
        }
        // ids starting "line:" (the Bloodline page's tree) are stage targets, and the page registers them root first, then the chain, then the sired
        return Zone.STAGE;
    }

    /** The ids by zone, each zone keeping the order they were registered in. */
    public static List<String> order(List<String> ids) {
        List<String> out = new ArrayList<>(ids);
        out.sort(Comparator.comparingInt(id -> zoneOf(id).ordinal()));
        return out;
    }

    /** True for the key 0 (top row or keypad): Home. */
    public static boolean isHomeKey(int glfwKey) {
        return glfwKey == KEY_0 || glfwKey == KP_0;
    }

    /** The index among the pages after Home that a number key (top row or keypad, 1 to 9) asks for, when that many pages show. */
    public static OptionalInt pageForKey(int glfwKey, int pageCount) {
        int i;
        if (glfwKey >= KEY_1 && glfwKey <= KEY_9) {
            i = glfwKey - KEY_1;
        } else if (glfwKey >= KP_1 && glfwKey <= KP_9) {
            i = glfwKey - KP_1;
        } else {
            return OptionalInt.empty();
        }
        return i < pageCount ? OptionalInt.of(i) : OptionalInt.empty();
    }
}
