package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Radial.Dir;

/**
 * Where everything of the wheel is, in GUI pixels, from the numbers of the approved W3 mockups (design box 480 x 270, wheel centre 162, 135): the hub, the inner ring of
 * mods (cells of 28 on r34), the middle ring of groups (labels on r69, a 42 degree fan round the open mod), the outer ring of powers (cells of 36 on r107, an arc of 312
 * degrees from bearing 204 clockwise with the 48 degree gap at the bottom, 12 to a page) and the page arrows in that gap. Bearings are compass degrees, clockwise from
 * the top. The design box is centred in the screen and keeps its GUI-pixel sizes at any scale (a bigger GUI leaves more margin). Pure.
 */
public final class WheelLayout {
    public static final int PAGE_SIZE = 12;
    public static final int DESIGN_W = 480;
    public static final int DESIGN_H = 270;
    /** The wheel's centre in the design box. */
    public static final int CENTRE_X = 162;
    public static final int CENTRE_Y = 135;
    public static final int INNER_R = 34;
    public static final int INNER_CELL = 28;
    public static final int MIDDLE_R = 69;
    public static final int MIDDLE_H = 14;
    public static final int MIDDLE_MIN_W = 36;
    public static final int MIDDLE_MAX_W = 54;
    public static final int OUTER_R = 107;
    public static final int OUTER_CELL = 36;
    public static final double ARC_START = 204;
    public static final double ARC_SPAN = 312;
    public static final double MAX_SEGMENT = 52;
    public static final double FAN_STEP = 42;
    public static final int ARROW_W = 22;
    public static final int ARROW_H = 20;
    public static final int ARROW_R = 106;
    public static final int PICKER_W = 204;
    public static final int PICKER_H = 64;
    public static final int MODE_LIST_W = 62;
    public static final int MODE_LIST_H = 46;
    /** What {@link Layout#neighbour} answers for an arrow key that turns the page. */
    public static final int TURN_PREVIOUS = -2;
    public static final int TURN_NEXT = -3;

    public enum Look { GRIMOIRE, BLOOD_MOON, TENSURA }

    public enum Ring { INNER, MIDDLE, OUTER }

    public record Cell(int x, int y, int w, int h, int index) {
        public boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    public record Hit(Ring ring, int index) {
    }

    private WheelLayout() {
    }

    /**
     * @param guiScale  the GUI scale: sizes are GUI pixels at every scale, so it changes nothing here; it is the scale the caller computed for and is kept for its checks
     * @param mods      how many mods are in the inner ring
     * @param openMod   which of them is open (the middle ring fans round it)
     * @param groups    how many groups the open mod has; fewer than 2 means no middle ring
     * @param entryCount how many powers the open group has (all pages)
     */
    public static Layout compute(int screenW, int screenH, int guiScale, Look look, int mods, int openMod, int groups, int entryCount, int page) {
        return new Layout(new Params(screenW, screenH, guiScale, look, Math.max(0, mods), openMod, Math.max(0, groups), Math.max(0, entryCount), page), null);
    }

    private record Params(int screenW, int screenH, int guiScale, Look look, int mods, int openMod, int groups, int entries, int page) {
    }

    private static Cell centred(double bearing, int r, int w, int h, int cx, int cy, int index) {
        double a = Math.toRadians(bearing);
        int x = cx + (int) Math.round(r * Math.sin(a));
        int y = cy - (int) Math.round(r * Math.cos(a));
        return new Cell(x - w / 2, y - h / 2, w, h, index);
    }

    private static double norm(double bearing) {
        return ((bearing % 360) + 360) % 360;
    }

    public static final class Layout {
        private final Params p;
        private final int ox;
        private final int oy;
        private final int cx;
        private final int cy;
        private final int page;
        private final int pageCount;
        private final List<Cell> inner = new ArrayList<>();
        private final List<Cell> middle = new ArrayList<>();
        private final List<Cell> outer = new ArrayList<>();
        private final double[] innerBearing;
        private final double[] middleBearing;
        private final double[] outerBearing;
        private final List<Cell> arrows = new ArrayList<>();
        private final int[] labelWidths;
        private final double outerSegment;
        private final double outerStart;

