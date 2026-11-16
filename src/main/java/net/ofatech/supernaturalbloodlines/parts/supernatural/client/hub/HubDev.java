package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.function.UnaryOperator;

/**
 * Development only: the switches of the hub screenshots ({@code DevShots}), in one place instead of one static per class. All off in a normal run, so none
 * of this is reachable; the hub reads them through these accessors.
 */
public final class HubDev {
    private static volatile boolean noTensura;
    private static volatile boolean quiet;
    private static volatile boolean handsFull;
    private static volatile UnaryOperator<HubView> view;

    private HubDev() {
    }

    /** Draw the hub as if Tensura were not installed (its look greyed, no EP). */
    static void noTensura(boolean on) {
        noTensura = on;
    }

    static boolean noTensura() {
        return noTensura;
    }

    /** The pointer counts as far away, so no hover and no tooltip is in a picture. */
    static void quiet(boolean on) {
        quiet = on;
    }

    static boolean quiet() {
        return quiet;
    }

    /** Every rite ingredient counts as in hand (the demo data, instead of giving items to the player). */
    static void handsFull(boolean on) {
        handsFull = on;
    }

    public static boolean handsFull() {
        return handsFull;
    }

    /** Replaces the hub's view of the player with demo data (a client view only; nothing is sent or changed on the server). */
    static void view(UnaryOperator<HubView> override) {
        view = override;
    }

    static UnaryOperator<HubView> view() {
        return view;
    }
}
