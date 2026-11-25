package net.ofatech.supernaturalbloodlines.core.group.coven;

/** Who may receive a ring forged by a circle, and how big the circle must be (R262, R263). */
public final class RingRules {
    private RingRules() {
    }

    /** A Moon Ring is for a werewolf who has a pack. */
    public static boolean moonReceiverOk(boolean werewolfFaction, boolean inAPack) {
        return werewolfFaction && inAPack;
    }

    /** A Sun Ring is for a vampire; with the Day Walker switch on, for a Day Walker of a clan only. */
    public static boolean sunReceiverOk(boolean vampireFaction, boolean dayWalkerOfAClan, boolean dayWalkerOnly) {
        return vampireFaction && (!dayWalkerOnly || dayWalkerOfAClan);
    }

    public static boolean circleOk(int size, int minCircle) {
        return size >= minCircle;
    }
}