package net.ofatech.supernaturalbloodlines.parts.hud.net;

import java.util.List;

/** The last catalogue the server sent this client (a plain holder: the screen reads it, the payload handler writes it). */
public final class ClientCatalogue {
    private static volatile List<WheelEntry> entries = List.of();
    private static volatile int revision = -1;

    private ClientCatalogue() {
    }

    /** Development only: the screenshot driver's demo list stays, whatever the server sends. */
    public static volatile boolean frozen;

    public static void accept(WheelCataloguePayload payload) {
        if (frozen) {
            return;
        }
        set(payload);
    }

    public static void set(WheelCataloguePayload payload) {
        entries = payload.entries();
        revision = payload.revision();
    }

    public static List<WheelEntry> entries() {
        return entries;
    }

    public static int revision() {
        return revision;
    }

    /** A new world or a lost connection: nothing from the old server is kept. */
    public static void clear() {
        entries = List.of();
        revision = -1;
    }
}
