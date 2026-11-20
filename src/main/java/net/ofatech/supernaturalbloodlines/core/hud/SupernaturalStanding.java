package net.ofatech.supernaturalbloodlines.core.hud;

/** Whether the player has anything supernatural: a plain human sees no Supernatural mode, its slots or its pill. Pure. */
public final class SupernaturalStanding {
    private SupernaturalStanding() {
    }

    /**
     * @param faction   Vampirism's current faction is set (vampire, werewolf or hunter)
     * @param species   a bloodline species other than none (Original, hybrid, tribrid, witch and the rest)
     * @param inTurning a human in the turning (about to become one, and able to bite to finish it)
     * @param ghoul     a ghoul
     */
    public static boolean is(boolean faction, boolean species, boolean inTurning, boolean ghoul) {
        return faction || species || inTurning || ghoul;
    }
}
