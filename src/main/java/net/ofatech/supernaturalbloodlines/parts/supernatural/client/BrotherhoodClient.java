package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.BrotherhoodViewPayload;

/** Client only: the brotherhood view the server last pushed. The Group page reads it; nothing here draws. */
public final class BrotherhoodClient {
    private static volatile BrotherhoodView view = BrotherhoodView.NONE;
    private static volatile boolean demo;
    private static volatile boolean received;

    private BrotherhoodClient() {}

    public static void receive(BrotherhoodViewPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                view = payload.view();
                received = true;
            }
        });
    }

    public static BrotherhoodView view() {
        return view;
    }

    /** Whether the server has answered at all (a server without the channel never does): the page shows a loading line until then. */
    public static boolean loaded() {
        return received || demo;
    }

    public static void clear() {
        view = BrotherhoodView.NONE;
        demo = false;
        received = false;
    }

    /** The development screenshots keep their made-up brotherhood. */
    public static void demo(BrotherhoodView shown) {
        view = shown;
        demo = true;
    }
}