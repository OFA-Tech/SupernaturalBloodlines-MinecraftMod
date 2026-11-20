package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Which mode the player chose, kept apart from which modes are on offer right now. The offer changes under the player's feet (a login, a respawn or a
 * dimension change leaves {@code HudStanding} unsynced for a few ticks, so Supernatural mode is briefly not available): the choice is stored only by
 * {@link #choose} (the mode key), never by reading the current mode, so an unavailable tick falls back for that tick only and the choice comes back with
 * the offer. Pure.
 */
public final class ModeMemory {
    @Nullable
    private SlotMode chosen;

    /** The player pressed the mode key and landed on {@code mode}. */
    public void choose(@Nullable SlotMode mode) {
        chosen = mode;
    }

    /** What the player chose, null before any choice (this session). */
    @Nullable
    public SlotMode chosen() {
        return chosen;
    }

    /**
     * The mode to show: the choice when it is on offer; else (no choice yet) the start mode of the saved setting; else the first mode on offer. Never stores
     * what it returns.
     */
    public Optional<SlotMode> current(String setting, List<SlotMode> available) {
        if (available.isEmpty()) {
            return Optional.empty();
        }
        if (chosen != null && available.contains(chosen)) {
            return Optional.of(chosen);
        }
        if (chosen == null) {
            return SlotMode.start(setting, available);
        }
        return Optional.of(available.get(0));
    }

    /** The mode key does something only with two modes on offer. */
    public static boolean canToggle(List<SlotMode> available) {
        return available.size() > 1;
    }

    /**
     * The mode key was pressed with Tensura and Vampirism installed but a player who is not supernatural: the key has nothing to switch to, and the player is
     * told why instead of getting silence.
     */
    public static boolean lockedHint(boolean tensura, boolean vampirism, boolean supernatural) {
        return tensura && vampirism && !supernatural;
    }
}
