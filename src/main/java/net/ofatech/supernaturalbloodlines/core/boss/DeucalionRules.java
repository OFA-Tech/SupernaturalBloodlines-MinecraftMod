package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R178: Deucalion comes on a full moon night for a player Alpha who stacks at least {@code deucalion_min_sparks} sparks, once per world, and brings his
 * Alpha pack. He walks as a man, drops to fours when he first falls below three quarters of his health or when his target is within 8 blocks at night,
 * and does not change back. He dies for good unless {@code deucalion_respawn} is on and enough days have passed.
 */
public final class DeucalionRules {
    public static final float SHIFT_AT_HEALTH = 0.75F;
    public static final double SHIFT_RANGE = 8.0;

    /** @param sparks the stacked sparks of the player's wolf state, 0 for no Alpha */
    public record Target(boolean werewolf, boolean alpha, int sparks, boolean overworld, boolean spectator) {
    }

    private DeucalionRules() {
    }

    public static boolean eligible(Target t, int minSparks) {
        return t.werewolf() && t.alpha() && t.overworld() && !t.spectator() && t.sparks() >= Math.max(1, minSparks);
    }

    /** Only on a full moon night, and only when somebody stacks enough: there is no dice, he is the consequence of the stack. */
    public static boolean picks(boolean fullMoonNight, boolean anyTarget) {
        return fullMoonNight && anyTarget;
    }

    /** Whether he starts to shift now: only the human form, alive, below three quarters of his health or with his target close on a night. */
    public static boolean shouldShift(BossForm form, float healthFraction, boolean targetClose, boolean night) {
        return form == BossForm.HUMAN && healthFraction > 0F && (healthFraction < SHIFT_AT_HEALTH || targetClose && night);
    }

    public static boolean targetClose(double distanceSquared) {
        return distanceSquared <= SHIFT_RANGE * SHIFT_RANGE;
    }

    /** The demon wolf is bigger than Klaus's: 1.2 wide and 2.6 tall, from the swap tick. */
    public static ShiftRules.Size dimensionsFor(BossForm form, int shiftTicks) {
        return ShiftRules.showsBeast(form, shiftTicks) ? new ShiftRules.Size(1.2F, 2.6F) : new ShiftRules.Size(0.6F, 1.95F);
    }

    /** The respawn, when the switch is on: at least {@code days} in-game days after the day he died. {@code diedDay} below 0 means nothing is written down. */
    public static boolean mayRespawn(boolean on, long today, long diedDay, int days) {
        return on && diedDay >= 0 && today - diedDay >= Math.max(1, days);
    }
}
