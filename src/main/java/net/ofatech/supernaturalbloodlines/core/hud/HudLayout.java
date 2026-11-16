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
