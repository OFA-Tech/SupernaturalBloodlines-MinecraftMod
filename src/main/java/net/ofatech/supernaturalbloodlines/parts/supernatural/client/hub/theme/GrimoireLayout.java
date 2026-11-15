package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;

/**
 * The open book's geometry for a screen of {@code width x height} GUI pixels: a cover, two pages either side of the spine, and the
 * ribbon bookmarks sticking out of the right edge. It shrinks to fit the smallest GUI (320 x 240, any scale) and stops growing at
 * {@link #MAX_W} x {@link #MAX_H}. Pure.
 */
public final class GrimoireLayout {
    static final int MAX_W = 360;
    static final int MAX_H = 230;
    static final int MARGIN = 6;
    static final int COVER = 6;
    static final int SPINE = 8;
    static final int PAD = 7;
    static final int RIBBON_OUT = 18;
    static final int RIBBON_H = 18;
    static final int RIBBON_GAP = 3;

    private GrimoireLayout() {
    }

    public static HubLayout of(int width, int height, int tabs) {
        int bookW = Math.min(MAX_W, width - 2 * MARGIN - RIBBON_OUT);
        int bookH = Math.min(MAX_H, height - 2 * MARGIN);
        Rect book = new Rect((width - bookW - RIBBON_OUT) / 2, (height - bookH) / 2, bookW, bookH);
        int pageW = (bookW - 2 * COVER - SPINE) / 2;
        Rect leftPage = new Rect(book.x() + COVER, book.y() + COVER, pageW, bookH - 2 * COVER);
        Rect rightPage = new Rect(leftPage.right() + SPINE, leftPage.y(), pageW, leftPage.h());
        List<Rect> ribbons = new ArrayList<>();
        for (int i = 0; i < tabs; i++) {
            ribbons.add(new Rect(book.right() - 4, book.y() + 14 + i * (RIBBON_H + RIBBON_GAP), RIBBON_OUT + 4, RIBBON_H));
        }
        return new HubLayout(book, leftPage.inset(PAD), rightPage.inset(PAD), ribbons);
    }

    /** The paper of a page, from its content region (what the theme draws behind it). */
    static Rect page(Rect content) {
        return content.inset(-PAD);
    }
}
