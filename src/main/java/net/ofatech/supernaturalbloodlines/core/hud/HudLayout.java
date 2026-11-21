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
    /** The pill's top edge is this far above the bottom of the screen. */
    public static final int PILL_ABOVE = 74;
    /** The vanilla action bar line stands 68 px above the bottom, its 9 px text starting 4 px above that, so it falls inside the pill's band. */
    public static final int ACTION_BAR_ABOVE = 72;
    public static final int ACTION_BAR_H = 9;
    /** The mode panel's width: six slots and a little air; it stays in the right half of the screen even at 480 GUI pixels. */
    public static final int PREVIEW_W = 6 * SLOT + 8;

    /**
     * The mode pill, centred above the hotbar between the two columns: its text plus 22 px of edge and diamond, narrowed (the text is cut) when the columns
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
        Box[] s = slots(screenW, screenH);
        int room = Math.max(PILL_H, s[3].x() - s[0].right() - 2 * MARGIN);
        int w = Math.min(textW + 22, room);
        int x = screenW / 2 - w / 2;
        if (lifted) {
            // the left half only (the mode panel stands in the right half), and its text is cut to what fits
            w = Math.min(w, screenW / 2 - 2 - MARGIN);
            x = Math.max(MARGIN, screenW / 2 - 2 - w);
        }
        return new Box(x, screenH - PILL_ABOVE - (lifted ? ALT_LIFT : 0), w, PILL_H);
    }

    /** A label is two lines (the name, then mastery and a short description) and stands level with its slot: its centre is the slot's centre. */
    public static final int LABEL_H = 20;
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
        int h = 30 + 9 * headLines;
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
