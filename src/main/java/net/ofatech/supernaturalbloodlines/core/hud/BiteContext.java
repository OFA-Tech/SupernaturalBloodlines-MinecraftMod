package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Everything {@link BiteRules} looks at.
 *
 * @param werewolfFaction      in the Werewolves faction (werewolf, hybrid, Original hybrid, tribrid)
 * @param wolfForm             in a transformed Werewolves form (the one in which Werewolves itself lets the player bite)
 * @param vampirismVampire     in the Vampirism vampire faction (a Ghoul too)
 * @param hybridType           a hybrid, Original hybrid or tribrid (plan 04's {@code HybridTraits.isHybrid})
 * @param inTurning            in plan 11's turning
 * @param turningWouldComplete a human-like bite would complete the turning (not a vampire, not a hunter or werewolf who would only end it quietly)
 */
public record BiteContext(boolean werewolfFaction, boolean wolfForm, boolean vampirismVampire, boolean hybridType, boolean inTurning,
        boolean turningWouldComplete) {
}
