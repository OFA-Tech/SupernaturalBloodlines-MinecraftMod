package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R173: Kol comes by night for a Hunter of the Five whose mark has at least {@code kol_min_mark} kills (the mark is the number of prey they have killed), in
 * the overworld, on the ground, who is not a spectator. He is fast and leaps, and has no other ability. With nobody to hunt and nobody to fight for a minute
 * he withdraws and may come again. Kol's Grimoire pays its skill points once (R174).
 */
public final class KolRules {
    /** Ticks with nobody to hunt and nobody to fight before he withdraws (1 minute). */
    public static final int WITHDRAW_AFTER = 1200;
    /** The leap: from this far to this far (blocks), then a pause (ticks) before the next one. */
    public static final double LEAP_NEAREST = 3.0;
    public static final double LEAP_FARTHEST = 9.0;
    public static final int LEAP_PAUSE = 80;

    private KolRules() {
    }

    public record Facts(boolean hunter, int markKills, boolean overworld, boolean onGround, boolean spectator) {
    }

    public static boolean eligible(Facts f, int minMark) {
        return f.hunter() && f.markKills() >= Math.max(1, minMark) && f.overworld() && f.onGround() && !f.spectator();
    }

    public static boolean stranded(boolean preyHere, boolean hasTarget) {
        return !preyHere && !hasTarget;
    }

    public static boolean withdraws(int strandedTicks) {
        return strandedTicks >= WITHDRAW_AFTER;
    }

    /** Whether he leaps now: the target is neither too near nor too far, he stands on the ground and the pause is over. */
    public static boolean leaps(double distanceSq, boolean onGround, int pauseLeft) {
        return onGround && pauseLeft <= 0 && distanceSq >= LEAP_NEAREST * LEAP_NEAREST && distanceSq <= LEAP_FARTHEST * LEAP_FARTHEST;
    }

    /** The skill points a reading of Kol's Grimoire pays: nothing for one that was read before. */
    public static int grimoireSp(boolean alreadyRead, int configured) {
        return alreadyRead ? 0 : Math.max(0, configured);
    }
}
