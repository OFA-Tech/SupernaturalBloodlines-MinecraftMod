package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

/** R292: attack damage for each other brother who counts and stands near, up to a cap. */
public final class ArmsBonus {
    private ArmsBonus() {
    }

    /** The bonus as a fraction (0.12 is 12%). */
    public static double of(int brothers, int percentEach, int maxPercent) {
        if (brothers <= 0 || percentEach <= 0 || maxPercent <= 0) {
            return 0;
        }
        return Math.min((long) brothers * percentEach, maxPercent) / 100.0;
    }

    public static boolean counts(boolean fits, boolean breaker, boolean online, boolean alive, boolean spectator, boolean sameDimension, double distSq, int radius) {
        return fits && !breaker && online && alive && !spectator && sameDimension && distSq <= (double) radius * radius;
    }
}