package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import java.util.List;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubRequestsPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Client only: the requests the server last pushed. Home and the toast read it; nothing here draws. */
public final class RequestsClient {
    private static volatile List<HubRequests.Request> requests = List.of();

    private RequestsClient() {}

    public static void receive(HubRequestsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> requests = payload.requests());
    }

    public static List<HubRequests.Request> requests() {
        return requests;
    }

    public static void clear() {
        requests = List.of();
    }
}

