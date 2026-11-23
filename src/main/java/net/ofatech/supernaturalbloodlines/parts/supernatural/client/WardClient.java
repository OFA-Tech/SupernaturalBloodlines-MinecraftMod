package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.WardViewPayload;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;

/** Client only: the wards the server last sent. The Path page reads it; nothing here draws. */
public final class WardClient {
    private static volatile WardHub.View view = WardHub.View.EMPTY;
    private static volatile boolean demo;

    private WardClient() {}

    public static void receive(WardViewPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) { // the dev screenshots keep their made-up wards
                view = payload.view();
            }
        });
    }

    public static WardHub.View view() {
        return view;
    }

    /** Development only (the dev screenshots): wards that were never sent. */
    public static void demo(WardHub.View made) {
        demo = true;
        view = made;
    }

    public static void clear() {
        demo = false;
        view = WardHub.View.EMPTY;
    }
}
