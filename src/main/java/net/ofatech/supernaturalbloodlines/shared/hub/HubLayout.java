package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Where a theme puts things: the frame, named content regions, one tab per visible page (in page order) and one seal per launcher.
 * The shared page views draw into {@link #MAIN} and {@link #SIDE}; a theme with another shape names its own regions and supplies its own
 * views for them ({@link HubTheme#view}). Pure.
 */
public record HubLayout(Rect frame, Map<String, Rect> regions, List<Rect> tabs, List<Rect> seals, Chrome chrome) {
    /** Where the detail of a selection goes: the side region (as before), the book's opposite page, a floating bottom bar, or an info pane. */
    public enum DetailKind {
        SIDE_REGION, PAGE, BOTTOM_BAR, INFO_PANE
    }

    /** How the pages are reached: index tabs out of an edge, a round icon rail with a label flag, or a list. */
    public enum NavStyle {
        INDEX_TABS, ICON_RAIL, LIST
    }

    /**
     * The parts of a theme that are not content: the theme switch, a close button, the emblem (the live model in its ring) and the detail
     * place. A null rectangle means the theme has none of it (yet). A theme owns all of this, so no two themes need share a shape.
     */
    public record Chrome(@Nullable Rect themeButton, @Nullable Rect close, @Nullable Rect emblem, @Nullable Rect detail, DetailKind detailKind,
                         NavStyle nav) {
        public static final Chrome NONE = new Chrome(null, null, null, null, DetailKind.SIDE_REGION, NavStyle.INDEX_TABS);

        public Chrome withThemeButton(Rect r) {
            return new Chrome(r, close, emblem, detail, detailKind, nav);
        }

        public Chrome withNav(NavStyle n) {
            return new Chrome(themeButton, close, emblem, detail, detailKind, n);
        }
    }

    public HubLayout(Rect frame, Map<String, Rect> regions, List<Rect> tabs, List<Rect> seals) {
        this(frame, regions, tabs, seals, Chrome.NONE);
    }

    /** The main content: the story, the list. */
    public static final String MAIN = "main";
    /** The detail of what is chosen in the main region. */
    public static final String SIDE = "side";

    public HubLayout {
        regions = Map.copyOf(regions);
        chrome = chrome == null ? Chrome.NONE : chrome;
        tabs = List.copyOf(tabs);
        seals = List.copyOf(seals);
    }

    public Rect region(String name) {
        Rect r = regions.get(name);
        if (r == null) {
            throw new IllegalArgumentException("this theme has no region " + name + "; it has " + regions.keySet());
        }
        return r;
    }
}
