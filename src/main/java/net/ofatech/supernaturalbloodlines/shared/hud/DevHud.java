package net.ofatech.supernaturalbloodlines.shared.hud;

/**
 * Development only: the switches of the HUD screenshots ({@code -Dsupernaturalbloodlines.devshots=hud:...}). The hub's screenshot driver sets them and the HUD
 * draws demo slots while {@link #on} is true. They stay false in a normal run, so nothing here is reachable.
 */
public final class DevHud {
    public static volatile boolean on;
    public static volatile boolean alt;
    public static volatile boolean idle;

    private DevHud() {
    }
}
