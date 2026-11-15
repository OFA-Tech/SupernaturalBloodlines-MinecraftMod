package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Where the six slots go on screen, in GUI pixels. The left column (slots 1 to 3, top to bottom) sits left of the offhand slot and the right column
 * (4 to 6) right of the attack indicator, both standing on the bottom edge like the hotbar. Pure, so it is tested for the screen sizes of GUI scale 2 and 4.
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

    /** The six slot boxes in slot order: 0 to 2 the left column, 3 to 5 the right column. */
    public static Box[] slots(int screenW, int screenH) {
        int left = screenW / 2 - HOTBAR_HALF - OFFHAND - MARGIN - SLOT;
        int right = screenW / 2 + HOTBAR_HALF + ATTACK_GAP + ATTACK + MARGIN;
        int top = screenH - 3 * SLOT - 2 * GAP;
        Box[] boxes = new Box[Loadout.SLOTS];
        for (int row = 0; row < 3; row++) {
            int y = top + row * (SLOT + GAP);
            boxes[row] = new Box(left, y, SLOT, SLOT);
            boxes[3 + row] = new Box(right, y, SLOT, SLOT);
        }
        return boxes;
    }
}
