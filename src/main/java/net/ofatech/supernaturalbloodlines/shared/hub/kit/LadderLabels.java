package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/**
 * Where the rung names go under a row of hexagons: the same code the Path page draws with and the tests check. Names sit under their rung, wrap to two lines
 * in the gap between rungs, drop to alternate rows when even a word does not fit the gap, and are kept inside the stage. Pure.
 */
public final class LadderLabels {
    /** The space under the hexagon row that the lower row of a staggered ladder takes. */
    public static final int LOWER = 18;
    private static final int LINE = 8;

    /** One rung's label: its lines, the rectangle of each line, whether it sits on the lower row, and the left edge of its hexagon. */
    public record Placed(List<String> lines, List<Rect> boxes, boolean lower, int hexX) {
    }

    private LadderLabels() {
    }

    /** The distance between two hexagons for {@code n} rungs on a stage {@code stageW} wide. */
    public static int step(int stageW, int n, int hex) {
        int span = Math.min(stageW - 16, n * 70);
        return n <= 1 ? 0 : (span - hex) / (n - 1);
    }

    /** Whether the names need two alternating rows to stay apart. */
    public static boolean staggered(List<String> names, int stageW, int hex, ToIntFunction<String> measure) {
        int n = names.size();
        return n > 1 && LabelFit.needsStagger(names, step(stageW, n, hex), measure);
    }

    /** The left edge of the first hexagon, so the row is centred on the stage. */
    public static int x0(Rect stage, int n, int hex) {
        return stage.x() + (stage.w() - (step(stage.w(), n, hex) * (n - 1) + hex)) / 2;
    }

    /**
     * @param y the top of the hexagon row; the boxes are placed under it
     */
    public static List<Placed> place(List<String> names, Rect stage, int hex, int y, ToIntFunction<String> measure) {
        int n = names.size();
        int step = step(stage.w(), n, hex);
        boolean stagger = staggered(names, stage.w(), hex, measure);
        int x0 = x0(stage, n, hex);
        List<Placed> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int x = x0 + i * step;
            int maxW = LabelFit.ladderWidth(step == 0 ? 80 : stagger ? 2 * step : step, 90);
            boolean lower = stagger && i % 2 == 1;
            List<String> lines = LabelFit.wrap(names.get(i), maxW, 2, measure);
            List<Rect> boxes = new ArrayList<>(lines.size());
            for (int k = 0; k < lines.size(); k++) {
                int lw = measure.applyAsInt(lines.get(k));
                int lx = Math.max(stage.x(), Math.min(stage.right() - lw - 4, x + hex / 2 - lw / 2));
                boxes.add(new Rect(lx, y + hex + 3 + (lower ? LOWER : 0) + LINE * k, lw + 4, LINE));
            }
            out.add(new Placed(lines, boxes, lower, x));
        }
        return out;
    }
}
