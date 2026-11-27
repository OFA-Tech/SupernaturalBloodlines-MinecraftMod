package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** Pixel rows of the medallion shapes: for each row of a {@code size x size} box, where the shape starts and how wide it is. Pure. */
public final class Shapes {
    public enum Shape {
        ROUND, HEX, SQUARE,
        /** A square without its four corner pixels (from size 8; smaller it is the square): the frame of a player's face. */
        SOFT
    }

    /** A filled run: from column {@code x}, {@code w} wide. */
    public record Span(int x, int w) {
    }

    /** A filled rectangle of a shape: the rows that have the same start and width merged into one. */
    public record Run(int x, int y, int w, int h) {
    }

    private static final java.util.Map<Long, Span[]> SPANS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, Run[]> RUNS = new java.util.concurrent.ConcurrentHashMap<>();

    private Shapes() {
    }

    private static long key(Shape shape, int size) {
        return ((long) shape.ordinal() << 32) | (size & 0xFFFFFFFFL);
    }

    /** The rows of a shape; the same array each time for the same shape and size (do not change it). */
    public static Span[] spans(Shape shape, int size) {
        return SPANS.computeIfAbsent(key(shape, size), k -> compute(shape, size));
    }

    /** The shape as the fewest rectangles that fill it (a square is one), cached by shape and size; drawing it takes one fill each. */
    public static Run[] runs(Shape shape, int size) {
        return RUNS.computeIfAbsent(key(shape, size), k -> {
            Span[] rows = spans(shape, size);
            java.util.List<Run> out = new java.util.ArrayList<>();
            int start = 0;
            for (int y = 1; y <= rows.length; y++) {
                if (y == rows.length || !rows[y].equals(rows[start])) {
                    out.add(new Run(rows[start].x(), start, rows[start].w(), y - start));
                    start = y;
                }
            }
            return out.toArray(new Run[0]);
        });
    }

    /** Whether the pixel (x, y) of a {@code size} box is inside the shape. */
    public static boolean contains(Shape shape, int size, int x, int y) {
        if (y < 0 || y >= size) {
            return false;
        }
        Span s = spans(shape, size)[y];
        return x >= s.x() && x < s.x() + s.w();
    }

    private static Span[] compute(Shape shape, int size) {
        Span[] rows = new Span[size];
        for (int y = 0; y < size; y++) {
            double yc = y + 0.5;
            int w = switch (shape) {
                case SQUARE -> size;
                case SOFT -> size >= 8 && (y == 0 || y == size - 1) ? size - 2 : size;
                case ROUND -> {
                    double half = size / 2.0;
                    double dy = yc - half;
                    yield (int) Math.round(2 * Math.sqrt(Math.max(0, half * half - dy * dy)));
                }
                // pointy top: the first and last quarter slope in to the apex, the middle half is full width
                case HEX -> {
                    double quarter = size / 4.0;
                    double d = yc < size / 2.0 ? yc : size - yc;
                    yield (int) Math.round(d >= quarter ? size : size * d / quarter);
                }
            };
            w = Math.max(1, Math.min(size, w));
            rows[y] = new Span((size - w) / 2, w);
        }
        return rows;
    }
}
