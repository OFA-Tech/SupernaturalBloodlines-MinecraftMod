package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ClanViewPayload;

/** Client only: the clan view the server last pushed. The Group page reads it; nothing here draws. */
public final class ClanClient {
    private static volatile ClanView view = ClanView.NONE;
    private static volatile boolean demo;

    private ClanClient() {}

    public static void receive(ClanViewPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                view = payload.view();
            }
        });
    }

    public static ClanView view() {
        return view;
    }

    public static void clear() {
        view = ClanView.NONE;
        demo = false;
    }

    /** The development screenshots keep their made-up clan. */
    public static void demo(ClanView shown) {
        view = shown;
        demo = true;
    }
}
