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
    /** Alt has been held for a second: the panels show beside the labels. */
    public static volatile boolean detail;
    /** One slot of the demo has just become ready: its glow is drawn at a fixed strength. */
    public static volatile boolean glow;
    /** The request toast of the demo is on screen (over the Tensura status HUD, when it is installed). */
    public static volatile boolean toast;

    /** Installed by the HUD part's client: shows the demo wheel in a state (open, alt, page, picker, aim, closed). Null without it. */
    public static volatile java.util.function.Consumer<String> wheelShow;

    private DevHud() {
    }
}
