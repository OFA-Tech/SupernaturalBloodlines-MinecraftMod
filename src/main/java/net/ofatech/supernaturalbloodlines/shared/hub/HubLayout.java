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
    /**
     * The parts of a theme that are not content: the theme switch, a close button, the emblem rectangle (a page may draw it elsewhere with
     * {@code Frame.emblem(Rect)}), where SP and EP show, and where a selection's detail goes. A null rectangle means the theme has none of it
     * (yet), so a theme owns all of this and no two themes need share a shape.
     */
    public record Chrome(@Nullable Rect themeButton, @Nullable Rect close, @Nullable Rect emblem, @Nullable Rect counters, @Nullable Rect detail) {
        public static final Chrome NONE = new Chrome(null, null, null, null, null);

        public Chrome withThemeButton(Rect r) {
            return new Chrome(r, close, emblem, counters, detail);
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
