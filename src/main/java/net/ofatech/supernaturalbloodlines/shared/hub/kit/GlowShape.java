package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a slot's ready glow, so it follows the frame it is on: a disc and its rim for the Blood Moon's round slots, a square for the others (the
 * Tensura theme's hexagon gets the square too: its glow is the square round the icon). Every piece is a {@code {x, y, w, h}} rectangle of a {@code size} square,
 * so it is drawn with plain fills. Pure.
 */
public final class GlowShape {
    private GlowShape() {
    }

    private static boolean inside(int x, int y, int size) {
        double r = size / 2.0;
        double dx = x + 0.5 - r;
        double dy = y + 0.5 - r;
        return dx * dx + dy * dy <= r * r;
    }

    /** The glow's body: the whole square, or the disc one row at a time. */
    public static List<int[]> fill(boolean round, int size) {
        List<int[]> out = new ArrayList<>();
        if (!round) {
            out.add(new int[] {0, 0, size, size});
            return out;
        }
        for (int y = 0; y < size; y++) {
            int x0 = -1;
            int x1 = -1;
            for (int x = 0; x < size; x++) {
                if (inside(x, y, size)) {
                    x0 = x0 < 0 ? x : x0;
                    x1 = x;
                }
            }
            if (x0 >= 0) {
                out.add(new int[] {x0, y, x1 - x0 + 1, 1});
            }
        }
        return out;
    }

    /** The glow's line: one pixel round the square, or the pixels of the disc that touch the outside. */
    public static List<int[]> outline(boolean round, int size) {
        List<int[]> out = new ArrayList<>();
        if (!round) {
            out.add(new int[] {0, 0, size, 1});
            out.add(new int[] {0, size - 1, size, 1});
            out.add(new int[] {0, 1, 1, size - 2});
            out.add(new int[] {size - 1, 1, 1, size - 2});
            return out;
        }
        for (int y = 0; y < size; y++) {
            int runStart = -1;
            for (int x = 0; x <= size; x++) {
                boolean edge = x < size && inside(x, y, size)
                        && (!inside(x - 1, y, size) || !inside(x + 1, y, size) || !inside(x, y - 1, size) || !inside(x, y + 1, size));
                if (edge && runStart < 0) {
                    runStart = x;
                } else if (!edge && runStart >= 0) {
                    out.add(new int[] {runStart, y, x - runStart, 1});
                    runStart = -1;
                }
            }
        }
        return out;
    }
}