        private Layout(Params p, int[] labelWidths) {
            this.p = p;
            this.labelWidths = labelWidths;
            ox = p.screenW / 2 - DESIGN_W / 2;
            oy = p.screenH / 2 - DESIGN_H / 2;
            cx = ox + CENTRE_X;
            cy = oy + CENTRE_Y;
            pageCount = Math.max(1, (p.entries + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.max(0, Math.min(pageCount - 1, p.page));
            innerBearing = new double[p.mods];
            double step = p.mods == 0 ? 0 : 360.0 / p.mods;
            double first = p.mods == 1 ? 0 : -step / 2;
            for (int i = 0; i < p.mods; i++) {
                innerBearing[i] = norm(first + step * i);
                inner.add(centred(innerBearing[i], INNER_R, INNER_CELL, INNER_CELL, cx, cy, i));
            }
            int groupCount = p.groups >= 2 ? p.groups : 0;
            middleBearing = new double[groupCount];
            double fanCentre = p.mods == 0 ? 180 : innerBearing[Math.max(0, Math.min(p.mods - 1, p.openMod))];
            for (int i = 0; i < groupCount; i++) {
                middleBearing[i] = norm(fanCentre + FAN_STEP * (i - (groupCount - 1) / 2.0));
                int w = labelWidths == null || i >= labelWidths.length ? MIDDLE_MIN_W : Math.max(MIDDLE_MIN_W, Math.min(MIDDLE_MAX_W, labelWidths[i]));
                middle.add(centred(middleBearing[i], MIDDLE_R, w, MIDDLE_H, cx, cy, i));
            }
            int onPage = Math.max(0, Math.min(PAGE_SIZE, p.entries - page * PAGE_SIZE));
            outerBearing = new double[onPage];
            double segment = Math.min(MAX_SEGMENT, ARC_SPAN / Math.max(1, Math.min(p.entries, PAGE_SIZE)));
            double start = ARC_START + (ARC_SPAN - segment * Math.min(p.entries, PAGE_SIZE)) / 2;
            this.outerSegment = segment;
            this.outerStart = start;
            for (int i = 0; i < onPage; i++) {
                outerBearing[i] = norm(start + segment * (i + 0.5));
                outer.add(centred(outerBearing[i], OUTER_R, OUTER_CELL, OUTER_CELL, cx, cy, i));
            }
            if (pageCount > 1) {
                arrows.add(centred(197, ARROW_R, ARROW_W, ARROW_H, cx, cy, 0));
                arrows.add(centred(163, ARROW_R, ARROW_W, ARROW_H, cx, cy, 1));
            }
        }

        public int centreX() {
            return cx;
        }

        public int centreY() {
            return cy;
        }

        public Look look() {
            return p.look;
        }

        /** The shape of the rings and cells: a hexagon for the Tensura look, a circle for the others. */
        public net.ofatech.supernaturalbloodlines.core.wheel.Annulus.Shape shape() {
            return p.look == Look.TENSURA ? Annulus.Shape.polygon(6) : Annulus.Shape.CIRCLE;
        }

        public List<Cell> ring(Ring ring) {
            return switch (ring) {
                case INNER -> inner;
                case MIDDLE -> middle;
                case OUTER -> outer;
            };
        }

        public double bearing(Ring ring, int index) {
            return switch (ring) {
                case INNER -> innerBearing[index];
                case MIDDLE -> middleBearing[index];
                case OUTER -> outerBearing[index];
            };
        }

        public int page() {
            return page;
        }

        /** The angle of one outer segment, and the bearing the first one starts at (for the dividers between cells). */
        public double outerSegment() {
            return outerSegment;
        }

        public double outerStart() {
            return outerStart;
        }

        public int pageCount() {
            return pageCount;
        }

        /** The index in the whole list of the {@code i}-th cell of the current page. */
        public int entryIndex(int i) {
            return page * PAGE_SIZE + i;
        }

        public Layout onPage(int newPage) {
            return new Layout(new Params(p.screenW, p.screenH, p.guiScale, p.look, p.mods, p.openMod, p.groups, p.entries, newPage), labelWidths);
        }

        /** The same layout with the middle labels as wide as their text (36 to 54): the hit boxes then follow what is drawn. */
        public Layout withLabelWidths(int[] widths) {
            return new Layout(p, widths.clone());
        }

        /** Empty when there is one page; else the previous page's arrow (index 0, at the start of the arc) and the next page's (index 1). */
        public List<Cell> pageArrows() {
            return arrows;
        }

        /** The dots of the pages, 5 x 5 and 8 apart, centred under the arrows. */
        public List<Cell> pageDots() {
            List<Cell> dots = new ArrayList<>();
            if (pageCount > 1) {
                int total = (pageCount - 1) * 8 + 5;
                for (int i = 0; i < pageCount; i++) {
                    dots.add(new Cell(cx - total / 2 + i * 8, cy + 104 - 2, 5, 5, i));
                }
            }
            return dots;
        }

        /** Where "2/3" is written: centred under the dots. */
        public Cell pageLabel() {
            return new Cell(cx - 14, cy + 110, 28, 9, -1);
        }

        public Cell sidePanel() {
            return new Cell(ox + 326, oy + 22, 144, 196, -1);
        }

        public Cell gHint() {
            return new Cell(ox + 8, oy + 8, 92, 14, -1);
        }

        public Cell altHint() {
            return new Cell(ox + 8, oy + 25, 92, 14, -1);
        }

        /** The slot picker under (or, at the bottom of the screen, above) the {@code index}-th cell of the outer ring. */
        public Cell pickerAnchor(int index) {
            Cell c = outer.get(index);
            int x = Math.max(4, Math.min(p.screenW - 4 - PICKER_W, c.x + c.w() / 2 - PICKER_W / 2));
            int y = c.y + c.h() + 7;
            if (y + PICKER_H > p.screenH - 4) {
                y = c.y - 7 - PICKER_H;
            }
            return new Cell(x, Math.max(0, y), PICKER_W, PICKER_H, index);
        }

        /** The list of a power's modes beside its cell: to the left of a cell on the left half of the wheel, to the right of one on the right half. */
        public Cell modeListAnchor(int index) {
            Cell c = outer.get(index);
            boolean left = c.x + c.w() / 2 < cx;
            int x = left ? c.x - 4 - MODE_LIST_W : c.x + c.w() + 4;
            x = Math.max(2, Math.min(p.screenW - 2 - MODE_LIST_W, x));
            int y = Math.max(2, Math.min(p.screenH - 2 - MODE_LIST_H, c.y + c.h() / 2 - MODE_LIST_H / 2));
            return new Cell(x, y, MODE_LIST_W, MODE_LIST_H, index);
        }

        /** The cell under the point: the outer ring first, then the inner, then the middle labels. */
        public Optional<Hit> hit(int x, int y) {
            for (Ring r : new Ring[] {Ring.OUTER, Ring.INNER, Ring.MIDDLE}) {
                for (Cell c : ring(r)) {
                    if (c.contains(x, y)) {
                        return Optional.of(new Hit(r, c.index()));
                    }
                }
            }
            return Optional.empty();
        }

        /** The arrow (0 previous, 1 next) under the point, or -1. */
        public int arrowAt(int x, int y) {
            for (Cell c : arrows) {
                if (c.contains(x, y)) {
                    return c.index();
                }
            }
            return -1;
        }

        /** The ring an arrow key from {@code ring} goes to: Up is outward and Down inward, skipping a middle ring that is not there; Left and Right stay. */
        public Ring ringAfter(Ring ring, Dir dir) {
            if (dir == Dir.LEFT || dir == Dir.RIGHT) {
                return ring;
            }
            boolean out = dir == Dir.UP;
            return switch (ring) {
                case INNER -> out ? (middle.isEmpty() ? Ring.OUTER : Ring.MIDDLE) : Ring.INNER;
                case MIDDLE -> out ? Ring.OUTER : Ring.INNER;
                case OUTER -> out ? Ring.OUTER : (middle.isEmpty() ? Ring.INNER : Ring.MIDDLE);
            };
        }

        /**
         * Where an arrow key goes from cell {@code from} of {@code ring}: Left and Right are the previous and next cell of the ring (clockwise is next; the inner and
         * middle rings wrap, the outer one turns the page at its ends, {@link #TURN_PREVIOUS} and {@link #TURN_NEXT}, and stays on the first and last page); Up and
         * Down give the cell of the ring {@link #ringAfter} nearest in bearing. -1 when there is nothing to go to.
         */
        public int neighbour(Ring ring, int from, Dir dir) {
            List<Cell> cells = ring(ring);
            int n = cells.size();
            if (n == 0) {
                return -1;
            }
            if (from < 0 || from >= n) {
                return 0;
            }
            if (dir == Dir.LEFT || dir == Dir.RIGHT) {
                int step = dir == Dir.RIGHT ? 1 : -1;
                int next = from + step;
                if (next >= 0 && next < n) {
                    return next;
                }
                if (ring == Ring.OUTER) {
                    if (step > 0 && page < pageCount - 1) {
                        return TURN_NEXT;
                    }
                    if (step < 0 && page > 0) {
                        return TURN_PREVIOUS;
                    }
                    return from;
                }
                return Math.floorMod(next, n);
            }
            Ring target = ringAfter(ring, dir);
            if (target == ring) {
                return from;
            }
            int count = ring(target).size();
            if (count == 0) {
                return -1;
            }
            double here = bearing(ring, from);
            int best = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                double d = Math.abs(norm(bearing(target, i) - here + 180) - 180);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = i;
                }
            }
            return best;
        }
    }
}
