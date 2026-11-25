package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.List;

/** R294: what breaks the Code, how strikes age and when they expel. A punishment only: nothing here rewards a kill. */
public final class CodeRules {
    public enum Kind { VILLAGER, PLAYER, OTHER }

    public record Victim(Kind kind, boolean prey, boolean fighting) {
    }

    private CodeRules() {
    }

    /** A villager, or a player who is not prey and did not fight the killer. Prey, a fight, a mob or an animal never. */
    public static boolean breaks(Victim v) {
        return v.kind() == Kind.VILLAGER || (v.kind() == Kind.PLAYER && !v.prey() && !v.fighting());
    }

    /** The strikes strictly inside the window; a stamp from the future (a clock that went back) is kept. */
    public static List<Long> strikesIn(List<Long> times, long now, long windowMillis) {
        return times.stream().filter(t -> now - t < windowMillis).toList();
    }

    public static boolean expels(int strikes, int limit) {
        return strikes >= limit;
    }

    public static long breakerUntil(long now, int hours) {
        return now + hours * 3_600_000L;
    }

    public static boolean isBreaker(long until, long now) {
        return now < until;
    }
}