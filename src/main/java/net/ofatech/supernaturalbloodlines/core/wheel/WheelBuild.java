package net.ofatech.supernaturalbloodlines.core.wheel;

/** Whether this build has a wheel screen. The wheel's keys of other mods are covered only when it does, so a player is never left with a hidden key and no way to use it. */
public final class WheelBuild {
    private WheelBuild() {
    }

    /** This build has the wheel screen (W4): the other mods' skill keys it replaces are covered while the HUD works. */
    public static boolean screenPresent() {
        return true;
    }
}
