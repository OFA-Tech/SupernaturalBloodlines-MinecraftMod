package net.ofatech.supernaturalbloodlines.core.wheel;

/** The one rule for a player who cannot use any power: put out of action by a part (downed, snapped, dying), a spectator, dead or asleep. */
public final class ActRules {
    private ActRules() {
    }

    public static boolean cannotAct(boolean blockedByPart, boolean spectator, boolean alive, boolean sleeping) {
        return blockedByPart || spectator || !alive || sleeping;
    }
}
