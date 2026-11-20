package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Where a choice dialog goes on a screen: a title, wrapped text, a list of {@code choices} buttons that scrolls when it does not fit, and one decline
 * button that is always on screen. Same chrome as {@link ConfirmLayout}. The text keeps every line it asked for as long as two choices stay visible; below
 * that the text gives up lines, never the buttons. Pure.
 */
public record ChoiceLayout(Rect panel, Rect text, Rect list, Rect decline, int textLines, int rows) {
    public static final int ROW = ConfirmLayout.BUTTON_H + 2;
    private static final int TITLE = 12;
    private static final int GAP = 4;

    public static ChoiceLayout of(int screenW, int screenH, int bodyLines, int choices) {
        int w = Math.min(ConfirmLayout.MAX_W, screenW - 16);
        int chrome = ConfirmLayout.PAD + TITLE + GAP + GAP + ConfirmLayout.BUTTON_H + ConfirmLayout.PAD;
        int limit = Math.max(chrome + ROW, screenH - 8);
        int body = Math.max(1, bodyLines);
        int h = Math.min(limit, chrome + body * ConfirmLayout.LINE + Math.max(0, choices) * ROW);
        int space = h - chrome;
        int minRows = Math.min(choices, 2);
        int lines = Math.max(1, Math.min(body, (space - minRows * ROW) / ConfirmLayout.LINE));
        int rows = Math.min(choices, Math.max(0, (space - lines * ConfirmLayout.LINE) / ROW));
        h = chrome + lines * ConfirmLayout.LINE + rows * ROW;
        Rect panel = new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
        int x = panel.x() + ConfirmLayout.PAD;
        int iw = w - 2 * ConfirmLayout.PAD;
        Rect text = new Rect(x, panel.y() + ConfirmLayout.PAD + TITLE, iw, lines * ConfirmLayout.LINE);
        Rect list = new Rect(x, text.bottom() + GAP, iw, rows * ROW);
        Rect decline = new Rect(x, panel.bottom() - ConfirmLayout.PAD - ConfirmLayout.BUTTON_H, iw, ConfirmLayout.BUTTON_H);
        return new ChoiceLayout(panel, text, list, decline, lines, rows);
    }

    /** The button of row {@code row} (0 is the first one shown); a list that scrolls leaves room on the right for the scroll bar. */
    public Rect row(int row, boolean scrolls) {
        return new Rect(list.x(), list.y() + row * ROW, list.w() - (scrolls ? 6 : 0), ConfirmLayout.BUTTON_H);
    }
}
