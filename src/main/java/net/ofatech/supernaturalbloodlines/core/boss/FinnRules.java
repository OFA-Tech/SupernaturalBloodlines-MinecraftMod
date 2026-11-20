package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R175: Finn comes under a lunar eclipse, once an eclipse night (one roll a day, at {@code finn_chance_per_eclipse}), to a witch-type or vampire player in the
 * overworld within 128 blocks. He builds a ritual of particles and channels it for {@code finn_ritual_seconds}. The ritual never harms anyone: if it is
 * left alone he withdraws, the people near are told, and he is back at the next eclipse after {@link #COOLDOWN_DAYS} days. Hurt for more than a fifth of his
 * health, or pulled more than 24 blocks from the ritual, he stops it and fights like anyone else (R176).
 */
public final class FinnRules {
    public static final double RANGE = 128.0;
    public static final double LEASH = 24.0;
    public static final double INTERRUPT_DAMAGE_FRACTION = 0.20;
    public static final int COOLDOWN_DAYS = 7;

    private FinnRules() {
    }

    public record Facts(boolean eclipse, boolean overworld, boolean witchOrVampire, double distance, boolean spectator) {
    }

    public static boolean eligible(Facts f) {
        return f.eclipse() && f.overworld() && f.witchOrVampire() && f.distance() <= RANGE && !f.spectator();
    }

    /** One roll a day: once it was made on (or promised past) {@code lastRollDay}, not again that day. */
    public static boolean mayRoll(long lastRollDay, long today) {
        return today > lastRollDay;
    }

    /** The {@code lastRollDay} to write after a ritual that was left alone, so the next roll is {@link #COOLDOWN_DAYS} days away. */
    public static long afterSuccess(long today) {
        return today + COOLDOWN_DAYS - 1;
    }

    public enum Step { CHANNELING, DONE, INTERRUPTED, ENDED }

    public static int totalTicks(int seconds) {
        return Math.max(1, seconds) * 20;
    }

    /**
     * Where the ritual stands. Hurt or pulled away comes first (it fails even on the last tick), then done, then the eclipse ending (it is over before
     * he finished: he leaves with nothing done).
     */
    public static Step step(int ticks, int totalTicks, float damageTaken, float maxHealth, double distanceFromCentre, boolean eclipse) {
        if (damageTaken > maxHealth * INTERRUPT_DAMAGE_FRACTION || distanceFromCentre > LEASH) {
            return Step.INTERRUPTED;
        }
        if (ticks >= totalTicks) {
            return Step.DONE;
        }
        return eclipse ? Step.CHANNELING : Step.ENDED;
    }
}
