package net.ofatech.supernaturalbloodlines.shared.hub;

/**
 * What the client does when the server sends the Alpha Trial warning (R147): the species part receives the packet, the hub (supernatural part) shows
 * the Rites page and the True Alpha dialog. The hub installs it; without it nothing is shown, and the rite stays refused until the player answers.
 */
public final class TrialWarnHook {
    private static volatile Runnable show = () -> { };

    private TrialWarnHook() {
    }

    public static void set(Runnable r) {
        show = r == null ? () -> { } : r;
    }

    public static void fire() {
        show.run();
    }
}
