package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Keyboard navigation of the hub: the focus over the targets of a frame (by id, so it survives a re-sort), pages, and scrolling to it. Pure. */
public final class HubFocus {
    private HubFocus() {
    }

    /** The id after moving by {@code delta}, wrapping; from none (or a target that went away) forward is the first and backward the last. */
    @Nullable
    public static String move(List<String> ids, @Nullable String focus, int delta) {
        if (ids.isEmpty()) {
            return null;
        }
        int i = focus == null ? -1 : ids.indexOf(focus);
        if (i < 0) {
            return delta >= 0 ? ids.get(0) : ids.get(ids.size() - 1);
        }
        return ids.get(Math.floorMod(i + delta, ids.size()));
    }

    /** The page {@code delta} ribbons away from {@code current} among the visible ones, wrapping. */
    public static HubPage page(List<HubPage> visible, HubPage current, int delta) {
        if (visible.isEmpty()) {
            return current;
        }
        int i = visible.indexOf(current);
        return visible.get(Math.floorMod((i < 0 ? 0 : i) + delta, visible.size()));
    }

    /**
     * The scroll of a region that shows {@code target} whole (drawn at {@code scroll}), moving as little as possible; a target taller than
     * the region shows its top.
     */
    public static int scrollToShow(int scroll, Rect clip, Rect target) {
        if (target.bottom() > clip.bottom()) {
            scroll += target.bottom() - clip.bottom();
            target = new Rect(target.x(), target.y() - (target.bottom() - clip.bottom()), target.w(), target.h());
        }
        if (target.y() < clip.y()) {
            scroll -= clip.y() - target.y();
        }
        return Math.max(0, scroll);
    }

    /** A page turn of the region: its height less one line, so a line cut at the edge shows again. */
    public static int pageScroll(int scroll, int max, Rect clip, int direction) {
        return Math.max(0, Math.min(max, scroll + direction * Math.max(10, clip.h() - 10)));
    }
}
