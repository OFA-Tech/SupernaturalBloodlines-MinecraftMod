package net.ofatech.supernaturalbloodlines.shared.hub;

/** A screen rectangle in GUI pixels. Pure. */
public record Rect(int x, int y, int w, int h) {
    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean overlaps(Rect o) {
        return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
    }

    public boolean inside(Rect o) {
        return x >= o.x && y >= o.y && right() <= o.right() && bottom() <= o.bottom();
    }

    /** The smallest rectangle that holds both. */
    public Rect union(Rect o) {
        int x0 = Math.min(x, o.x);
        int y0 = Math.min(y, o.y);
        return new Rect(x0, y0, Math.max(right(), o.right()) - x0, Math.max(bottom(), o.bottom()) - y0);
    }

    public Rect inset(int d) {
        return new Rect(x + d, y + d, w - 2 * d, h - 2 * d);
    }
}
