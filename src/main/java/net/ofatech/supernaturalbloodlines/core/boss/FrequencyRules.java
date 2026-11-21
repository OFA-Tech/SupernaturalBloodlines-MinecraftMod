package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R190: the forcing "frequency" rite. With the Valet Pike in the circle (it is given back) and a Hearing Device, on a full moon night, it either brings the Host (once:
 * when nothing has started, or the first Host died) or makes the waiting Host change now. It needs Werewolves and the Beast's switch. Anything else is refused with a
 * reason that tells the caster nothing about who else is in the world.
 */
public final class FrequencyRules {
    public enum Result { MAKE_HOST, FORCE_CHANGE, DISABLED, NEEDS_WEREWOLVES, NOT_NIGHT, BUSY, OVER, NO_MORE_HOSTS }

    private FrequencyRules() {
    }

    public static Result act(BeastPhase phase, int hostsMade, boolean werewolves, boolean enabled, boolean fullMoonNight) {
        if (!enabled) {
            return Result.DISABLED;
        }
        if (!werewolves) {
            return Result.NEEDS_WEREWOLVES;
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

    public static String messageKey(Result result) {
        return "supernaturalbloodlines.rite.frequency." + result.name().toLowerCase(java.util.Locale.ROOT);
    }
}
