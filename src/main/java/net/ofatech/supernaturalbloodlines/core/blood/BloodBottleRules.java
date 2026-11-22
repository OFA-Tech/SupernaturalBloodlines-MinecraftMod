package net.ofatech.supernaturalbloodlines.core.blood;

/** What one drink of a bottle does to the drinker (R210), as a table. */
public final class BloodBottleRules {
    /** HUMAN = no faction, a Ghoul, or someone in the turning. */
    public enum Drinker { HUMAN, WEREWOLF, WEREWOLF_IN_TRANSITION, OTHER }

    /** NAME_SIRE only records the source as the pending sire (a human already in the turning); TRANSITION_BLOOD does the same for a werewolf. */
    public enum Effect { MARK, MARK_AND_NAME_SIRE, NAME_SIRE, TRANSITION_BLOOD, UNSTABLE, NO_EFFECT, REFUSED }

    private BloodBottleRules() {
    }

    /**
     * @param canMark          vampire blood would change this drinker (a human outside the turning)
     * @param startsTransition the source may start the hybrid transition under {@code hybrid_transition_blood}
     */
    public static Effect decide(Drinker d, BloodKind k, boolean canMark, boolean startsTransition) {
        return switch (d) {
            case HUMAN -> canMark ? (k.namesSire() ? Effect.MARK_AND_NAME_SIRE : Effect.MARK)
                    : (k.namesSire() ? Effect.NAME_SIRE : Effect.REFUSED);
            case WEREWOLF -> k.namesSire() ? (startsTransition ? Effect.TRANSITION_BLOOD : Effect.REFUSED) : Effect.NO_EFFECT;
            case WEREWOLF_IN_TRANSITION -> k.namesSire() ? (startsTransition ? Effect.TRANSITION_BLOOD : Effect.REFUSED) : Effect.UNSTABLE;
            case OTHER -> Effect.REFUSED;
        };
    }
}
