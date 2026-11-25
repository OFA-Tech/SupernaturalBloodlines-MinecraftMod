package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.CovenViewPayload;

/** Client only: the coven view the server last pushed. The Group page reads it; nothing here draws. */
public final class CovenClient {
    private static volatile CovenView view = CovenView.NONE;
    private static volatile boolean demo;

    private CovenClient() {}

    public static void receive(CovenViewPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                view = payload.view();
            }
        });
    }

    public static CovenView view() {
        return view;
    }

    public static void clear() {
        view = CovenView.NONE;
        demo = false;
    }

    /** The development screenshots keep their made-up circle. */
    public static void demo(CovenView shown) {
        view = shown;
        demo = true;
    }
}