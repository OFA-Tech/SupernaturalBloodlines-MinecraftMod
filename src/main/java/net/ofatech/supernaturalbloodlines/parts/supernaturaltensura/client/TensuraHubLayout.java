package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.NavGroups;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * The Tensura expansion's geometry, a status window: a title bar along the top (the page's name on the left, the stats line in the middle, the Theme
 * button and the close mark on the right), a list on the left (the pages from the top, "Other mods" under them), a stage in the middle and an info
 * pane down the right that holds the selection and its buttons ({@link HubLayout.Chrome#detail}). The stage is {@code moon}, {@code full} and, split in two,
 * {@link HubLayout#MAIN} and {@link HubLayout#SIDE} for the pages that read as two columns. Pure.
 */
public final class TensuraHubLayout {
    public static final String MOON = "moon";
    public static final String FULL = "full";
    static final int MAX_W = 760;
    static final int MAX_H = 430;
    static final int MARGIN = 6;
    static final int INSET = 8;
    static final int TITLE = 22;
    /** Room over the page list for its "Pages" caption, and over the mod list for "Other mods". */
    static final int CAPTION = 11;
    static final int GAP = 8;
    /** Between the two columns: the main one's scroll bar (7 px, one off its edge) and a pixel of air on each side of it. */
    static final int COLUMN_GAP = 10;
    /** Below this window width the page and mod lists show icons only. */
    static final int NARROW = 340;

    private TensuraHubLayout() {
    }

    /** The height of a group heading (its text is 8 high). */
    static final int HEADING = 9;
    static final int GROUP_GAP = 3;
    /** A row is never squeezed below this; when headings would, the groups get a plain gap instead. */
    static final int MIN_STEP = 13;

    public static HubLayout of(int width, int height, int tabs, int seals) {
        return of(width, height, NavGroups.noGaps(tabs), List.of(), seals, false);
    }

    public static HubLayout of(int width, int height, List<HubPage> pages, int seals) {
        int[] gaps = NavGroups.gaps(pages);
        List<NavGroups.Heading> heads = NavGroups.headings(pages);
        Rect nav = nav(frameOf(width, height));
        boolean wide = nav.w() > 40;
        int rows = pages.size() + seals;
        int mods = seals > 0 ? 6 + CAPTION : 0;
        // headings when they fit with rows of at least MIN_STEP, a plain gap otherwise (and the old "Pages" caption on top)
        boolean withHeadings = wide && rows > 0 && (nav.h() - heads.size() * HEADING - mods) / rows >= MIN_STEP;
        return of(width, height, gaps, withHeadings ? heads : List.of(), seals, withHeadings);
    }

    static Rect frameOf(int width, int height) {
        int w = Math.min(MAX_W, width - 2 * MARGIN);
        int h = Math.min(MAX_H, height - 2 * MARGIN);
        return new Rect((width - w) / 2, (height - h) / 2, w, h);
    }

    private static HubLayout of(int width, int height, int[] gaps, List<NavGroups.Heading> headings, int seals, boolean withHeadings) {
        int tabs = gaps.length;
        int w = Math.min(MAX_W, width - 2 * MARGIN);
        int h = Math.min(MAX_H, height - 2 * MARGIN);
        Rect frame = new Rect((width - w) / 2, (height - h) / 2, w, h);
        Rect nav = nav(frame);
        // every row fits: the step shrinks on a small screen
        int rows = tabs + seals;
        int gapPx = withHeadings ? 0 : NavGroups.total(gaps) * GROUP_GAP;
        int avail = nav.h() - (withHeadings ? headings.size() * HEADING + CAPTION : 2 * CAPTION) - gapPx - (seals > 0 ? 6 : 0);
        int step = Math.max(MIN_STEP, Math.min(18, rows == 0 ? 18 : avail / rows));
        int rowH = step - 2;
        List<Rect> pages = new ArrayList<>();
        List<Rect> heads = new ArrayList<>();
        int y = nav.y() + (withHeadings ? 0 : CAPTION);
        int hi = 0;
        for (int i = 0; i < tabs; i++) {
            if (withHeadings && hi < headings.size() && headings.get(hi).tab() == i) {
                heads.add(new Rect(nav.x(), y, nav.w(), HEADING));
                y += HEADING;
                hi++;
            } else if (!withHeadings) {
                y += gaps[i] * GROUP_GAP;
            }
            pages.add(new Rect(nav.x(), y, nav.w(), rowH));
            y += step;
        }
        List<Rect> mods = new ArrayList<>();
        int modsTop = y + 6 + CAPTION;
        for (int i = 0; i < seals; i++) {
            mods.add(new Rect(nav.x(), modsTop + i * step, nav.w(), rowH));
        }
        Rect pane = pane(frame);
        int left = nav.right() + GAP;
        Rect stage = new Rect(left, frame.y() + TITLE + INSET, pane.x() - GAP - left, frame.bottom() - INSET - (frame.y() + TITLE + INSET));
        int mainW = (stage.w() - COLUMN_GAP) * 42 / 100;
        Rect main = new Rect(stage.x(), stage.y(), mainW, stage.h());
        Rect side = new Rect(stage.x() + mainW + COLUMN_GAP, stage.y(), stage.w() - mainW - COLUMN_GAP, stage.h());
        Rect close = new Rect(frame.right() - INSET - 14, frame.y() + 4, 14, 14);
        Rect themeButton = new Rect(close.x() - 6 - 52, frame.y() + 4, 52, 14);
        int counterW = Math.max(60, Math.min(250, themeButton.x() - (frame.x() + 150) - 8));
        Rect counters = new Rect(themeButton.x() - 8 - counterW, frame.y() + 7, counterW, 10);
        return new HubLayout(frame, Map.of(MOON, stage, FULL, stage, HubLayout.MAIN, main, HubLayout.SIDE, side), pages, mods,
                new HubLayout.Chrome(themeButton, close, stage, counters, pane), heads);
    }

    static Rect nav(Rect frame) {
        // a narrow window shrinks the list to icons first, so the stage keeps its room
        int navW = frame.w() < NARROW ? 26 : Math.max(70, Math.min(104, frame.w() * 17 / 100));
        return new Rect(frame.x() + INSET, frame.y() + TITLE + INSET, navW, frame.h() - TITLE - 2 * INSET);
    }

    /** The info pane down the right of the window. */
    static Rect pane(Rect frame) {
        int w = Math.max(110, Math.min(190, frame.w() * 27 / 100));
        return new Rect(frame.right() - INSET - w, frame.y() + TITLE + INSET, w, frame.h() - TITLE - 2 * INSET);
    }

    /** {@code label} as it fits in {@code width}: whole, or cut with an ellipsis (the row's tooltip has it whole). */
    static String fit(String label, int width, java.util.function.ToIntFunction<String> measure) {
        if (measure.applyAsInt(label) <= width) {
            return label;
        }
        String ellipsis = "\u2026";
        int room = width - measure.applyAsInt(ellipsis);
        int end = label.length();
        while (end > 0 && measure.applyAsInt(label.substring(0, end)) > room) {
            end--;
        }
        return end == 0 ? "" : label.substring(0, end).stripTrailing() + ellipsis;
    }

    /** The panel behind a content region of the two column pages. */
    static Rect panel(Rect region) {
        return region.inset(-4);
    }
}
