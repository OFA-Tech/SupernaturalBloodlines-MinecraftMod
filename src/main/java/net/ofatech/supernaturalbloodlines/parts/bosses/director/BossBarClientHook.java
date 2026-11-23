package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import java.util.function.Consumer;

/** Where the client's bar plugs in without the common code naming it: the bosses part sets it on a client only, so a server never loads a client class. */
public final class BossBarClientHook {
    private static volatile Consumer<BossBarInfoPayload> sink = info -> { };
    private static volatile Consumer<BossHitPayload> hits = hit -> { };

    public static void installHits(Consumer<BossHitPayload> client) {
        hits = client;
    }

    public static void hit(BossHitPayload payload) {
        hits.accept(payload);
    }

    private BossBarClientHook() {
    }

    public static void install(Consumer<BossBarInfoPayload> client) {
        sink = client;
    }

    public static void accept(BossBarInfoPayload info) {
        sink.accept(info);
    }
}
