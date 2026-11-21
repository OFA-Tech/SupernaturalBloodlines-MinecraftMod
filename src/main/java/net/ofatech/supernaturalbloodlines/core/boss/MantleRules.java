package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R191: the Mantle of the Beast, the Beast's one reward (no spark, no stacked spark, no pike). A werewolf type who wears it (Curios back slot, or carries it where
 * Curios is not installed) while shifted is wrapped in shadows, and every {@link #COOLDOWN_SECONDS} the Shadow Wrap weakens and slows the enemies within
 * {@link #RANGE} blocks for {@link #WRAP_SECONDS}. Everybody else has a cloak with no effect, so it is a trophy and never something to farm. The wrap spares
 * the wearer, their pack and anybody who cannot be hurt.
 */
public final class MantleRules {
    public static final int WRAP_SECONDS = 20;
    public static final int COOLDOWN_SECONDS = 120;
    public static final double RANGE = 6.0;

    private MantleRules() {
    }

    /** Whether the Mantle does anything for its wearer now. */
    public static boolean active(boolean shifted, boolean werewolfType, boolean worn) {
        return worn && shifted && werewolfType;
    }

    public static boolean wrapReady(long now, long readyAt) {
        return now >= readyAt;
    }

    public static long nextWrapAt(long now) {
        return now + COOLDOWN_SECONDS * 20L;
    }

    /** Whether an enemy takes the wrap: in range (the edge included), not the wearer, not a packmate, and one who can be hurt. */
    public static boolean wraps(double distanceSquared, boolean wearer, boolean packmate, boolean canBeHurt) {
        return !wearer && !packmate && canBeHurt && distanceSquared <= RANGE * RANGE;
    }
}
