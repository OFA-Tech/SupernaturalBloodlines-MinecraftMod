package net.ofatech.supernaturalbloodlines.core.wheel;

/** Whether this build has a wheel screen. The wheel's keys of other mods are covered only when it does, so a player is never left with a hidden key and no way to use it. */
public final class WheelBuild {
    private WheelBuild() {
    }

    /** Flipped by the screen's own phase (W4, T10). */
    public static boolean screenPresent() {
        return false;
    }
}
