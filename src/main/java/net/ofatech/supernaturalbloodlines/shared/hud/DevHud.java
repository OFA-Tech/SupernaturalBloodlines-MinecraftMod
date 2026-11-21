package net.ofatech.supernaturalbloodlines.shared.hud;

/**
 * Development only: the switches of the HUD screenshots ({@code -Dsupernaturalbloodlines.devshots=hud:...}). The hub's screenshot driver sets them and the HUD
 * draws demo slots while {@link #on} is true. They stay false in a normal run, so nothing here is reachable.
 */
public final class DevHud {
    public static volatile boolean on;
    public static volatile boolean alt;
    public static volatile boolean idle;
    public static volatile boolean switched;
    /** A plain human in the demo: Tensura mode only, so no mode panel. */
    public static volatile boolean human;
    /** The demo shows a Tensura skill cooldown counting down. */
    public static volatile boolean cooldown;

    private DevHud() {
    }
}
