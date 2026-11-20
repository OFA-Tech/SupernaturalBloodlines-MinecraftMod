package net.ofatech.supernaturalbloodlines.core.moon;

/**
 * The "lunar mood" of a night: a second layer on top of the real moon, read from Enhanced Celestials 2 (or nothing). It never replaces
 * {@link MoonRule}: rites, the Werewolves forced shift and the Werewolves helper keep reading the real phase and time. Every factor of
 * {@link #NONE} is 1. Pure.
 */
public record LunarMood(Kind kind, boolean superMoon) {
    public enum Kind { NONE, BLOOD, BLUE, HARVEST, ECLIPSE }

    public static final LunarMood NONE = new LunarMood(Kind.NONE, false);
    public static final String EC_DEFAULT_NAMESPACE = "enhancedcelestials2defaultlunarevents";
    public static final String ECLIPSE_NAMESPACE = "supernaturalbloodlines";
    public static final String ECLIPSE_PATH = "lunar_eclipse";
    /** The rites that need the full moon and that a super moon makes cheaper (Severance and the Alpha Trial also need it and are left out on purpose, see {@code FullMoonRitesDataTest}). A test keeps this and that list equal to the rite data. */
    public static final java.util.Set<String> FULL_MOON_RITES = java.util.Set.of("supernaturalbloodlines:rite_of_the_moon", "supernaturalbloodlines:silver_moon", "supernaturalbloodlines:immortality");

    /** The mood of an Enhanced Celestials event id. Only the Default Lunar Events and our own eclipse are known; any other event is {@link #NONE}. */
    public static LunarMood ofEvent(String namespace, String path) {
        if (ECLIPSE_NAMESPACE.equals(namespace)) {
            return ECLIPSE_PATH.equals(path) ? new LunarMood(Kind.ECLIPSE, false) : NONE;
        }
        if (!EC_DEFAULT_NAMESPACE.equals(namespace)) {
            return NONE;
        }
        boolean sup = path.startsWith("super_");
        String name = sup ? path.substring("super_".length()) : path;
        return switch (name) {
            case "blood_moon" -> new LunarMood(Kind.BLOOD, sup);
            case "blue_moon" -> new LunarMood(Kind.BLUE, sup);
            case "harvest_moon" -> new LunarMood(Kind.HARVEST, sup);
            case "moon" -> new LunarMood(Kind.NONE, sup);
            default -> NONE;
        };
    }

    /** {@code amount} times {@code factor}, rounded half up. A factor of 1 leaves it alone; an amount of 1 does not grow by a quarter. */
    public static int scaled(int amount, double factor) {
        return factor == 1 ? amount : (int) Math.round(amount * factor);
    }

    public boolean isNone() {
        return kind == Kind.NONE && !superMoon;
    }

    /**
     * Rite altar cost factor: a Blue Moon makes every rite cheaper ({@code blue}), a super moon makes the full-moon rites cheaper on top
     * ({@code superFactor}). It never lets a full-moon rite run on a night that is not a full moon.
     */
    public double riteCost(boolean fullMoonRite, double blue, double superFactor) {
        double factor = kind == Kind.BLUE ? blue : 1;
        return superMoon && fullMoonRite ? factor * superFactor : factor;
    }

    /** A vampire's bite on a Blood Moon gives {@code bonus} times the blood. */
    public double biteFactor(double bonus) {
        return kind == Kind.BLOOD ? bonus : 1;
    }



    /** NPC werewolf packs spawn on a Blood Moon at {@code factor} of the full-moon chance (R143). */
    public double packChance(double factor) {
        return kind == Kind.BLOOD ? factor : 1;
    }

    /** A Super Blue Moon skips the Werewolves forced shift for everybody, when the switch is on. */
    public boolean sparesForcedShift(boolean switchOn) {
        return switchOn && superMoon && kind == Kind.BLUE;
    }
}
