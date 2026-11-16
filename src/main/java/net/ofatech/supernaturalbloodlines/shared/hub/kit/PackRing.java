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
    /** Room between two portraits on the ring. */
    private static final int GAP = 2;

    /** How many portraits of {@code size} fit round a ring of {@code radius}. */
    public static int capacity(int radius, int size) {
        return Math.max(1, (int) (2 * Math.PI * radius) / (size + GAP));
    }

    /**
     * The members from the top clockwise, the roles in their order (Alphas first); subordinates at half size and half opacity. Portraits shrink down to
     * {@link #MIN_SIZE} as the members grow, and past what fits the last ones (the lowest roles) are left out and counted in {@code hidden}.
     */
    public static Layout layout(List<Member> members, int cx, int cy, int radius, int size) {
        List<Member> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingInt(m -> m.role().ordinal()));
        int use = size;
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

    /** The portraits of {@link #layout}. */
    public static List<Slot> ring(List<Member> members, int cx, int cy, int radius, int size) {
        return layout(members, cx, cy, radius, size).slots();
    }
}