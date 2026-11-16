package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** Where a confirm dialog and its two buttons go on a screen. Pure. */
public record ConfirmLayout(Rect panel, Rect cancel, Rect confirm) {
    public static final int MAX_W = 200;
    public static final int H = 78;
    public static final int BUTTON_H = 16;
    public static final int PAD = 8;

    public static ConfirmLayout of(int screenW, int screenH) {
        int w = Math.min(MAX_W, screenW - 16);
        Rect panel = new Rect((screenW - w) / 2, (screenH - H) / 2, w, H);
        int bw = (w - 2 * PAD - 6) / 2;
        int by = panel.bottom() - PAD - BUTTON_H;
        return new ConfirmLayout(panel, new Rect(panel.x() + PAD, by, bw, BUTTON_H), new Rect(panel.right() - PAD - bw, by, bw, BUTTON_H));
    }

    /** The body text's box: between the title and the buttons. */
    public Rect text() {
        return new Rect(panel.x() + PAD, panel.y() + PAD + 12, panel.w() - 2 * PAD, cancel.y() - panel.y() - PAD - 12 - 4);
    }
}
