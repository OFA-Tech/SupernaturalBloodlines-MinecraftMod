package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.List;

/**
 * The rules of a leadership duel and where the challenger stands, as far as this client can tell: the same dimension and the 64 blocks are known only when
 * the other player is loaded near them; the ten-minute cooldown is the server's to say (it replies when it refuses). Pure.
 */
public final class DuelChecks {
    public static final int RANGE = 64;

    public enum State { OK, FAIL, UNKNOWN }

    /** @param key lang suffix of the rule; {@code detail} what was measured ("31 blocks"), or empty */
    public record Check(String key, State state, String detail) {
    }

    private DuelChecks() {}

    /** @param distance blocks to the other player, or -1 when they are not loaded on this client; {@code inDuel} the viewer is in a challenge or a duel already */
    public static List<Check> of(double distance, boolean inDuel) {
        boolean known = distance >= 0;
        return List.of(
                new Check("dimension", known ? State.OK : State.UNKNOWN, ""),
                new Check("range", !known ? State.UNKNOWN : distance <= RANGE ? State.OK : State.FAIL, known ? Math.round(distance) + " blocks" : ""),
                new Check("cooldown", State.UNKNOWN, ""),
                new Check("free", inDuel ? State.FAIL : State.OK, ""));
    }
}

