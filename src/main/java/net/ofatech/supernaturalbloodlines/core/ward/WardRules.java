package net.ofatech.supernaturalbloodlines.core.ward;

import java.util.Set;
import java.util.UUID;

/** R300 to R303 as pure rules: who a ward turns away, the box it guards, the way out of it, the invite list. No game classes. */
public final class WardRules {
    public static final int MAX_RADIUS = 24;

    public enum Kind { AREA, DOOR }

    public enum InviteResult { ADDED, ALREADY, FULL }

    public record Dir(double x, double z) {}

    public record Point(double x, double z) {}

    /** The cube of {@code half} cells on each side of the centre cell. */
    public record Box(int cx, int cy, int cz, int half) {
        public boolean contains(double x, double y, double z) {
            return x >= cx - half && x < cx + half + 1 && y >= cy - half && y < cy + half + 1 && z >= cz - half && z < cz + half + 1;
        }

        /** Inside the box grown by {@code margin} blocks on every side (the owner's voice carries that far, R305). */
        public boolean near(double x, double y, double z, double margin) {
            return x >= cx - half - margin && x < cx + half + 1 + margin && y >= cy - half - margin && y < cy + half + 1 + margin
                    && z >= cz - half - margin && z < cz + half + 1 + margin;
        }

        double minX() {
            return cx - half;
        }

        double maxX() {
            return cx + half + 1;
        }

        double minZ() {
            return cz - half;
        }

        double maxZ() {
            return cz + half + 1;
        }
    }

    private WardRules() {
    }

    /** Whether the ward turns this entity away (R302). Werewolves only when {@code blocksWolves}. */
    public static boolean blocks(boolean enabled, boolean standing, boolean vampiric, boolean wolfLike, boolean blocksWolves,
                                 boolean creativeOrSpectator, boolean owner, boolean invited) {
        if (!enabled || !standing || creativeOrSpectator || owner || invited) {
            return false;
        }
        return vampiric || blocksWolves && wolfLike;
    }

    /** A vampire's home has no barrier (owner decision): a vampiric owner may not raise a ward. */
    public static boolean mayOwn(boolean ownerVampiric) {
        return !ownerVampiric;
    }

    /** The unit vector toward the nearest face of the box, flat. */
    public static Dir pushDirection(Box b, double x, double z) {
        double west = x - b.minX();
        double east = b.maxX() - x;
        double north = z - b.minZ();
        double south = b.maxZ() - z;
        double min = Math.min(Math.min(west, east), Math.min(north, south));
        if (min == west) {
            return new Dir(-1, 0);
        }
        if (min == east) {
            return new Dir(1, 0);
        }
        if (min == north) {
            return new Dir(0, -1);
        }
        return new Dir(0, 1);
    }

    /** A flat point just outside the nearest face. */
    public static Point exitPoint(Box b, double x, double z) {
        Dir d = pushDirection(b, x, z);
        double margin = 1.0;
        if (d.x() < 0) {
            return new Point(b.minX() - margin, z);
        }
        if (d.x() > 0) {
            return new Point(b.maxX() + margin, z);
        }
        if (d.z() < 0) {
            return new Point(x, b.minZ() - margin);
        }
        return new Point(x, b.maxZ() + margin);
    }

    public static InviteResult invite(Set<UUID> list, UUID id, int max) {
        if (list.contains(id)) {
            return InviteResult.ALREADY;
        }
        if (list.size() >= max) {
            return InviteResult.FULL;
        }
        list.add(id);
        return InviteResult.ADDED;
    }

    public static int clampRadius(int r) {
        return Math.max(1, Math.min(MAX_RADIUS, r));
    }

    /** The upper end of {@code ward_max_per_owner}: the hub shows this many wards of one owner, so no setting can outgrow it. */
    public static final int MAX_PER_OWNER_LIMIT = 8;
    /** The invited person hears "you are invited" from the same owner at most this often (invite, withdraw, invite cannot spam them). */
    public static final long INVITE_NOTICE_TICKS = 1200;

    /** One point per block column just outside the box ({@code margin} out), round all four faces. */
    public static java.util.List<Point> perimeter(Box b, double margin) {
        java.util.List<Point> out = new java.util.ArrayList<>();
        for (double x = Math.floor(b.minX() - margin) + 0.5; x <= b.maxX() + margin; x++) {
            out.add(new Point(x, b.minZ() - margin));
            out.add(new Point(x, b.maxZ() + margin));
        }
        for (double z = Math.floor(b.minZ()) + 0.5; z <= b.maxZ(); z++) {
            out.add(new Point(b.minX() - margin, z));
            out.add(new Point(b.maxX() + margin, z));
        }
        return out;
    }
    /** How far below, and above, the spot of a vampire that is moved out a safe place is looked for: three blocks is the most a fall does no harm from. */
    public static final int SAFE_DOWN = 3;
    public static final int SAFE_UP = 8;

    /** Every flat point just outside the box, nearest face first (west, east, north, south on a tie). */
    public static java.util.List<Point> exitPoints(Box b, double x, double z, double margin) {
        double[] distance = {x - b.minX(), b.maxX() - x, z - b.minZ(), b.maxZ() - z};
        Point[] points = {new Point(b.minX() - margin, z), new Point(b.maxX() + margin, z), new Point(x, b.minZ() - margin), new Point(x, b.maxZ() + margin)};
        Integer[] order = {0, 1, 2, 3};
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> distance[i]));
        java.util.List<Point> out = new java.util.ArrayList<>();
        for (int i : order) {
            out.add(points[i]);
        }
        return out;
    }

    /** The height offsets tried for a safe spot: where it stands, then downwards, then upwards. */
    public static int[] heightOffsets(int down, int up) {
        int[] out = new int[1 + down + up];
        int n = 1;
        for (int d = 1; d <= down; d++) {
            out[n++] = -d;
        }
        for (int u = 1; u <= up; u++) {
            out[n++] = u;
        }
        return out;
    }

    public record Chunk(int x, int z) {}

    /** Every chunk the box overlaps (R301: the claims are read for all of them, not for the altar's). */
    public static java.util.List<Chunk> chunksOf(Box b) {
        java.util.List<Chunk> out = new java.util.ArrayList<>();
        for (int x = Math.floorDiv(b.cx() - b.half(), 16); x <= Math.floorDiv(b.cx() + b.half(), 16); x++) {
            for (int z = Math.floorDiv(b.cz() - b.half(), 16); z <= Math.floorDiv(b.cz() + b.half(), 16); z++) {
                out.add(new Chunk(x, z));
            }
        }
        return out;
    }
}
