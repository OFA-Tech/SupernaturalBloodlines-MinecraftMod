package net.ofatech.supernaturalbloodlines.core.blood;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;

/** Who may be made to drink a vampire's blood, and what it may do to them (R208). */
public final class ForceFeedRules {
    public enum Refusal { NOT_A_PLAYER, SELF, VERVAIN, NOT_WILLING }

    private ForceFeedRules() {
    }

    /**
     * Consent comes before vervain, so a refusal for vervain is only ever heard by someone who could have fed the target otherwise: it does not say what a
     * target who could not be fed anyway is carrying.
     *
     * @param willing        the target is sneaking: they accept
     * @param pvp            the server allows players to hurt each other
     * @param healthFraction the target's health over their maximum
     * @param downed         the target is down (dying, snapped, or in the turning's screen)
     * @param healthLimit    an unwilling target at or under this fraction may be fed
     * @return the refusal, empty when the blood may be given (claims are asked afterwards)
     */
    public static Optional<Refusal> check(boolean targetIsPlayer, boolean self, boolean vervain, boolean willing,
                                          boolean pvp, float healthFraction, boolean downed, float healthLimit) {
        if (!targetIsPlayer) {
            return Optional.of(Refusal.NOT_A_PLAYER);
        }
        if (self) {
            return Optional.of(Refusal.SELF);
        }
        if (!willing && !(pvp && (downed || healthFraction <= healthLimit))) {
            return Optional.of(Refusal.NOT_WILLING);
        }
        return vervain ? Optional.of(Refusal.VERVAIN) : Optional.empty();
    }

    /**
     * What a drink does to someone who did not accept it: blood forced on a player never names their sire (their line is theirs to choose) and never
     * holds or ruins a werewolf's transition by naming one. A mark (vampire blood in the system) and the rest are unchanged.
     */
    public static Effect withoutConsent(Effect effect) {
        return switch (effect) {
            case NAME_SIRE, TRANSITION_BLOOD -> Effect.REFUSED;
            case MARK_AND_NAME_SIRE -> Effect.MARK;
            default -> effect;
        };
    }
}
