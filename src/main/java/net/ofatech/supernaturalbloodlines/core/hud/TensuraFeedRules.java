package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Tensura's race-ability feed (the vampire race's ability on a creature) for a Vampirism vampire (plan 13c, "one blood value"). Pure.
 * Tensura's feed hurts the victim for 2, heals the feeder 2 and eats 2 food. For a Vampirism vampire the food is not the value that matters, so the
 * same amount is credited as Vampirism blood, taken from the victim's Vampirism blood (a drained victim gives nothing, as in the hybrid feed).
 */
public final class TensuraFeedRules {
    /** What one Tensura feed is worth: its food value. */
    public static final int FEED = 2;

    private TensuraFeedRules() {
    }

    /** Blood credited to the vampire for one feed from a victim that has {@code victimBlood}. */
    public static int credit(int victimBlood, boolean victimBloodless) {
        return victimBloodless ? 0 : Math.max(0, Math.min(FEED, victimBlood));
    }

    /**
     * The answer to Tensura's {@code FoodData.needsFood()} inside its vampire race ability. Tensura feeds when it is hurt or this is true, or in creative.
     * A Vampirism vampire's food is never short, so for them "needs food" means "blood is not full"; everyone else keeps Tensura's own answer.
     */
    public static boolean needsFood(boolean original, boolean isVampirismVampire, boolean bloodFull) {
        return isVampirismVampire ? !bloodFull : original;
    }
}
