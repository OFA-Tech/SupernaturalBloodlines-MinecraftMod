package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Lays items of given widths left to right and wraps them to the next row when the row is full, so a row of buttons or chips never runs
 * past its region. An item wider than the area gets the whole row, cut to fit. Pure.
 */
public final class Flow {
    /** @param bottom the y under the last row, for the next block to start at */
    public record Result(List<Rect> rects, int bottom) {
    }

    private Flow() {
    }

    public static Result wrap(Rect area, List<Integer> widths, int h, int gap) {
        List<Rect> out = new ArrayList<>();
        int x = area.x();
        int y = area.y();
        for (int w : widths) {
            int fit = Math.min(w, area.w());
            if (x > area.x() && x + fit > area.right()) {
                x = area.x();
                y += h + gap;
            }
            out.add(new Rect(x, y, fit, h));
            x += fit + gap;
        }
        return new Result(out, widths.isEmpty() ? area.y() : y + h);
    }

    /** {@code count} equal columns across {@code area} with {@code gap} between them. */
    public static List<Rect> columns(Rect area, int count, int gap, int h) {
        List<Rect> out = new ArrayList<>();
        int w = (area.w() - gap * (count - 1)) / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            out.add(new Rect(area.x() + i * (w + gap), area.y(), w, h));
        }
        return out;
    }
}
