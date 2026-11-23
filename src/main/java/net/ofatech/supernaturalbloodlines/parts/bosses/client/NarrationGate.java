package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * When the boss row is said aloud. The whole row only when the SET of bosses changes (one comes or goes); when only the focus changes, just the focus boss, and not within
 * {@link #MIN_HOLD} ticks of the last thing said, so a focus that flips between two bosses is not read out at every flip. Decided from the ids alone, so nothing is looked up or
 * translated on a tick that says nothing. Pure.
 */
public final class NarrationGate {
    /** The least ticks between two sayings that are only a change of focus: the same hold as {@link BossFocus#MIN_HOLD}. */
    public static final long MIN_HOLD = BossFocus.MIN_HOLD;

    public enum Say {
        NONE, ROW, FOCUS
    }

    /** What was last said: which bosses there were, which was the focus, and when. */
    public record State(Set<UUID> set, UUID focus, long at) {
    }

    private NarrationGate() {
    }

    public static Say decide(@Nullable State last, Set<UUID> set, @Nullable UUID focus, long now) {
        if (set.isEmpty() || focus == null) {
            return Say.NONE;
        }
        if (last == null || !last.set().equals(set)) {
            return Say.ROW;
        }
        if (!Objects.equals(last.focus(), focus)) {
            return now - last.at() >= MIN_HOLD ? Say.FOCUS : Say.NONE;
        }
        return Say.NONE;
    }

    /** What has been said once {@code say} was done. A focus change that was held back leaves the state as it was, so it is said when the hold is over (if it still stands). */
    @Nullable
    public static State after(@Nullable State last, Say say, Set<UUID> set, @Nullable UUID focus, long now) {
        if (set.isEmpty() || focus == null) {
            return null;
        }
        return switch (say) {
            case NONE -> last;
            case ROW -> new State(Set.copyOf(set), focus, now);
            case FOCUS -> new State(last.set(), focus, now);
        };
    }
}
