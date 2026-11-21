package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R184: how the Beast's encounter moves. {@link #next} is the step the director takes every second from what it measures (the Beast's health, whether it stands
 * inside a closed ring of mountain ash, the Spirit's time); {@link #after} is the step an event makes. Nothing leaves {@code KILLED} or {@code GONE} but an operator's
 * reset, so nothing here can be farmed: the Spirit returns once, and a lost Host is replaced once.
 */
public final class BeastPhaseRules {
    /** The Beast collapses at this part of its health, or the moment it is trapped in mountain ash. */
    public static final float COLLAPSE_AT = 0.25F;
    /** The health the returning Beast has. */
    public static final float RETURN_HEALTH = 0.5F;
    /** A Host can be made twice in a world: the first, and the one after the first died. */
    public static final int MAX_HOSTS = 2;

    public enum Event { HOST_MADE, HOST_LOST, CHANGE, PIKE_HIT, RESET }

    private BeastPhaseRules() {
    }

    /**
     * The automatic step. {@code returnsOnce} is {@code beast_returns_once}; {@code returned} whether the Spirit already came back once. A Beast at 0 health is
     * dying, not collapsing, so nothing happens to it here.
     */
    public static BeastPhase next(BeastPhase phase, float healthFraction, boolean trapped, int spiritTicksLeft, boolean returned, boolean returnsOnce) {
        return switch (phase) {
            case BEAST -> healthFraction > 0F && (healthFraction <= COLLAPSE_AT || trapped) ? BeastPhase.SPIRIT : BeastPhase.BEAST;
            case SPIRIT -> spiritTicksLeft > 0 ? BeastPhase.SPIRIT : returnsOnce && !returned ? BeastPhase.RETURN_PENDING : BeastPhase.GONE;
            default -> phase;
        };
    }

    public static BeastPhase after(BeastPhase phase, Event event) {
        if (event == Event.RESET) {
            return BeastPhase.NONE;
        }
        return switch (event) {
            case HOST_MADE -> phase == BeastPhase.NONE || phase == BeastPhase.FAILED ? BeastPhase.HOST : phase;
            case HOST_LOST -> phase == BeastPhase.HOST || phase == BeastPhase.SPIRIT || phase == BeastPhase.RETURN_PENDING ? BeastPhase.FAILED : phase;
            case CHANGE -> phase == BeastPhase.HOST || phase == BeastPhase.RETURN_PENDING ? BeastPhase.BEAST : phase;
            case PIKE_HIT -> phase == BeastPhase.SPIRIT ? BeastPhase.KILLED : phase;
            default -> phase;
        };
    }

    /** Whether a Host may be made: nothing started yet, or the first one died, and fewer than {@link #MAX_HOSTS} have been made. */
    public static boolean hostMayBeMade(BeastPhase phase, int hostsMade) {
        return (phase == BeastPhase.NONE || phase == BeastPhase.FAILED) && hostsMade < MAX_HOSTS;
    }

    /**
     * Whether the Host changes now: forced (the frequency rite, a command), or on a full moon night that is a later day than the one the Host was made (or the
     * Spirit got away) on, for a Host that waits or a Spirit that is to return.
     */
    public static boolean changeDue(BeastPhase phase, boolean forced, boolean fullMoonNight, long today, long sinceDay) {
        if (phase != BeastPhase.HOST && phase != BeastPhase.RETURN_PENDING) {
            return false;
        }
        return forced || fullMoonNight && today > sinceDay;
    }

    /** The health a Beast of this phase's history starts with: half after a return, full otherwise. */
    public static float startHealth(boolean returning) {
        return returning ? RETURN_HEALTH : 1.0F;
    }

    /** The Host is untouchable while the encounter needs it: waiting, collapsed, or waiting for the Spirit to return. */
    public static boolean hostProtected(BeastPhase phase) {
        return phase == BeastPhase.HOST || phase == BeastPhase.SPIRIT || phase == BeastPhase.RETURN_PENDING;
    }
}
