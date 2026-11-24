package net.ofatech.supernaturalbloodlines.core.bloodline;

public enum Species {
    NONE, ORIGINAL, ORIGINAL_HYBRID, HYBRID, WITCH, SIPHONER, HERETIC, TRIBRID, POTENTIAL, HUNTER_OF_THE_FIVE, AUGUSTINE;

    /** A hybrid, an Original hybrid or a tribrid: the one definition of "hybrid type" (the werewolf side that the curse can bind). */
    public boolean hybridType() {
        return this == HYBRID || this == ORIGINAL_HYBRID || this == TRIBRID;
    }

    /** A witch, a siphoner, a heretic or a tribrid: who may work the witch-type rites (R301, R315). */
    public boolean witchType() {
        return this == WITCH || this == SIPHONER || this == HERETIC || this == TRIBRID;
    }
}
