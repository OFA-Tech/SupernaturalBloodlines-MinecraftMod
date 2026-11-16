package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** Pixel rows of the medallion shapes: for each row of a {@code size x size} box, where the shape starts and how wide it is. Pure. */
public final class Shapes {
    public enum Shape {
        ROUND, HEX, SQUARE
    }

    /** A filled run: from column {@code x}, {@code w} wide. */
    public record Span(int x, int w) {
    }

    private Shapes() {
    }

    public static Span[] spans(Shape shape, int size) {
        Span[] rows = new Span[size];
        for (int y = 0; y < size; y++) {
            double yc = y + 0.5;
            int w = switch (shape) {
                case SQUARE -> size;
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
