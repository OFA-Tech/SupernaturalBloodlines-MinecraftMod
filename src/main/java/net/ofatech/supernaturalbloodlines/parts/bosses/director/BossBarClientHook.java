package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.function.Consumer;

/** Where the client's bar plugs in without the common code naming it: the bosses part sets it on a client only, so a server never loads a client class. */
public final class BossBarClientHook {
    private static volatile Consumer<BossBarInfoPayload> sink = info -> { };

    private BossBarClientHook() {
    }

    public static void install(Consumer<BossBarInfoPayload> client) {
        sink = client;
    }

    public static void accept(BossBarInfoPayload info) {
        sink.accept(info);
    }
}
