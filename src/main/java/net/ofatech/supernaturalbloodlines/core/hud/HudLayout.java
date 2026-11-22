package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Where the six slots go on screen, in GUI pixels. The left column (slots 1 to 3, top to bottom) sits left of the offhand slot and the right column
 * (4 to 6) right of the attack indicator, both standing {@link #BOTTOM} pixels above the bottom edge. Pure, so it is tested for the screen sizes of GUI scale 2 and 4.
 */
public final class HudLayout {
    public static final int SLOT = 22;
    public static final int GAP = 2;
    public static final int MARGIN = 4;
    public static final int HOTBAR_HALF = 91;
    /** The offhand slot's sprite is 29 wide and ends at the hotbar. */
    public static final int OFFHAND = 29;
    /** The hotbar-mode attack indicator: 6 right of the hotbar, 18 wide. */
    public static final int ATTACK_GAP = 6;
    public static final int ATTACK = 18;
    /** Height of the strip along the bottom edge that the hotbar, the offhand slot and the attack indicator use. */
    public static final int HOTBAR_STRIP = 24;
    /** Air under the columns, so the last slot (and the tab or glow round it) is never cut by the screen edge. */
    public static final int BOTTOM = 3;

    public record Box(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean overlaps(Box o) {
            return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
        }
    }

    private HudLayout() {
    }

    public static final int PILL_H = 14;
    /** The Grimoire's pill picture has only 6 dark rows between its gold lines, too few for 7 rows of capitals: it is drawn 16 tall (its middle rows are one colour, so they stretch without a seam). */
    public static final int PILL_H_TALL = 16;

    /** The height the pill is drawn at in a look. */
    public static int pillHeight(SlotBadge.Look look) {
        return look == SlotBadge.Look.GRIMOIRE ? PILL_H_TALL : PILL_H;
    }
    /** The pill's top edge is this far above the bottom of the screen. */
    public static final int PILL_ABOVE = 74;
    /** The vanilla action bar line stands 68 px above the bottom, its 9 px text starting 4 px above that, so it falls inside the pill's band. */
    public static final int ACTION_BAR_ABOVE = 72;
    public static final int ACTION_BAR_H = 9;
    /** The mode panel's width: six slots and a little air; it stays in the right half of the screen even at 480 GUI pixels. */
    public static final int PREVIEW_W = 6 * SLOT + 8;
    /** The preview's slots start this far in from its left edge; the last mark then ends inside the panel's own edge. */
    public static final int PREVIEW_INSET = 3;
    /** Where the preview's slots start under its head: the mode mark stands 5 px over a slot's top edge, so the head's text must end above it. */
    public static int previewSlotsTop(int headLines) {
        return 11 + 9 * headLines;
    }
    /** The pill's room round its text: the mark (7 px) and the end caps of the kit's pill (up to 8 px each side) and a pixel of air. */
    public static final int PILL_EXTRA = 26;

    /**
     * The mode pill, centred above the hotbar between the two columns: its text plus {@link #PILL_EXTRA} px of end caps and mark, narrowed (the text is cut) when the columns
     * stand closer than that.
     */
    public static Box pill(int screenW, int screenH, int textW) {
        return pill(screenW, screenH, textW, false);
    }

    /** How far the pill and the left column's tooltip rise above an action bar message while Alt is held. */
    public static final int ALT_LIFT = ACTION_BAR_H + 6;

    /**
     * {@link #pill(int, int, int)}; {@code lifted} (Alt is held while an action bar message is up) moves it above the message and left of the screen's middle,
     * so it clears the message, the slots and the mode panel (which stands in the right half).
     */
    public static Box pill(int screenW, int screenH, int textW, boolean lifted) {
        return pill(screenW, screenH, textW, lifted, PILL_H);
    }

    public static Box pill(int screenW, int screenH, int textW, boolean lifted, int height) {
        Box[] s = slots(screenW, screenH);
        int room = Math.max(PILL_H, s[3].x() - s[0].right() - 2 * MARGIN);
        int w = Math.min(textW + PILL_EXTRA, room);
        int x = screenW / 2 - w / 2;
        if (lifted) {
            // the left half only (the mode panel stands in the right half), and its text is cut to what fits
            w = Math.min(w, screenW / 2 - 2 - MARGIN);
            x = Math.max(MARGIN, screenW / 2 - 2 - w);
        }
        return new Box(x, screenH - PILL_ABOVE - (lifted ? ALT_LIFT : 0), w, height);
    }

    /** A label is two lines (the name, then mastery and a short description) and stands level with its slot: its centre is the slot's centre. */
    /** The top of the left column's fallback panel: above the column, and above the lifted pill too while an action bar message is up. */
    public static int leftTooltipTop(int columnTop, int tooltipH, boolean lifted) {
        return Math.max(2, columnTop - tooltipH - 6 - (lifted ? ALT_LIFT : 0));
    }

    /** The full moon warning at the top right: its icon box, and the line of text under it (about 120 px wide at most, ending under the icon). */
    public static Box moonBox(int screenW) {
        return new Box(screenW - 36 - 120, 6, 120, 6 + 24 + 4 + 9 - 6);
    }

    /** The panel keeps this far from the moon warning. */
    public static final int MOON_CLEAR = 8;
    /** The top of the area the panels may use on the right: under the moon warning and its clearance. */
    public static int panelFloor(int screenW) {
        return moonBox(screenW).bottom() + MOON_CLEAR;
    }

    /** The most lines a panel can have in {@code room} px of height (its frame takes 8 px, a line 10). */
    public static int panelLines(int room) {
        return Math.max(0, (room - 8) / 10);
    }

    /**
     * The room for a column's panel: for the left one, from the screen's top to {@code columnTop} (less the lift of the pill, and 6 px of air); for the right one,
     * from under the moon warning to {@code stackTop} (the mode panel's top, or the column's).
     */
    public static int panelRoom(boolean right, int screenW, int stackTop, boolean lifted) {
        return right ? stackTop - 6 - panelFloor(screenW) : stackTop - 6 - (lifted ? ALT_LIFT : 0) - 2;
    }

    /** The top of the right column's panel of height {@code h}: above {@code stackTop}, and never into the moon warning's clearance. */
    public static int rightPanelTop(int screenW, int stackTop, int h) {
        return Math.max(panelFloor(screenW), stackTop - h - 6);
    }

    public static final int LABEL_H = 20;

    /** The height a label of {@code lines} lines is drawn at: a pixel of air above and below, 9 px a line. Two lines are {@link #LABEL_H}. */
    public static int drawnHeight(int lines) {
        return 2 + 9 * lines;
    }
    /** Air between a column and its labels. */
    public static final int LABEL_AIR = 3;
    /** Air between a label and the screen's edge. */
    public static final int LABEL_EDGE = 2;
    /** A label narrower than this is not drawn (a window so small that the column leaves no room beside it): the slot is still there. */
    public static final int LABEL_MIN_W = 32;

    /**
     * The labels held Alt shows, one per slot in slot order, each beside its own slot on the OUTER side: left of the left column (the text ends at the
     * slot) and right of the right column (the text starts at the slot), centred on the slot's row. Nothing else of the HUD stands out there (the pill, the
     * mode panel, the hotbar, the offhand slot and the attack indicator are all between or above the columns), and every label stays on screen. A column
     * with no room outside it (a small window at a big GUI scale) gets a label of width 0, which is not drawn.
     */
    public static Box[] labels(int screenW, int screenH) {
        Box[] s = slots(screenW, screenH);
        Box[] out = new Box[Loadout.SLOTS];
        for (int i = 0; i < out.length; i++) {
            int y = s[i].y() + (SLOT - LABEL_H) / 2;
            if (i < 3) {
                int w = s[i].x() - LABEL_AIR - LABEL_EDGE;
                out[i] = w < LABEL_MIN_W ? new Box(Math.max(0, s[i].x() - LABEL_AIR), y, 0, LABEL_H) : new Box(LABEL_EDGE, y, w, LABEL_H);
            } else {
                int x = s[i].right() + LABEL_AIR;
                int w = screenW - LABEL_EDGE - x;
                out[i] = w < LABEL_MIN_W ? new Box(Math.min(x, screenW), y, 0, LABEL_H) : new Box(x, y, w, LABEL_H);
            }
        }
        return out;
    }
    /** The action bar line of {@code textW} px (the vanilla one: item pickup, bed, and the HUD's own refusals). */
    public static Box actionBar(int screenW, int screenH, int textW) {
        return new Box(screenW / 2 - textW / 2, screenH - ACTION_BAR_ABOVE, textW, ACTION_BAR_H);
    }

    /** The panel with the other mode's slots: above the right column, its head {@code headLines} (1 or 2) lines tall, never past the right edge. */
    public static Box preview(int screenW, int screenH, int headLines) {
        Box[] s = slots(screenW, screenH);
        int h = previewSlotsTop(headLines) + SLOT + 4;
        int x = Math.max(4, Math.min(s[3].right() - PREVIEW_W, screenW - PREVIEW_W - 4));
        return new Box(x, s[3].y() - h - 4, PREVIEW_W, h);
    }

    public static Box hotbar(int screenW, int screenH) {
        return new Box(screenW / 2 - HOTBAR_HALF, screenH - 22, 2 * HOTBAR_HALF, 22);
    }

    public static Box offhand(int screenW, int screenH) {
        return new Box(screenW / 2 - HOTBAR_HALF - OFFHAND, screenH - 23, OFFHAND, 23);
    }

    public static Box attackIndicator(int screenW, int screenH) {
        return new Box(screenW / 2 + HOTBAR_HALF + ATTACK_GAP, screenH - 20, ATTACK, 18);
    }

    /**
     * The six slot boxes in slot order: 0 to 2 the left column, 3 to 5 the right column. A column that does not fit beside the hotbar (a narrow window at
     * a big GUI scale) is pushed against the screen edge and lifted above the hotbar strip, so it stays on screen and never covers the hotbar.
     */
    public static Box[] slots(int screenW, int screenH) {
        int left = screenW / 2 - HOTBAR_HALF - OFFHAND - MARGIN - SLOT;
        int right = screenW / 2 + HOTBAR_HALF + ATTACK_GAP + ATTACK + MARGIN;
        int columnH = 3 * SLOT + 2 * GAP;
        int low = screenH - BOTTOM - columnH;
        int lifted = screenH - HOTBAR_STRIP - columnH;
        boolean liftLeft = left < 0;
        boolean liftRight = right + SLOT > screenW;
        left = Math.max(0, left);
        right = Math.min(right, screenW - SLOT);
        Box[] boxes = new Box[Loadout.SLOTS];
        for (int row = 0; row < 3; row++) {
            boxes[row] = new Box(left, (liftLeft ? lifted : low) + row * (SLOT + GAP), SLOT, SLOT);
            boxes[3 + row] = new Box(right, (liftRight ? lifted : low) + row * (SLOT + GAP), SLOT, SLOT);
        }
        return boxes;
    }
}
