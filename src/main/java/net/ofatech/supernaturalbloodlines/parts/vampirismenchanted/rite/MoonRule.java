package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

/** Pure, so a unit test can reach it without Minecraft. */
public final class MoonRule {
    private MoonRule() {
    }

    /** Dimensions with a fixed time (the Nether, the End) report phase 0 forever, which is not a full moon. */
    public static boolean isFullMoon(boolean hasFixedTime, int phase) {
        return !hasFixedTime && phase == 0;
    }
}
