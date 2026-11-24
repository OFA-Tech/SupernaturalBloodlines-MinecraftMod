package net.ofatech.supernaturalbloodlines.core.wheel;

/** What a click or a scroll on the wheel means (owner decisions, 2026-11-22): left uses, right opens the slot picker, Alt cycles the mode, scroll changes a level or turns the page. */
public final class ClickPlan {
    /** {@code MENU}: the middle click, Infinity's own (a destination, Will Construct's quick slots, a choice menu); a power without a menu ignores it. */
    public enum Action { NONE, USE, SLOT_PICKER, USE_SECOND_OPTION, MODE_NEXT, MODE_PREVIOUS, MENU }

    public enum Scroll { NONE, LEVEL_UP, LEVEL_DOWN, MODE_NEXT, MODE_PREVIOUS, PAGE_NEXT, PAGE_PREVIOUS }

    /**
     * @param overOuterRing the pointer is on the outer ring (a power or an empty place of it)
     * @param overPower     it is on a power
     * @param hasLevels     that power has levels (a variable one)
     * @param hasModes      that power has more than one mode
     * @param pageable      the open group has more than one page
     */
    public record ScrollTarget(boolean overOuterRing, boolean overPower, boolean hasLevels, boolean hasModes, boolean pageable) {
    }

    private ClickPlan() {
    }

    /** {@code button} is GLFW's: 0 left, 1 right, 2 middle. Alt wins over Shift. */
    public static Action of(int button, boolean alt, boolean shift) {
        if (button == 0) {
            return alt ? Action.MODE_NEXT : shift ? Action.USE_SECOND_OPTION : Action.USE;
        }
        if (button == 1) {
            return alt ? Action.MODE_PREVIOUS : Action.SLOT_PICKER;
        }
        return button == 2 ? Action.MENU : Action.NONE;
    }

    /**
     * Precedence: Alt over a power with modes changes its mode; without Alt, a power with levels changes its level; without Alt, over the outer ring of a group with
     * several pages, the page turns. Alt never pages and never changes a level. Scrolling up ({@code delta > 0}) goes to the previous, down to the next.
     */
    public static Scroll scroll(double delta, boolean alt, ScrollTarget over) {
        if (delta == 0 || Double.isNaN(delta)) {
            return Scroll.NONE;
        }
        boolean up = delta > 0;
        if (alt) {
            return over.overPower() && over.hasModes() ? (up ? Scroll.MODE_PREVIOUS : Scroll.MODE_NEXT) : Scroll.NONE;
        }
        if (over.overPower() && over.hasLevels()) {
            return up ? Scroll.LEVEL_UP : Scroll.LEVEL_DOWN;
        }
        if (over.overOuterRing() && over.pageable()) {
            return up ? Scroll.PAGE_PREVIOUS : Scroll.PAGE_NEXT;
        }
        return Scroll.NONE;
    }
}
