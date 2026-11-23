package net.ofatech.supernaturalbloodlines.core.bloodline;

public enum Species {
    NONE, ORIGINAL, ORIGINAL_HYBRID, HYBRID, WITCH, SIPHONER, HERETIC, TRIBRID, POTENTIAL, HUNTER_OF_THE_FIVE, AUGUSTINE;

    /** A hybrid, an Original hybrid or a tribrid: the one definition of "hybrid type" (the werewolf side that the curse can bind). */
    public boolean hybridType() {
        return this == HYBRID || this == ORIGINAL_HYBRID || this == TRIBRID;
    }
}
