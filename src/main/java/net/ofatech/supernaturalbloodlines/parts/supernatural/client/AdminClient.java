package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminSnapshotPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandReplyPayload;

/**
 * Client only: what the server last told this player about the admin path. The Admin page reads it; nothing here draws.
 * {@code LadderNet} names the handlers only when {@code FMLEnvironment.dist} is CLIENT.
 */
public final class AdminClient {
    private static volatile AdminSnapshotPayload snapshot;
    private static volatile HubCommandReplyPayload reply;
    private static volatile boolean demo;

    private AdminClient() {}

    public static void snapshot(AdminSnapshotPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                snapshot = payload;
            }
        });
    }

    public static void reply(HubCommandReplyPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!demo) {
                reply = payload;
            }
        });
    }

    /** The newest snapshot, or null before the server answered. */
    public static AdminSnapshotPayload snapshot() {
        return snapshot;
    }

    /** The newest answer to a command the hub ran, or null. */
    public static HubCommandReplyPayload reply() {
        return reply;
    }

    public static List<Component> replyLines() {
        HubCommandReplyPayload r = reply;
        return r == null ? List.of() : r.lines();
    }

    /** Development only (the dev screenshots): a snapshot and a reply that no server sent. */
    public static void demo(AdminSnapshotPayload snap, HubCommandReplyPayload last) {
        demo = true;
        snapshot = snap;
        reply = last;
    }

    public static void clear() {
        snapshot = null;
        reply = null;
    }
}
