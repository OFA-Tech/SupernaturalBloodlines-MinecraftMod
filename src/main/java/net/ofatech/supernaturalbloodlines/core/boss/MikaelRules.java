package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R155: Mikael hunts a vampire player of high enough rung, at night, on the ground, outside a mountain ash ring, with a chance per night. He leaves
 * Originals and Original hybrids alone unless {@code mikael_stakes_originals} is on (R158: his white oak would kill them outright and their line with them).
 */
public final class MikaelRules {
    /** How long (ticks) he waits for a prey who is gone, or for a night that does not come, before he withdraws. */
    public static final int WITHDRAW_AFTER = 1200;

    /**
     * @param original         the player is an Original or an Original hybrid
     * @param stakesOriginals  {@code mikael_stakes_originals}
     */
    public record Facts(boolean vampire, int rung, int minRung, boolean night, boolean overworld, boolean onGround, boolean inAshRing,
                        boolean original, boolean stakesOriginals) {
        /** The facts of a hunt that has no Original in it (most of the tests). */
        public Facts(boolean vampire, int rung, int minRung, boolean night, boolean overworld, boolean onGround, boolean inAshRing) {
            this(vampire, rung, minRung, night, overworld, onGround, inAshRing, false, false);
        }
    }

    private MikaelRules() {
    }

    /** Whether this player is a target now (before the roll). */
    public static boolean eligible(Facts f) {
        return f.vampire() && f.rung() >= f.minRung() && f.night() && f.overworld() && f.onGround() && !f.inAshRing() && mayHuntOriginal(f.original(), f.stakesOriginals());
    }

    /** R158: an Original is hunted only when the config lets his stake kill one. */
    public static boolean mayHuntOriginal(boolean original, boolean stakesOriginals) {
        return !original || stakesOriginals;
    }

    /** Whether he goes after this prey right now: it is night, and the prey is not an Original he must not touch. */
    public static boolean mayChase(boolean night, boolean original, boolean stakesOriginals) {
        return night && mayHuntOriginal(original, stakesOriginals);
    }

    /** He is stranded when his prey is not in his level, or when it is day and he has nobody to fight. */
    public static boolean stranded(boolean preyHere, boolean night, boolean hasTarget) {
        return !preyHere || (!night && !hasTarget);
    }

    /** Whether the time he has been stranded is enough for him to withdraw. */
    public static boolean withdraws(int strandedTicks) {
        return strandedTicks >= WITHDRAW_AFTER;
    }

    /** The roll against the chance: a roll of exactly the chance does not pass. */
    public static boolean picks(Facts f, double chance, double roll) {
        return eligible(f) && roll < chance;
    }

    /** Night is from tick 13000 to 22999 of the day (a vampire's hours, and when the sky is dark enough to hunt). */
    public static boolean isNight(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        return tick >= 13000L && tick < 23000L;
    }

    /** Share of the damage Mikael heals when he hits a vampire (R156): he feeds on them. */
    public static final float FEED_SHARE = 0.25F;

    public static float feedHeal(float damage, boolean victimIsVampire) {
        return victimIsVampire && damage > 0 ? damage * FEED_SHARE : 0F;
    }
}
