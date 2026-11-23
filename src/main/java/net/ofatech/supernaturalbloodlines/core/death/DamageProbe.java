package net.ofatech.supernaturalbloodlines.core.death;

import java.util.function.Supplier;

/**
 * A skill that must ask "would this hit be allowed here?" (Rip Heart, Neck Snap: a claim, a duel, a protected player) posts a real
 * {@code LivingIncomingDamageEvent} and deals nothing. While it does, {@link #active()} is true on that thread: every listener of ours that does something
 * when damage arrives (counts a blow, desiccates an NPC Original, marks an attacker) returns early, so a probe leaves no trace. Listeners that only
 * cancel or scale the damage keep running, because their answer is what the probe asks for. Another mod's listener cannot know: it sees a hit of the probe's
 * (tiny) amount that never lands (see the species docs). Force-feeding asks a claim mod the same way with a posted {@code EntityInteract}: our listeners that
 * pull a dagger, siphon or compel on a click return early too.
 */
public final class DamageProbe {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DamageProbe() {
    }

    public static boolean active() {
        return ACTIVE.get();
    }

    public static <T> T run(Supplier<T> probe) {
        boolean outer = ACTIVE.get();
        ACTIVE.set(Boolean.TRUE);
        try {
            return probe.get();
        } finally {
            ACTIVE.set(outer);
        }
    }
}
