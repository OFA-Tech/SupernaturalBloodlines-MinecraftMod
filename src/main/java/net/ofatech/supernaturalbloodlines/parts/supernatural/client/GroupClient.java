package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.GroupViewPayload;

/** Client only: the group view the server last pushed. The Group page and the Profile read it; nothing here draws. */
public final class GroupClient {
    private static volatile GroupView view = GroupView.NONE;
    private static volatile boolean demo;

    private GroupClient() {}

    public static void receive(GroupViewPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                view = payload.view();
            }
        });
    }

    public static GroupView view() {
        return view;
    }

    public static void clear() {
        view = GroupView.NONE;
        demo = false;
    }

    /** The development screenshots keep their made-up group. */
    public static void demo(GroupView shown) {
        view = shown;
        demo = true;
    }
}
