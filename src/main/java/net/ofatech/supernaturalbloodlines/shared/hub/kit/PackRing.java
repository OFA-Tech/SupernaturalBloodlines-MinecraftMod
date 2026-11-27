package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The pack portrait ring: who stands where and how big. Alphas red, Betas gold, Packmates blue, Brothers brown; subordinates (the pack's
 * extra members, plan 15) are small and faint at half. Pure, so plans 14 and 15 only add members.
 */
public final class PackRing {
    public enum Role {
        ALPHA(0xFFE0484A, 1f, false), BETA(0xFFE0A83A, 1f, false), PACKMATE(0xFF4A8AD8, 1f, false), BROTHER(0xFF8A6A4A, 1f, false),
        SUBORDINATE(0xFF8A8A98, 0.5f, true);

        private final int color;
        private final float alpha;
        private final boolean small;

        Role(int color, float alpha, boolean small) {
            this.color = color;
            this.alpha = alpha;
            this.small = small;
        }

        public int color() {
            return color;
        }

        public float alpha() {
            return alpha;
        }

        public boolean small() {
            return small;
        }
    }

    public record Member(String id, Role role) {
    }

    /** One portrait: its centre, its size in pixels and its opacity. */
    public record Slot(Member member, int x, int y, int size, float alpha) {
    }

    private PackRing() {
    }

    /** The ring as drawn: the portraits, how many members did not fit (shown as "+N"), and the portrait size actually used. */
    public record Layout(List<Slot> slots, int hidden, int size) {
    }

    /** The smallest portrait; below this a face is not a face. */
    public static final int MIN_SIZE = 8;
    /** The ring round a portrait is 2 px outside its face on each side; this is what must stay clear between the centres of two neighbours, on top of the faces: the two rings, 2 px of air and a pixel for the rounding of the positions. */
    public static final int GAP = 7;

    /**
     * How many portraits of {@code size} fit round a ring of {@code radius}: the chord between two neighbours (not the arc) is at least {@code size + GAP},
     * and the square frames of two neighbours ({@code size + 4}, {@link PortraitGeometry}) keep a pixel of air between them where the ring runs diagonally.
     */
    public static int capacity(int radius, int size) {
        int n = 1;
        while (n < 200 && 2 * radius * Math.sin(Math.PI / (n + 1)) >= size + GAP && squaresClear(radius, size, n + 1)) {
            n++;
        }
        return n;
    }

    /** Whether {@code n} square frames placed as {@link #layout} places them (rounded the same way) stay a pixel apart. */
    private static boolean squaresClear(int radius, int size, int n) {
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / n;
            double b = -Math.PI / 2 + 2 * Math.PI * ((i + 1) % n) / n;
            long dx = Math.abs(Math.round(radius * Math.cos(a)) - Math.round(radius * Math.cos(b)));
            long dy = Math.abs(Math.round(radius * Math.sin(a)) - Math.round(radius * Math.sin(b)));
            if (n > 1 && Math.max(dx, dy) < size + 5) {
                return false;
            }
        }
        return true;
    }

    /**
     * The members from the top clockwise, the roles in their order (Alphas first); subordinates at half size and half opacity. Portraits shrink down to
     * {@link #MIN_SIZE} as the members grow, and past what fits the last ones (the lowest roles) are left out and counted in {@code hidden}.
     */
    public static Layout layout(List<Member> members, int cx, int cy, int radius, int size) {
        List<Member> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingInt(m -> m.role().ordinal()));
        int use = size;
        // the Alphas and Betas are drawn at the full size and the rest at half, but the ring is spaced for the full size
        while (use > MIN_SIZE && capacity(radius, use) < sorted.size()) {
            use--;
        }
        int shown = Math.min(sorted.size(), capacity(radius, use));
        List<Slot> out = new ArrayList<>(shown);
        for (int i = 0; i < shown; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, shown);
            Member m = sorted.get(i);
            out.add(new Slot(m, cx + (int) Math.round(radius * Math.cos(a)), cy + (int) Math.round(radius * Math.sin(a)),
                    m.role().small() ? Math.max(MIN_SIZE, use / 2) : use, m.role().alpha()));
        }
        return new Layout(out, sorted.size() - shown, use);
    }

    /** Whether a box (a line of text) stands clear of every portrait and its ring ({@code pad} px of air), so a line under the model never runs into a face. */
    public static boolean clearOfFaces(Layout layout, int x, int y, int w, int h, int pad) {
        for (Slot s : layout.slots()) {
            int half = s.size() / 2 + 2 + pad;
            if (x < s.x() + half && s.x() - half < x + w && y < s.y() + half && s.y() - half < y + h) {
                return false;
            }
        }
        return true;
    }

    /** The y of the "+N more" line: under the lowest portrait and its ring, so it never sits on a face. */
    public static int moreLabelY(Layout layout, int cy) {
        int bottom = cy;
        for (Slot s : layout.slots()) {
            bottom = Math.max(bottom, s.y() + s.size() / 2 + 2);
        }
        return bottom + 3;
    }

    /** Room the names beside the side portraits take, and the radius from which the names are shown at all. */
    public static final int NAME_RESERVE = 52;
    public static final int NAMED_RADIUS = 40;
    public static final int MIN_RADIUS = 26;

    /**
     * The radius of the ring in an area {@code areaW} x {@code areaH}: with room for the names beside the side portraits when that leaves a ring of
     * {@link #NAMED_RADIUS} or more, else the ring grows to the width of the area (the names on the top and bottom still fit, the rest are in the tooltips).
     */
    public static int radiusFor(int areaW, int areaH, int face) {
        int byHeight = areaH / 2 - face / 2 - 12;
        int withNames = Math.min(areaW / 2 - NAME_RESERVE, byHeight);
        if (withNames >= NAMED_RADIUS) {
            return withNames;
        }
        return Math.max(MIN_RADIUS, Math.min(areaW / 2 - face / 2 - 4, byHeight));
    }

    /** The portraits of {@link #layout}. */
    public static List<Slot> ring(List<Member> members, int cx, int cy, int radius, int size) {
        return layout(members, cx, cy, radius, size).slots();
    }
}