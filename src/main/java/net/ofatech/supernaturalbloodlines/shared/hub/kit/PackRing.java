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

    /** The members from the top clockwise, the roles in their order (Alphas first); subordinates at half size and half opacity. */
    public static List<Slot> ring(List<Member> members, int cx, int cy, int radius, int size) {
        List<Member> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingInt(m -> m.role().ordinal()));
        List<Slot> out = new ArrayList<>(sorted.size());
        int n = sorted.size();
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, n);
            Member m = sorted.get(i);
            out.add(new Slot(m, cx + (int) Math.round(radius * Math.cos(a)), cy + (int) Math.round(radius * Math.sin(a)),
                    m.role().small() ? Math.max(8, size / 2) : size, m.role().alpha()));
        }
        return out;
    }
}
