package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;

/**
 * The pixels of a ring band or one wedge of it, as one span per row: how the wheel's bands are filled with {@code GuiGraphics.fill} (no sprite, no triangle fan). Angles are
 * compass bearings in degrees, clockwise from the top; {@code up} is the distance above the centre. A pixel belongs to the band when its centre is. A wedge must be 180 degrees
 * or less (the wheel draws a ring of more than that whole). The shape is a circle, or a regular polygon with a vertex at the top (the Tensura look). Pure.
 */
public final class Annulus {
    /** A row of pixels: {@code y} (relative to the centre, downward) and the columns {@code x0} to {@code x1} inclusive (relative to the centre). */
    public record Span(int y, int x0, int x1) {
    }

    /** {@code sides} 0 is a circle of radius r; otherwise a regular polygon whose vertices are r from the centre, one of them straight up. */
    public record Shape(int sides) {
        public static final Shape CIRCLE = new Shape(0);

        public static Shape polygon(int sides) {
            return new Shape(sides);
        }
    }

    private Annulus() {
    }

    /** The band between {@code r0} and {@code r1}, all the way round. */
    public static List<Span> band(Shape shape, double r0, double r1) {
        return wedge(shape, r0, r1, 0, 360);
    }

    /** The band between {@code r0} and {@code r1} from bearing {@code a0} clockwise to {@code a1} ({@code a1 - a0} at most 180, or exactly 360 for the whole ring). */
    public static List<Span> wedge(Shape shape, double r0, double r1, double a0, double a1) {
        boolean whole = a1 - a0 >= 360 - 1e-9;
        if (!whole && a1 - a0 > 180 + 1e-9) {
            throw new IllegalArgumentException("a wedge is at most 180 degrees: " + (a1 - a0));
        }
        List<Span> out = new ArrayList<>();
        int top = (int) Math.floor(-r1);
        int bottom = (int) Math.ceil(r1);
        for (int y = top; y <= bottom; y++) {
            double up = -(y + 0.5);
            double[] outer = interval(shape, r1, up);
            if (outer == null) {
                continue;
            }
            double[] inner = r0 <= 0 ? null : interval(shape, r0, up);
            double[][] pieces = inner == null ? new double[][] {outer}
                    : new double[][] {{outer[0], Math.min(outer[1], inner[0])}, {Math.max(outer[0], inner[1]), outer[1]}};
            for (double[] piece : pieces) {
                double lo = piece[0];
                double hi = piece[1];
                if (!whole) {
                    double[] c1 = clip(a0, up, true);
                    double[] c2 = clip(a1, up, false);
                    lo = Math.max(lo, Math.max(c1[0], c2[0]));
                    hi = Math.min(hi, Math.min(c1[1], c2[1]));
                }
                int x0 = (int) Math.ceil(lo - 0.5);
                int x1 = (int) Math.floor(hi - 0.5);
                if (x0 <= x1) {
                    out.add(new Span(y, x0, x1));
                }
            }
        }
        return out;
    }

    /** The x interval of the shape of size r on the row at {@code up}; null when the row misses it. */
    private static double[] interval(Shape shape, double r, double up) {
        if (shape.sides() == 0) {
            double d = r * r - up * up;
            return d < 0 ? null : new double[] {-Math.sqrt(d), Math.sqrt(d)};
        }
        int n = shape.sides();
        double apothem = r * Math.cos(Math.PI / n);
        double lo = Double.NEGATIVE_INFINITY;
        double hi = Double.POSITIVE_INFINITY;
        for (int k = 0; k < n; k++) {
            double phi = Math.toRadians((k + 0.5) * 360.0 / n);
            double sx = Math.sin(phi);
            double cu = Math.cos(phi);
            double rest = apothem - up * cu; // x * sx <= rest
            if (Math.abs(sx) < 1e-9) {
                if (rest < 0) {
                    return null;
                }
            } else if (sx > 0) {
                hi = Math.min(hi, rest / sx);
            } else {
                lo = Math.max(lo, rest / sx);
            }
        }
        return lo > hi ? null : new double[] {lo, hi};
    }

    /**
     * The x interval where a point on the row is on the right side of the edge ray at bearing {@code a}: clockwise of it (start edge) or anticlockwise of it (end edge),
     * within 180 degrees.
     */
    private static double[] clip(double a, double up, boolean start) {
        double dx = Math.sin(Math.toRadians(a));
        double du = Math.cos(Math.toRadians(a));
        // start edge: dx * up - du * x <= 0, i.e. du * x >= dx * up; end edge: dx * up - du * x >= 0, i.e. du * x <= dx * up
        double rhs = dx * up;
        if (Math.abs(du) < 1e-9) {
            boolean ok = start ? rhs <= 0 : rhs >= 0;
            return ok ? new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY} : new double[] {1, 0};
        }
        boolean geq = start ? du > 0 : du < 0; // x >= rhs / du ?  (the inequality flips with the sign of du)
        double bound = rhs / du;
        return geq ? new double[] {bound, Double.POSITIVE_INFINITY} : new double[] {Double.NEGATIVE_INFINITY, bound};
    }

    /** Whether the centre of the pixel ({@code x}, {@code y}) is in the band: the slow rule the spans are tested against. */
    public static boolean contains(Shape shape, double r0, double r1, double a0, double a1, int x, int y) {
        double px = x + 0.5;
        double up = -(y + 0.5);
        double[] outer = interval(shape, r1, up);
        if (outer == null || px < outer[0] || px > outer[1]) {
            return false;
        }
        double[] inner = r0 <= 0 ? null : interval(shape, r0, up);
        if (inner != null && px >= inner[0] && px <= inner[1]) {
            return false;
        }
        if (a1 - a0 >= 360 - 1e-9) {
            return true;
        }
        double bearing = Math.toDegrees(Math.atan2(px, up));
        double rel = ((bearing - a0) % 360 + 360) % 360;
        return rel <= a1 - a0 + 1e-9;
    }
}
