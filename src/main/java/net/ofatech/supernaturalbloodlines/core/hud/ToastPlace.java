package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;

/** Where the request toast stands: the top left, and under the status that Tensura and Vampirism draw there when either is installed. Pure. */
public final class ToastPlace {
    public static final int X = 4;
    public static final int Y = 4;
    /** Air between the other mods' status and the toast. */
    public static final int GAP = 4;

    private ToastPlace() {
    }

    public static Box place(int screenW, int screenH, int w, int h, boolean statusCorner) {
        int y = statusCorner ? TrackerAnchor.TOP + TrackerAnchor.CORNER_H + GAP : Y;
        int x = Math.max(0, Math.min(X, screenW - w));
        return new Box(x, Math.max(0, Math.min(y, screenH - h)), w, h);
    }
}