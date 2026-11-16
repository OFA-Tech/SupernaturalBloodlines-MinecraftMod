package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Where a confirm dialog and its two buttons go on a screen: the safe button (Stay) on the left, the confirm button on the right. The
 * height follows the wrapped body, up to about 70% of the screen. Pure.
 */
public record ConfirmLayout(Rect panel, Rect safe, Rect confirm, int maxLines) {
    public static final int MAX_W = 200;
    public static final int MIN_H = 62;
    public static final int BUTTON_H = 16;
    public static final int PAD = 8;
    public static final int LINE = 9;
    private static final int TITLE = 12;

    /** The width the body wraps to on a screen this wide. */
    public static int textWidth(int screenW) {
        return Math.min(MAX_W, screenW - 16) - 2 * PAD;
    }

    public static ConfirmLayout of(int screenW, int screenH, int bodyLines) {
        int w = Math.min(MAX_W, screenW - 16);
        int limit = Math.max(MIN_H, screenH * 7 / 10);
        int chrome = PAD + TITLE + 4 + BUTTON_H + PAD;
        int h = Math.max(MIN_H, Math.min(limit, chrome + Math.max(1, bodyLines) * LINE));
        Rect panel = new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
        int bw = (w - 2 * PAD - 6) / 2;
        int by = panel.bottom() - PAD - BUTTON_H;
        return new ConfirmLayout(panel, new Rect(panel.x() + PAD, by, bw, BUTTON_H), new Rect(panel.right() - PAD - bw, by, bw, BUTTON_H),
                Math.max(1, (h - chrome) / LINE));
    }

    /** The body text's box: between the title and the buttons. */
    public Rect text() {
        return new Rect(panel.x() + PAD, panel.y() + PAD + TITLE, panel.w() - 2 * PAD, safe.y() - panel.y() - PAD - TITLE - 4);
    }
}
