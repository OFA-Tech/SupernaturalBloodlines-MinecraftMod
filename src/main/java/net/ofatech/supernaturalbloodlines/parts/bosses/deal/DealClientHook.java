package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import java.util.function.Consumer;

/** Where the client's dialog plugs in without the common code naming it: the bosses part sets it on a client only, so a server never loads a screen. */
public final class DealClientHook {
    private static volatile Consumer<DealOfferPayload> opener = offer -> { };

    private DealClientHook() {
    }

    public static void install(Consumer<DealOfferPayload> screen) {
        opener = screen;
    }

    public static void open(DealOfferPayload offer) {
        opener.accept(offer);
    }
}