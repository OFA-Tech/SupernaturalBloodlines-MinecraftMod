package net.ofatech.supernaturalbloodlines.core.blood;

import java.util.Optional;

/** Who may be made to drink a vampire's blood (R208). */
public final class ForceFeedRules {
    public enum Refusal { NOT_A_PLAYER, SELF, VERVAIN, NOT_WILLING }

    private ForceFeedRules() {
    }

    /**
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
        if (vervain) {
            return Optional.of(Refusal.VERVAIN);
        }
        if (willing || pvp && (downed || healthFraction <= healthLimit)) {
            return Optional.empty();
        }
        return Optional.of(Refusal.NOT_WILLING);
    }
}
