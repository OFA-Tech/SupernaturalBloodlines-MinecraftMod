package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R172: the Demon Wolf Roar (S-WOL-11, granted by Deucalion's death). On the Alpha Roar's key, with a cooldown of its own, it makes the other Alphas
 * within 32 blocks shift: player Alphas who are werewolves, not the roarer and not the roarer's own pack. A Beta is not an Alpha and does not shift.
 */
public final class DemonRoarRules {
    public static final String SKILL = "S-WOL-11";
    public static final double RANGE = 32.0;
    public static final int COOLDOWN = 2400;

    private DemonRoarRules() {
    }

    public static boolean shifts(boolean werewolf, boolean alpha, boolean self, boolean ownPack, boolean sameLevel, double distanceSquared) {
        return werewolf && alpha && !self && !ownPack && sameLevel && distanceSquared <= RANGE * RANGE;
    }

    /** {@code last} is the server tick of the last roar, or -1 for none. */
    public static boolean ready(long last, long now) {
        return last < 0 || now - last >= COOLDOWN;
    }

    public static long secondsLeft(long last, long now) {
        return Math.max(0, (COOLDOWN - (now - last) + 19) / 20);
    }
}
