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
}
