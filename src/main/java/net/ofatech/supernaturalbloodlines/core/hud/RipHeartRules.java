package net.ofatech.supernaturalbloodlines.core.hud;

/** R204: Rip Heart. A target at or under the health threshold dies; an Original never does, and the user cannot tell the two apart. Pure. */
public final class RipHeartRules {
    /** TOO_STRONG: above the threshold, refused with no cooldown. FINISH: the heart is torn out. NO_EFFECT: an Original at or under the threshold, which looks like FINISH to the user. */
    public enum Result { TOO_STRONG, FINISH, NO_EFFECT }

    private RipHeartRules() {
    }

    /**
     * The refusal of a healthy target never depends on what the target is (an Original at 50 health is TOO_STRONG like anyone), so the only question that a
     * low-health Original can answer is the one the user gets the same line and cooldown for.
     */
    public static Result decide(float targetHealth, float threshold, boolean targetIsOriginal) {
        if (targetHealth > threshold) {
            return Result.TOO_STRONG;
        }
        return targetIsOriginal ? Result.NO_EFFECT : Result.FINISH;
    }
}
