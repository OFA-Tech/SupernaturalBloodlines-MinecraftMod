package net.ofatech.supernaturalbloodlines.core.boss;

/** R155: Mikael hunts a vampire player of high enough rung, at night, on the ground, outside a mountain ash ring, with a chance per night. */
public final class MikaelRules {
    public record Facts(boolean vampire, int rung, int minRung, boolean night, boolean overworld, boolean onGround, boolean inAshRing) {
    }

    private MikaelRules() {
    }

    /** Whether this player is a target now (before the roll). */
    public static boolean eligible(Facts f) {
        return f.vampire() && f.rung() >= f.minRung() && f.night() && f.overworld() && f.onGround() && !f.inAshRing();
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

    /** The roll against the chance: a roll of exactly the chance does not pass. */
    public static boolean picks(Facts f, double chance, double roll) {
        return eligible(f) && roll < chance;
    }
}