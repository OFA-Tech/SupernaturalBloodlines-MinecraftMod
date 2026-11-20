package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R161: when a boss that shifts goes from its human body to the beast, how long it takes, how big each body is, and what the beast gains. He shifts
 * once and stays shifted: at half health or less, or when the one he fights is a werewolf.
 */
public final class ShiftRules {
    /** Ticks from the start of the shift to the beast (2 seconds): the human body is first, the beast is shown from half way. */
    public static final int SHIFT_TICKS = 40;
    /** Ticks the howl lasts (the length of the howl animation, 3.2 s): counted from the start of the shift. */
    public static final int HOWL_TICKS = 64;
    public static final float SHIFT_AT_HEALTH = 0.5F;
    /** The beast bites harder and runs faster than the man (a share of the base value added). */
    public static final double BEAST_DAMAGE_BONUS = 0.5;
    public static final double BEAST_SPEED_BONUS = 0.25;

    public record Size(float width, float height) {
    }

    private ShiftRules() {
    }

    /** The hitbox follows the body: a man, then a wolf 0.9 wide and 1.5 at the shoulder (the model reaches 2.1 to the ear tips). */
    public static Size dimensionsFor(BossForm form) {
        return form == BossForm.BEAST ? new Size(0.9F, 1.5F) : new Size(0.6F, 1.95F);
    }

    /** Whether he starts to shift now: only the human form shifts, at half health or less (but alive), or against a werewolf. */
    public static boolean shouldShift(BossForm form, float healthFraction, boolean targetIsWerewolf) {
        return form == BossForm.HUMAN && healthFraction > 0F && (healthFraction <= SHIFT_AT_HEALTH || targetIsWerewolf);
    }

    /** How far along the shift is, 0 to 1. */
    public static float progress(int shiftTicks) {
        return Math.max(0F, Math.min(1F, shiftTicks / (float) SHIFT_TICKS));
    }

    /** Whether the wolf model is what is drawn: the beast, or a shift that is half done. */
    public static boolean showsBeast(BossForm form, int shiftTicks) {
        return form == BossForm.BEAST || form == BossForm.SHIFTING && progress(shiftTicks) >= 0.5F;
    }

    /** The form after one more tick of shifting. */
    public static BossForm afterTick(BossForm form, int shiftTicks) {
        return form == BossForm.SHIFTING && shiftTicks >= SHIFT_TICKS ? BossForm.BEAST : form;
    }

    /** Whether the howl is playing: from the start of the shift, for {@link #HOWL_TICKS}. */
    public static boolean howling(BossForm form, int shiftTicks) {
        return form != BossForm.HUMAN && shiftTicks < HOWL_TICKS;
    }
}
