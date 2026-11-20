package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.Optional;

/** R154: one table of reasons a boss is not spawned, checked in this order. An empty answer means it may be. */
public final class SpawnRules {
    public static final String DISABLED = "disabled";
    public static final String NEEDS_MOD = "needs_mod";
    public static final String DEAD = "dead";
    public static final String PRESENT = "present";
    public static final String COOLING = "cooling";

    private SpawnRules() {
    }

    public static Optional<String> refusal(BossFacts f) {
        if (!f.enabled()) {
            return Optional.of(DISABLED);
        }
        if (!f.modsPresent()) {
            return Optional.of(NEEDS_MOD);
        }
        if (f.state() == BossState.DEAD) {
            return Optional.of(DEAD);
        }
        if (f.state() == BossState.ACTIVE || f.state() == BossState.OPEN) {
            return Optional.of(PRESENT);
        }
        return f.cooling() ? Optional.of(COOLING) : Optional.empty();
    }
}