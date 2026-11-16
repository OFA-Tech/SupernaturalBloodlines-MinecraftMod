package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Where the tracked task's pill goes. It never sits where another mod's HUD does: the default is the top centre, under the boss bars, which nothing of
 * Tensura, Vampirism or Werewolves draws on. Pure, so it is tested for the smallest screens.
 */
public enum TrackerAnchor {
    TOP_CENTER,
    TOP_RIGHT,
    TOP_LEFT,
    ABOVE_MODE_PILL;

    public static final int TOP = 6;
    /** The pill's height. */
    public static final int HEIGHT = 22;

    /** The pill's height for {@code lines} lines of text: one line is {@link #HEIGHT}, every further line is 9 more. */
    public static int height(int lines) {
        return HEIGHT + 9 * (Math.max(1, lines) - 1);
    }
    /** The right edge of the full moon warning's corner (it takes 6 + 22 + 30 from the right) and its text row under it. */
    /** The width of the status Tensura and Vampirism draw in the top left corner (about 130 pixels in the screenshots). */
    public static final int CORNER_W = 140;
    private static final int MOON_ROW = 6 + 26 + 10;

    /** @param bossBottom the lowest pixel of the boss bars drawn last frame (0 for none) */
    public int[] place(int screenW, int screenH, int w, int h, int bossBottom) {
        int x;
        int y;
        switch (this) {
            case TOP_LEFT -> {
                // under the corner where Tensura and Vampirism draw their status (about 70 pixels tall)
                x = 6;
                y = TOP + 70;
            }
            case TOP_RIGHT -> {
                x = screenW - 6 - w;
                y = MOON_ROW + 2;
            }
            case ABOVE_MODE_PILL -> {
                x = screenW / 2 - w / 2;
                y = screenH - 22 - 52 - h - 4;
            }
            default -> {
                x = screenW / 2 - w / 2;
                y = Math.max(TOP, bossBottom + 4);
                // on a narrow screen the centre is under the other mods' status corner: move right of it
                if (y < TOP + 70) {
                    x = Math.max(x, CORNER_W + 4);
                }
            }
        }
        x = Math.max(2, Math.min(x, screenW - w - 2));
        y = Math.max(2, Math.min(y, screenH - h - 2));
        return new int[] {x, y};
    }
}
