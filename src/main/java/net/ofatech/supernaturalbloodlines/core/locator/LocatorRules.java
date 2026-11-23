package net.ofatech.supernaturalbloodlines.core.locator;

import java.util.Random;

/** The locator as pure rules (R316, R325): one answer for every reason somebody cannot be found, a blurred point, and a distance in words. */
public final class LocatorRules {
    public record Pt(double x, double z) {}

    /** {@code veryNear} under fifty blocks; otherwise {@code blocks} rounded to the nearest hundred. */
    public record Distance(boolean veryNear, int blocks) {}

    private LocatorRules() {
    }

    /**
     * Whether the needle may point at the target. Every reason it may not (the setting, offline, dead, another dimension, out of range, cloaked, creative or
     * spectator, the attunement spent) is the same {@code false}, so nothing about them can be read from the needle.
     *
     * @param range blocks; 0 means the whole dimension
     */
    public static boolean findable(boolean enabled, boolean online, boolean alive, boolean sameDimension, double distanceSqr, int range,
                                   boolean cloaked, boolean spectator, long now, long until) {
        if (!enabled || !online || !alive || !sameDimension || cloaked || spectator || now >= until) {
            return false;
        }
        return range <= 0 || distanceSqr <= (double) range * range;
    }

    /**
     * The point the needle shows: the middle of the {@code cell}-block cell the target stands in, moved by an offset that depends only on the cell and
     * the seed, so stepping inside a cell does not move it and the exact position never leaves the server.
     */
    public static Pt blur(double x, double z, long seed, int cell) {
        int cellX = Math.floorDiv((int) Math.floor(x), cell);
        int cellZ = Math.floorDiv((int) Math.floor(z), cell);
        Random r = new Random(seed ^ (cellX * 73_856_093L) ^ (cellZ * 19_349_663L));
        double spread = cell / 2.0;
        double ox = (r.nextDouble() - 0.5) * spread;
        double oz = (r.nextDouble() - 0.5) * spread;
        return new Pt(cellX * (double) cell + cell / 2.0 + ox, cellZ * (double) cell + cell / 2.0 + oz);
    }

    public static Distance distance(int blocks) {
        if (blocks < 50) {
            return new Distance(true, 0);
        }
        return new Distance(false, (blocks + 50) / 100 * 100);
    }
}
