package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.jetbrains.annotations.Nullable;

/**
 * Where the request toast stands. By default the top left, and under the status that Tensura and Vampirism draw there when either is installed; the player may
 * pick any {@link TrackerAnchor} corner for it. It never covers the tracked task's pill: when the two would overlap, the toast moves under the pill. Pure.
 */
public final class ToastPlace {
    public static final int X = 4;
    public static final int Y = 4;
    /** Air between the other mods' status (or the tracker) and the toast. */
    public static final int GAP = 4;

    private ToastPlace() {
    }

    /**
     * shortcut: the other mods' status corner is a measured guess (140 x 70 px, {@link TrackerAnchor#CORNER_W}/{@link TrackerAnchor#CORNER_H}), not read from their
     * HUD; upgrade when Tensura or Vampirism expose the rectangle they draw.
     *
     * @param statusCorner whether Tensura or Vampirism is installed (their status stands top left)
     * @param anchor       the player's choice of corner; TOP_LEFT is the default
     * @param tracker      the tracked task's pill as it was drawn, or null when there is none
     */
    public static Box place(int screenW, int screenH, int w, int h, boolean statusCorner, TrackerAnchor anchor, @Nullable Box tracker) {
        int x;
        int y;
        if (anchor == TrackerAnchor.TOP_LEFT) {
            x = X;
            y = statusCorner ? TrackerAnchor.TOP + TrackerAnchor.CORNER_H + GAP : Y;
        } else {
            int[] at = anchor.place(screenW, screenH, w, h, 0);
            x = at[0];
            y = at[1];
        }
        Box b = clamp(screenW, screenH, x, y, w, h);
        if (tracker != null && b.overlaps(new Box(tracker.x() - GAP, tracker.y() - GAP, tracker.w() + 2 * GAP, tracker.h() + 2 * GAP))) {
            b = clamp(screenW, screenH, b.x(), tracker.bottom() + GAP, w, h);
        }
        return b;
    }

    private static Box clamp(int screenW, int screenH, int x, int y, int w, int h) {
        return new Box(Math.max(0, Math.min(x, screenW - w)), Math.max(0, Math.min(y, screenH - h)), w, h);
    }
}