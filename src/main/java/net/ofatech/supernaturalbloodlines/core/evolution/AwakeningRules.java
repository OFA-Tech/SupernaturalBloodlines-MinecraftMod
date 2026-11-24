package net.ofatech.supernaturalbloodlines.core.evolution;

/**
 * What Ascension's Awakening Altar asks of a player before it grants an Ultimate, as numbers (spec 8.2, E2): the rule {@code doAscensionUltimate}, the
 * cooldown since the player's last awakening, Max EP at or above the addon's threshold, and a true Hero or true Demon Lord. The values are read live from the
 * addon by {@code AscensionProbe}; nothing here is a copy of one. The pair's own gate (the Unique held and mastered) is the recipe's {@code needs}. Pure.
 */
public final class AwakeningRules {
    private AwakeningRules() {
    }

    public record Facts(boolean ruleOn, long now, long lastAwakeningTick, int cooldownMinutes, double maxEp, double minEp, boolean trueHero, boolean trueDemonLord) {
    }

    /** The cooldown in game ticks: the addon's number of "minutes" of 60 s of 20 ticks (its comment says real time; its check counts game ticks). */
    public static long cooldownTicks(int minutes) {
        return (long) minutes * 60L * 20L;
    }

    /** The altar refuses while {@code now - last < ticks}, and never for a player who has not awakened ({@code last == 0}) or when the cooldown is 0. */
    public static boolean coolingDown(long now, long last, int minutes) {
        long ticks = cooldownTicks(minutes);
        return ticks > 0L && last != 0L && now - last < ticks;
    }

    public static boolean enoughEp(double maxEp, double minEp) {
        return maxEp >= minEp;
    }

    public static boolean qualifyingStatus(boolean hero, boolean demonLord) {
        return hero || demonLord;
    }

    public static boolean satisfied(Facts f) {
        return f.ruleOn() && !coolingDown(f.now(), f.lastAwakeningTick(), f.cooldownMinutes()) && enoughEp(f.maxEp(), f.minEp()) && qualifyingStatus(f.trueHero(), f.trueDemonLord());
    }
}
