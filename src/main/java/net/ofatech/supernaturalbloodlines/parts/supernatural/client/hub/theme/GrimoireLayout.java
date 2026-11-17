package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The open book's geometry for a screen of {@code width x height} GUI pixels: a leather cover with a gold line, two pages either side of the
 * spine (the left is {@link HubLayout#MAIN}, the right {@link HubLayout#SIDE}), each with a header band (the margin notes) and a foot (the
 * page number); the index tabs out of the right edge from the top, the brass clasp (the theme switch) over the first tab and the close under
 * the last; and the wax seals hanging on cords in a row below the book. It shrinks to fit the smallest GUI (320 x 240, any scale) and stops
 * growing at {@link #MAX_W} x {@link #MAX_H}. Pure.
 */
public final class GrimoireLayout {
    static final int MAX_W = 496;
    static final int MAX_H = 300;
    static final int MARGIN = 6;
    static final int COVER = 6;
    static final int SPINE = 8;
    static final int PAD = 7;
    /** The band at the top of a page for margin notes (SP and EP). */
    static final int HEADER = 12;
    /** The band at the foot of a page for its number. */
    static final int FOOT = 14;
    /** How far a tab sticks out of the book's right edge, and its height. */
    static final int TAB_OUT = 22;
    static final int TAB_H = 18;
    static final int TAB_GAP = 2;
    static final int SEAL = 16;
    static final int SEAL_STEP = 20;
    /** The cord a seal hangs on, from the book's bottom edge. */
    static final int CORD = 6;
    /** What the seals take below the book. */
    static final int SEAL_ROW = CORD + SEAL + 4;
    /** Tabs start below the top 34 GUI pixels, where vanilla's toasts (tutorial, advancement, recipe) are drawn over the screen. */
    static final int TOAST_CLEAR = 34;

    private GrimoireLayout() {
    }

    public static HubLayout of(int width, int height, int tabs, int seals) {
        int bookW = Math.min(MAX_W, width - 2 * MARGIN - TAB_OUT);
        int bookH = Math.min(MAX_H, height - MARGIN - SEAL_ROW);
        Rect book = new Rect((width - bookW - TAB_OUT) / 2, MARGIN + Math.max(0, (height - MARGIN - SEAL_ROW - bookH) / 2), bookW, bookH);
        int pageW = (bookW - 2 * COVER - SPINE) / 2;
        Rect leftPage = new Rect(book.x() + COVER, book.y() + COVER, pageW, bookH - 2 * COVER);
        Rect rightPage = new Rect(leftPage.right() + SPINE, leftPage.y(), pageW, leftPage.h());
        List<Rect> tabRects = new ArrayList<>();
        int top = Math.max(book.y() + 12, TOAST_CLEAR);
        // the tabs share the edge above the close button: the pitch shrinks (never below a pressable 12) when ten of them would not fit
        int pitch = tabs <= 0 ? TAB_H + TAB_GAP : Math.max(12 + TAB_GAP, Math.min(TAB_H + TAB_GAP, (book.bottom() - 18 - top) / tabs));
        for (int i = 0; i < tabs; i++) {
            tabRects.add(new Rect(book.right() - 4, top + i * pitch, TAB_OUT + 4, pitch - TAB_GAP));
        }
        List<Rect> wax = new ArrayList<>();
        for (int i = 0; i < seals; i++) {
            wax.add(new Rect(book.x() + 24 + i * SEAL_STEP, book.bottom() + CORD, SEAL, SEAL));
        }
        Rect main = content(leftPage);
        Rect clasp = new Rect(book.right() - 4, top - 17, TAB_OUT + 4, 14);
        Rect close = new Rect(book.right() - 4, book.bottom() - 16, TAB_OUT + 4, 14);
        Rect counters = new Rect(main.x(), leftPage.y() + 3, main.w(), 9);
        return new HubLayout(book, Map.of(HubLayout.MAIN, main, HubLayout.SIDE, content(rightPage)), tabRects, wax,
                new HubLayout.Chrome(clasp, close, null, counters, null));
    }

    /** The text area of a page: inside the margins, under the header band and over the foot. */
    static Rect content(Rect page) {
        return new Rect(page.x() + PAD, page.y() + PAD + HEADER, page.w() - 2 * PAD, page.h() - 2 * PAD - HEADER - FOOT);
    }

    /** The paper of a page, from its content region (what the theme draws behind it). */
    static Rect page(Rect content) {
        return new Rect(content.x() - PAD, content.y() - PAD - HEADER, content.w() + 2 * PAD, content.h() + 2 * PAD + HEADER + FOOT);
    }

    /** Where a page's number sits: centred in its foot. */
    static Rect number(Rect page) {
        return new Rect(page.x(), page.bottom() - 13, page.w(), 9);
    }
}
