package net.ofatech.supernaturalbloodlines.core.hud;

import org.jetbrains.annotations.Nullable;

/**
 * How far a Tensura skill has got, as the slot's thin bar shows it. ManasCore keeps one number per skill instance ({@code getMastery}, saved in the instance's
 * NBT and so on the client) against {@code getMaxMastery} (100 unless a skill says otherwise); Tensura's own HUD reads the same two numbers, and a number below
 * zero is a resist skill still being learned (it counts up from minus its learning requirement to zero, then mastery starts). Pure.
 *
 * @param fraction 0 to 1 of the bar
 */
public record Mastery(float fraction, State state) {
    public enum State { LEARNING, PROGRESS, MASTERED }

    /** Tensura's own HUD uses 100 when a skill does not say how much learning it needs. */
    public static final int DEFAULT_LEARNING = 100;

    /**
     * @param mastery        {@code ManasSkillInstance.getMastery()}
     * @param max            {@code getMaxMastery()}; a skill with none has no bar
     * @param learningNeeded {@code ResistSkill.getLearningPointRequirement()}, or 0 for a skill that is not a resist skill
     * @param mastered       {@code ManasSkillInstance.isMastered(entity)}
     * @return null when there is nothing to show
     */
    @Nullable
    public static Mastery of(double mastery, int max, int learningNeeded, boolean mastered) {
        if (max <= 0 || Double.isNaN(mastery)) {
            return null;
        }
        if (mastery < 0) {
            int need = learningNeeded > 0 ? learningNeeded : DEFAULT_LEARNING;
            return new Mastery(clamp((float) ((need - Math.abs(mastery)) / need)), State.LEARNING);
        }
        if (mastered || mastery >= max) {
            return new Mastery(1f, State.MASTERED);
        }
        return new Mastery(clamp((float) (mastery / max)), State.PROGRESS);
    }

    public boolean mastered() {
        return state == State.MASTERED;
    }

    private static float clamp(float f) {
        return Math.max(0f, Math.min(1f, f));
    }
}
