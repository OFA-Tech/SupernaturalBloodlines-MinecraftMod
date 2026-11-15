package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClient;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;

/** Client only: the hub's payload handlers. {@code LadderNet} names them only when {@code FMLEnvironment.dist} is CLIENT. */
public final class HubClientHandlers {
    private HubClientHandlers() {
    }

    public static void snapshot(HubSnapshotPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HubClient.receive(payload));
    }
}
