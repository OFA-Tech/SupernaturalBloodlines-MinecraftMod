package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** Where a detail's action buttons go when a theme stacks them at the bottom of its place, and how much is left for the text. Pure. */
public final class DetailLayout {
    public static final int BUTTON_H = 14;
    public static final int GAP = 2;

    private DetailLayout() {
    }

    /** {@code count} buttons stacked from the bottom of {@code r}, listed in the order of the actions (the first is the topmost). */
    public static List<Rect> stack(Rect r, int count) {
        List<Rect> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int y = r.bottom() - (count - i) * (BUTTON_H + GAP) + GAP;
            out.add(new Rect(r.x(), y, r.w(), BUTTON_H));
        }
        return out;
    }

    /** What stacking {@code count} buttons takes from the bottom of a place. */
    public static int actionsHeight(int count) {
        return count == 0 ? 0 : count * (BUTTON_H + GAP);
    }

    /** The part of {@code r} above the stacked actions: the text's box. */
    public static Rect textArea(Rect r, int actionCount) {
        return new Rect(r.x(), r.y(), r.w(), Math.max(0, r.h() - actionsHeight(actionCount)));
    }
}
