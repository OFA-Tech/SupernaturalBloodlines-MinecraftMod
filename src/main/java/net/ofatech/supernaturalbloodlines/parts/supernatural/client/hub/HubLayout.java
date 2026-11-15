package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.List;
import java.util.Map;

/**
 * Where a theme puts things: the frame, named content regions, one tab per visible page (in page order) and one seal per launcher.
 * The shared page views draw into {@link #MAIN} and {@link #SIDE}; a theme with another shape names its own regions and supplies its own
 * views for them ({@link HubTheme#view}). Pure.
 */
public record HubLayout(Rect frame, Map<String, Rect> regions, List<Rect> tabs, List<Rect> seals) {
    /** The main content: the story, the list. */
    public static final String MAIN = "main";
    /** The detail of what is chosen in the main region. */
    public static final String SIDE = "side";

    public HubLayout {
        regions = Map.copyOf(regions);
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
