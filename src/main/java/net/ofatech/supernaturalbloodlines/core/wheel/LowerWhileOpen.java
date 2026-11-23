package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Optional;

/**
 * A switch of another mod that we lower while the wheel is open and give back exactly as it was: what to write when the wheel opens (the lowered value, once), and what to
 * write when it closes (the value it had, once). A second open or close writes nothing, and a close that never saw an open writes nothing, so a value the player changed
 * in between is never overwritten by a stale one. Pure.
 */
public final class LowerWhileOpen {
    private boolean lowered;
    private boolean saved;

    /** @param current the switch's value now; the answer is what to write, empty when nothing */
    public Optional<Boolean> open(boolean current, boolean lowerTo) {
        if (lowered) {
            return Optional.empty();
        }
        lowered = true;
        saved = current;
        return current == lowerTo ? Optional.empty() : Optional.of(lowerTo);
    }

    public Optional<Boolean> close(boolean current, boolean lowerTo) {
        if (!lowered) {
            return Optional.empty();
        }
        lowered = false;
        return current == lowerTo && saved != lowerTo ? Optional.of(saved) : Optional.empty();
    }

    public boolean isLowered() {
        return lowered;
    }
}
