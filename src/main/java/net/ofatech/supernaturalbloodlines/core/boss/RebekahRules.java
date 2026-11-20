package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R169: Rebekah comes {@code rebekah_delay_seconds} after a Cure was made at the rite (the {@code CureMadeEvent}), within 48 blocks of the circle, and
 * hunts whoever made it. She does not come for a Cure made while she lies desiccated, is dead, or is out there already, and a {@code /give} is no
 * event at all.
 */
public final class RebekahRules {
    /** How far from the circle she may arrive (blocks), and the nearest. */
    public static final int RADIUS = 48;
    public static final int NEAREST = 12;
    /** Ticks with nobody to hunt and nobody to fight before she withdraws (1 minute). */
    public static final int WITHDRAW_AFTER = 1200;

    private RebekahRules() {
    }

    /** Whether a Cure made now sends her: she has not been seen yet, or was lost; never when she is out there (active or down) or dead. */
    public static boolean answers(BossState state) {
        return state == BossState.UNSEEN || state == BossState.LOST;
    }

    public static long arrivesAt(long now, int delaySeconds) {
        return now + Math.max(0, delaySeconds) * 20L;
    }

    public static boolean due(long now, long arriveAt) {
        return arriveAt >= 0 && now >= arriveAt;
    }

    public static boolean stranded(boolean preyHere, boolean hasTarget) {
        return !preyHere && !hasTarget;
    }

    public static boolean withdraws(int strandedTicks) {
        return strandedTicks >= WITHDRAW_AFTER;
    }
}