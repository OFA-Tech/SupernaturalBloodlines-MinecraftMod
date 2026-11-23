package net.ofatech.supernaturalbloodlines.core.rite;

/**
 * Who the performer of a rite is when Enchanted hands the rite only the id of whoever started it. The performer is that player when they are in the
 * level. When they are not (they logged out or left the area while the rite was working) the one on the circle stands in for them ONLY when that one is
 * the performer: a rite that changes the target asks the target's consent unless performer and target are the same person, and a stand-in for someone
 * else would skip the question and change an unwilling player. Pure.
 */
public final class PerformerRule {
    public enum Pick { FOUND, TARGET, NONE }

    private PerformerRule() {
    }

    public static Pick pick(boolean foundIsPlayer, boolean targetIsCaster) {
        if (foundIsPlayer) {
            return Pick.FOUND;
        }
        return targetIsCaster ? Pick.TARGET : Pick.NONE;
    }
}
