package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R190: the forcing "frequency" rite. With the Valet Pike in the circle (it is given back), a Hearing Device and an echo shard, on a full moon night, in the overworld, a
 * werewolf either brings the Host (once: when nothing has started, or the first Host died) or makes the waiting Host change now. It needs Werewolves and the Beast's
 * switch. Whatever it cannot do it says why, and gives every ingredient back. Where the reason would say something about the story (it is going on, it is over, the Host
 * has been called as often as he will be) the answer is one and the same, "the frequency is silent".
 */
public final class FrequencyRules {
    public enum Result { MAKE_HOST, FORCE_CHANGE, DISABLED, NEEDS_WEREWOLVES, NOT_WEREWOLF, WRONG_PLACE, NOT_NIGHT, NO_GROUND, BUSY, OVER, NO_MORE_HOSTS }

    private FrequencyRules() {
    }

    /** What the rite does: {@code callerWerewolf} is whether the one who works it is a werewolf type; {@code overworld} whether the circle stands in the overworld. */
    public static Result act(BeastPhase phase, int hostsMade, boolean werewolves, boolean enabled, boolean callerWerewolf, boolean overworld, boolean fullMoonNight) {
        if (!enabled) {
            return Result.DISABLED;
        }
        if (!werewolves) {
            return Result.NEEDS_WEREWOLVES;
        }
        if (!callerWerewolf) {
            return Result.NOT_WEREWOLF;
        }
        if (!overworld) {
            return Result.WRONG_PLACE;
        }
        if (!fullMoonNight) {
            return Result.NOT_NIGHT;
        }
        return switch (phase) {
            case NONE, FAILED -> BeastPhaseRules.hostMayBeMade(phase, hostsMade) ? Result.MAKE_HOST : Result.NO_MORE_HOSTS;
            case HOST, RETURN_PENDING -> Result.FORCE_CHANGE;
            case BEAST, SPIRIT -> Result.BUSY;
            case GONE, KILLED -> Result.OVER;
        };
    }

    /** BUSY, OVER and NO_MORE_HOSTS read the same: nothing about how far the story has gone. */
    public static boolean silent(Result result) {
        return result == Result.BUSY || result == Result.OVER || result == Result.NO_MORE_HOSTS;
    }

    public static String messageKey(Result result) {
        return "supernaturalbloodlines.rite.frequency." + (silent(result) ? "silent" : result.name().toLowerCase(java.util.Locale.ROOT));
    }
}
