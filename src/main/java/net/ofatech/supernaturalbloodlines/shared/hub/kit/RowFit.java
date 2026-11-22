package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.ScrollHint;

/** Rows in a scrolled column: every row keeps its place (and its target, so the keyboard reaches it), but one that is not whole above the clip is not drawn. Pure. */
public final class RowFit {
    public record Row(int y, boolean drawn) {
    }

    private RowFit() {
    }

    public static List<Row> plan(int startY, int rowH, int step, int count, int clipBottom) {
        List<Row> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int y = startY + i * step;
            out.add(new Row(y, ScrollHint.rowFits(y, rowH, clipBottom)));
        }
        return out;
    }
}