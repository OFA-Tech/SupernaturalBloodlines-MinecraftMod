package net.ofatech.supernaturalbloodlines.core.wheel;

/**
 * What the wheel decides about Infinity Stones, from facts its client compat reads out of the other mod. Pure: the reading is {@code parts/hud/client/wheel/infinity}.
 * Infinity's own server is the authority (it re-validates every use); these rules only choose what is drawn and which call is made.
 */
public final class StoneRules {
    /** Whether the compat works: on, and the other mod's classes are what we read. */
    public enum State { ACTIVE, OFF_MISSING, OFF_BROKEN, OFF_SETTING }

    /** The possession actions of Infinity's {@code Possession}: {@code ACT_RELEASE = 0}, {@code ACT_CONTROL = 1}; the possession modes of {@code SoulNet}: none, spectate, control, mob. */
    public static final int ACT_RELEASE = 0;
    public static final int ACT_CONTROL = 1;
    public static final int SOUL_NONE = 0;
    public static final int SOUL_MOB = 3;

    private StoneRules() {
    }

    /**
     * With the compat on, the stones come from Infinity's own list and the plain Tensura entries of namespace {@code infinity} (a stone is a Tensura UNIQUE skill, and the
     * immortality skill is a power of the Soul stone) are not shown as well; off, they stay Tensura skills. Any other namespace is never hidden.
     */
    public static boolean hidesTensuraEntry(State state, String namespace) {
        return state == State.ACTIVE && "infinity".equals(namespace);
    }

    /**
     * What a click on a stone's current power does. A switch (a toggle or a passive switch) flips and the wheel stays open; a channel that is running is stopped by the same
     * click; a power that needs aim is aimed first; a channel that is not running starts (it is on while it runs, so it is a toggle too); anything else is used at once.
     */
    public static PowerKind kind(boolean isSwitch, boolean isChannel, boolean needsAim, boolean holdingThis) {
        if (isSwitch) {
            return PowerKind.TOGGLE;
        }
        if (isChannel && holdingThis) {
            return PowerKind.TOGGLE;
        }
        if (needsAim) {
            return PowerKind.AIMED;
        }
        return isChannel ? PowerKind.TOGGLE : PowerKind.INSTANT;
    }

    /** Infinity's {@code WheelPayloads.Use} phase: 0 use, 1 stop a channel, 2 the second option (only a dual power has one). */
    public static int phase(boolean isSwitch, boolean isChannel, boolean holdingThis, boolean second, boolean isDual) {
        if (isSwitch) {
            return 0;
        }
        if (isChannel && holdingThis) {
            return 1;
        }
        return second && isDual ? 2 : 0;
    }

    /**
     * A state received before this world was joined is another world's: Infinity's server sends the list when it changes (not at a fixed pace), so age alone says nothing,
     * but a list older than the join cannot be this player's.
     */
    public static boolean stale(long receivedAtMs, long joinedAtMs) {
        return receivedAtMs < joinedAtMs;
    }

    /** Soul control is on the wheel only while a possession runs and the compat can send the action. */
    public static boolean soulEntryShown(State state, boolean soulSupported, int soulMode) {
        return state == State.ACTIVE && soulSupported && soulMode != SOUL_NONE;
    }

    /** What his key sends: release in mob mode, control otherwise; -1 when nothing is possessed. */
    public static int soulAction(int soulMode) {
        return soulMode == SOUL_NONE ? -1 : soulMode == SOUL_MOB ? ACT_RELEASE : ACT_CONTROL;
    }

    /** His soul-control key is covered only when the wheel can do its job, or the player would have no way to release a possession. */
    public static boolean soulControlKeyCovered(State state, boolean soulSupported) {
        return state == State.ACTIVE && soulSupported;
    }
}
