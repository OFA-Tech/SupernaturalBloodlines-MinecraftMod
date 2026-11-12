package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Result of a rule: the new state, side effects for the caller to perform, and a refusal key if refused. */
public record Outcome(BloodlineState state, List<Action> actions, @Nullable String refusalKey) {

    public static final String REFUSAL_PREFIX = "supernaturalbloodlines.refusal.";

    public enum Action { JOIN_VAMPIRE, JOIN_WEREWOLF, LEAVE_FACTION, KILL_PLAYER }

    public Outcome {
        actions = List.copyOf(actions);
    }

    public boolean refused() {
        return refusalKey != null;
    }

    static Outcome of(BloodlineState state, Action... actions) {
        return new Outcome(state, List.of(actions), null);
    }

    /** Refusals leave the state unchanged. */
    static Outcome refuse(BloodlineState unchanged, String reason) {
        return new Outcome(unchanged, List.of(), REFUSAL_PREFIX + reason);
    }
}
