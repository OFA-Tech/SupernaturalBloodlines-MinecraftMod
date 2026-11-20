package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R151: what a blow does to an NPC Original. White oak kills only an Original that is open (desiccated), unless {@code npc_original_stake_needs_open}
 * is off; on a closed one it only hurts (the blow cannot take the last health). Any other lethal blow, and the Ash Dagger, desiccates a closed
 * Original; an open one cannot be hurt further than 1 health by anything but white oak.
 */
public final class DefeatRules {
    public enum Kind { WHITE_OAK, ASH_DAGGER, OTHER }

    /** HURT: the damage lands as it is; SPARE: it lands but leaves 1 health; DESICCATE: the Original goes down; DIE: they die for good. */
    public enum Verdict { HURT, SPARE, DESICCATE, DIE }

    private DefeatRules() {
    }

    public static Verdict verdict(Kind kind, boolean lethal, boolean open, boolean stakeNeedsOpen) {
        if (kind == Kind.ASH_DAGGER) {
            return open ? Verdict.SPARE : Verdict.DESICCATE;
        }
        if (!lethal) {
            return Verdict.HURT;
        }
        if (kind == Kind.WHITE_OAK) {
            return open || !stakeNeedsOpen ? Verdict.DIE : Verdict.SPARE;
        }
        return open ? Verdict.SPARE : Verdict.DESICCATE;
    }
}