package net.ofatech.supernaturalbloodlines.core.boss;

/** A chance per check, times a factor (Enhanced Celestials super moons raise it in plan 17i; 1 until then), kept in 0 to 1. */
public final class BossWeight {
    private BossWeight() {
    }

    public static double chance(double base, double factor) {
        return Math.max(0.0, Math.min(1.0, base * factor));
    }
}