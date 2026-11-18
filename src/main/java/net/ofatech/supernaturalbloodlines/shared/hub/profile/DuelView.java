package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.Optional;

/**
 * The state of the viewer's own duel as the Profile of the opponent says it: asked and waiting for the answer, in progress, or paused while
 * the opponent is away. Pure; the server sends the phase word and the ticks left, the client counts down.
 */
public record DuelView(Phase phase, String opponent, int seconds) {
    public enum Phase { ASKED, ON, PAUSED }

    public static Optional<DuelView> of(String token, String opponent, int ticksLeft) {
        Phase phase = switch (token) {
            case "asked" -> Phase.ASKED;
            case "on" -> Phase.ON;
            case "paused" -> Phase.PAUSED;
            default -> null;
        };
        return phase == null ? Optional.empty() : Optional.of(new DuelView(phase, opponent, Math.max(0, (ticksLeft + 19) / 20)));
    }

    public String langKey() {
        return "supernaturalbloodlines.hub.profile.duel." + phase.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 1:42 from a minute up, else 45 s. */
    public String clock() {
        return seconds >= 60 ? seconds / 60 + ":" + String.format("%02d", seconds % 60) : seconds + " s";
    }
}

