package net.ofatech.supernaturalbloodlines.core.bloodline;

/**
 * R206: the stages of werewolf venom. Pure. The clock is the effect's own remaining time, so nothing is stored: a venom that was already
 * running when this came in simply continues in its new stage.
 */
public final class VenomStages {
    public enum Stage { MILD, SICK, DEATH }

    private VenomStages() {
    }

    /** {@code remaining <= 1}: death (the last pulse); {@code total - remaining >= sickAfterTicks}: sick; before that, mild. */
    public static Stage of(int remainingTicks, int totalTicks, int sickAfterTicks) {
        if (remainingTicks <= 1) {
            return Stage.DEATH;
        }
        return totalTicks - remainingTicks >= sickAfterTicks ? Stage.SICK : Stage.MILD;
    }

    /** The pulse {@code remaining} is sick and the one before it ({@code pulseTicks} earlier) was not: the line "you feel sick" is said once. */
    public static boolean justFellSick(int remainingTicks, int totalTicks, int sickAfterTicks, int pulseTicks) {
        return of(remainingTicks, totalTicks, sickAfterTicks) == Stage.SICK
                && of(remainingTicks + pulseTicks, totalTicks, sickAfterTicks) == Stage.MILD;
    }
}
