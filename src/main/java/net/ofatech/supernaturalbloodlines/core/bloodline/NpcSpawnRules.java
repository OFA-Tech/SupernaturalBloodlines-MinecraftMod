package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.function.DoubleSupplier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar.Night;

/**
 * When and how an NPC werewolf pack spawns (R143). Full-moon nights (and a running eclipse, which is one) roll at the full chance; a Blood Moon on
 * another night rolls at {@code blood_moon_pack_chance_factor} of it (it comes through {@code LunarMoods}); any other night never. A player is
 * rolled once a night, and a level has a most for the night. Pure: the dice are passed in so a test can draw any number.
 */
public final class NpcSpawnRules {
    /** The numbers of the config. */
    public record Tuning(double packChance, double loneChance, double alphaChance, int minSize, int maxSize, int maxPacksPerNight) {
    }

    /** What one roll gives: a pack ({@code wolves} of them, led by an Alpha when {@code alpha}), and, independently, a lone wolf. */
    public record Plan(boolean pack, boolean alpha, int wolves, boolean lone) {
        public static final Plan NOTHING = new Plan(false, false, 0, false);

        public boolean any() {
            return pack || lone;
        }
    }

    private NpcSpawnRules() {
    }

    /** The share of the full chance a night rolls at: 1 on a full moon or an eclipse, the Blood Moon factor on a Blood Moon, 0 otherwise. */
    public static double nightFactor(Night night, double bloodMoonFactor) {
        return switch (night) {
            case FULL_MOON, ECLIPSE -> 1;
            case BLOOD_MOON -> clamp01(bloodMoonFactor);
            case NONE -> 0;
        };
    }

    /**
     * One roll for one player. Draws, in this order: pack, Alpha, size, lone wolf (one {@code dice} call each, always, so a test can feed the numbers
     * in order). {@code followerLimit} is how many wolves an Alpha can lead (it reads the Alpha's own limit at run time); a pack without an Alpha has
     * no such limit.
     */
    public static Plan plan(Night night, double bloodMoonFactor, Tuning t, int followerLimit, DoubleSupplier dice) {
        double factor = nightFactor(night, bloodMoonFactor);
        double rPack = dice.getAsDouble();
        double rAlpha = dice.getAsDouble();
        double rSize = dice.getAsDouble();
        double rLone = dice.getAsDouble();
        if (factor <= 0) {
            return Plan.NOTHING;
        }
        boolean pack = rPack < clamp01(t.packChance()) * factor;
        boolean lone = rLone < clamp01(t.loneChance()) * factor;
        boolean alpha = pack && rAlpha < clamp01(t.alphaChance());
        int min = Math.max(1, t.minSize());
        int max = Math.max(min, t.maxSize());
        int wolves = min + (int) Math.min(max - min, Math.floor(rSize * (max - min + 1)));
        if (alpha) {
            wolves = Math.max(1, Math.min(wolves, followerLimit));
        }
        return new Plan(pack, alpha, pack ? wolves : 0, lone);
    }

    /** Whether a player may be rolled for now: not twice a night, and not once the level has had its most. */
    public static boolean mayRoll(long lastNight, long night, int packsThisNight, int maxPacksPerNight) {
        return lastNight != night && packsThisNight < Math.max(0, maxPacksPerNight);
    }

    /** R144: a wild werewolf is feral on a full-moon night in the dark (block and sky light 7 or less where it stands). */
    public static boolean feral(boolean fullMoonNight, int light) {
        return fullMoonNight && light <= 7;
    }

    /** The night a day time belongs to: its day number (the night window lies inside one day). */
    public static long nightOf(long dayTime) {
        return Math.floorDiv(dayTime, 24000L);
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
