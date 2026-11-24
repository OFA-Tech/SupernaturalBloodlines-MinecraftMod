package net.ofatech.supernaturalbloodlines.core.curse;

/** The Curse of Susceptibility as pure rules (R322, R323). */
public final class SusceptibilityRules {
    private SusceptibilityRules() {
    }

    /** How much more damage the cursed player takes: {@code extraPercent} per strength level plus one, at most {@code maxPercent} of normal, never below normal. */
    public static double multiplier(int strength, int extraPercent, int maxPercent) {
        double raised = 1.0 + extraPercent * (strength + 1) / 100.0;
        return Math.max(1.0, Math.min(raised, maxPercent / 100.0));
    }

    /** A real blow is raised; one that bypasses invulnerability (/kill, the void), a probe that deals nothing, and a blow of nothing are left alone. */
    public static float apply(float amount, double multiplier, boolean bypassesInvulnerability, boolean probe) {
        if (amount <= 0 || bypassesInvulnerability || probe) {
            return amount;
        }
        return (float) (amount * multiplier);
    }
}
