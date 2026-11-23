package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.ArrayList;
import java.util.List;

/**
 * The health ring of a token: a band {@code t} thick round a square {@code s} wide, filled clockwise from the top centre. The rectangles are relative to the
 * square's top left corner, so the view draws them with plain fills (no new sprite: a note for the art). Pure.
 */
public final class HealthRing {
    /** x, y, width, height. */
    public record Rect(int x, int y, int w, int h) {
        public int area() {
            return w * h;
        }
    }

    private HealthRing() {
    }

    /** How many pixels long the band is along its middle: the five runs of {@link #filled} added up. */
    public static int length(int s, int t) {
        return 4 * s - 4 * t;
    }

    /** The whole band, as four rectangles that do not overlap. */
    public static List<Rect> track(int s, int t) {
        return List.of(new Rect(0, 0, s, t), new Rect(s - t, t, t, s - 2 * t), new Rect(0, s - t, s, t), new Rect(0, t, t, s - 2 * t));
    }

    /** How many pixels of the band {@code fraction} of the health fills: none for no health, one for any, all of them for full. */
    public static int lengthFor(int s, int t, float fraction) {
        if (!(fraction > 0f)) {
            return 0;
        }
        int total = length(s, t);
        return fraction >= 1f ? total : Math.max(1, Math.min(total, Math.round(total * fraction)));
    }

    /** The part of the band that {@code fraction} of the health fills: none for no health, a sliver of one pixel for any, the whole band for full. */
    public static List<Rect> filled(int s, int t, float fraction) {
        return filledPx(s, t, lengthFor(s, t, fraction));
    }

    /** The first {@code px} pixels of the band, clockwise from the top centre. */
    public static List<Rect> filledPx(int s, int t, int px) {
        List<Rect> out = new ArrayList<>(5);
        int left = Math.max(0, Math.min(length(s, t), px));
        if (left == 0) {
            return out;
        }
        int topRight = s - s / 2;
        int run = Math.min(left, topRight);
        if (run > 0) {
            out.add(new Rect(s / 2, 0, run, t));
            left -= run;
        }
        run = Math.min(left, s - 2 * t);
        if (run > 0) {
            out.add(new Rect(s - t, t, t, run));
            left -= run;
        }
        run = Math.min(left, s);
        if (run > 0) {
            out.add(new Rect(s - run, s - t, run, t));
            left -= run;
        }
        run = Math.min(left, s - 2 * t);
        if (run > 0) {
            out.add(new Rect(0, s - t - run, t, run));
            left -= run;
        }
        run = Math.min(left, s / 2);
        if (run > 0) {
            out.add(new Rect(0, 0, run, t));
        }
        return out;
    }
}
