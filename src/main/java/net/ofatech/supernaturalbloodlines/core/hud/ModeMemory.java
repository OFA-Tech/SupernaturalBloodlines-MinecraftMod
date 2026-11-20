package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.List;
import java.util.Optional;

/**
 * Which mode to show. The memory of the player's choice is the client {@code mode} setting itself (the mode key writes it, the hub Settings page edits it),
 * so a change in either applies at once and there is no second copy to drift. The modes on offer change under the player's feet (a login, a respawn or a
 * dimension change leaves {@code HudStanding} unsynced for a few ticks, so Supernatural mode is briefly not available): that falls back for that tick only,
 * and, since nothing is stored, the setting brings the choice back with the offer. Pure.
 */
public final class ModeMemory {
    /** A freshly created player entity (login, respawn, dimension change) has had no time to receive its standing for this many ticks. */
    public static final int STANDING_SETTLE_TICKS = 40;

    private ModeMemory() {
    }

    /** The mode to show for the saved {@code setting} (AUTO, TENSURA or SUPERNATURAL) and the modes on offer: the setting's mode if on offer, else the first one. */
    public static Optional<SlotMode> current(String setting, List<SlotMode> available) {
        return SlotMode.start(setting, available);
    }

    /** The mode key does something only with two modes on offer. */
    public static boolean canToggle(List<SlotMode> available) {
        return available.size() > 1;
    }

    public enum Hint { NONE, LOCKED, NOT_NOW }

    /**
     * What the mode key says when there is nothing to switch to, for a player with Tensura and Vampirism installed: that Supernatural mode unlocks when they
     * become supernatural, or, while their standing may not have synced yet, only that it is not available right now.
     *
     * @param playerAgeTicks ticks the local player entity has existed
     */
    public static Hint hint(boolean tensura, boolean vampirism, boolean supernatural, int playerAgeTicks) {
        if (!tensura || !vampirism || supernatural) {
            return Hint.NONE;
        }
        return playerAgeTicks >= STANDING_SETTLE_TICKS ? Hint.LOCKED : Hint.NOT_NOW;
    }
}
