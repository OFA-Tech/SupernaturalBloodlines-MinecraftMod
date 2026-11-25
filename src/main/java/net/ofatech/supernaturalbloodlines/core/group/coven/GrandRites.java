package net.ofatech.supernaturalbloodlines.core.group.coven;

import java.util.Map;

/** The three grand rites of a coven (R255, R264 to R268): which circle tier each needs, and how long a coven waits to cast the same one again. */
public final class GrandRites {
    public static final String FETE = "fete_des_benedictions";
    public static final String NIGHT_WATCH = "night_watch";
    public static final String GREAT_HARVEST = "great_harvest";

    private static final Map<String, Integer> TIER = Map.of(FETE, 1, NIGHT_WATCH, 2, GREAT_HARVEST, 3);

    private GrandRites() {
    }

    /** 1 to 3, or 0 for a rite that is not a grand rite. */
    public static int tierOf(String rite) {
        return TIER.getOrDefault(rite, 0);
    }

    /** The linked members the rite needs: its tier's size from the config, raised (never lowered) by a {@code min_circle} in the rite's own file. */
    public static int minCircle(String rite, int fileMin, int tier1, int tier2, int tier3) {
        int second = Math.max(tier1, tier2);
        int third = Math.max(second, tier3);
        int byTier = switch (tierOf(rite)) {
            case 1 -> tier1;
            case 2 -> second;
            case 3 -> third;
            default -> 0;
        };
        return Math.max(fileMin, byTier);
    }

    public static long cooldownUntil(long now, int hours) {
        return now + hours * 60L * 60 * 1000;
    }
}