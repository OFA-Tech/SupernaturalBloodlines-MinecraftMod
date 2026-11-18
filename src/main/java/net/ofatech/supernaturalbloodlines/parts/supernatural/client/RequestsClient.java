package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import java.util.List;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubRequestsPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Client only: the requests the server last pushed. Home and the toast read it; nothing here draws. */
public final class RequestsClient {
    private static volatile List<HubRequests.Request> requests = List.of();
    private static volatile long receivedAt;
    private static volatile boolean demo;

    private RequestsClient() {}

    public static void receive(HubRequestsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (demo) {
                return; // the dev screenshots keep their made-up requests
            }
            requests = payload.requests();
            receivedAt = System.nanoTime();
        });
    }

    private static final java.util.Set<String> DISMISSED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** What waits for an answer and has not been dismissed: Home, the badge and the toast all read this. */
    public static List<HubRequests.Request> requests() {
        return requests.stream().filter(r -> r.kind() != HubRequests.Kind.DUEL && !DISMISSED.contains(key(r))).toList();
    }

    /** The player's own duel, if any (it rides with the requests but is not one): the Profile of the opponent shows it. */
    public static HubRequests.Request duel() {
        return requests.stream().filter(r -> r.kind() == HubRequests.Kind.DUEL).findFirst().orElse(null);
    }

    private static String key(HubRequests.Request r) {
        return r.kind() + ":" + r.from() + ":" + r.token();
    }

    /** Hides a card on this client only: an invitation has no decline command, so declining is waiting or dismissing. */
    public static void dismiss(HubRequests.Request r) {
        DISMISSED.add(key(r));
    }

    /** Development only (the dev screenshots): requests that were never sent. */
    public static void demo(List<HubRequests.Request> demo) {
        RequestsClient.demo = true;
        requests = List.copyOf(demo);
        receivedAt = System.nanoTime();
    }

    /** Ticks left of {@code r} now: what the server said, less what has passed since it said it (20 ticks a second). */
    public static int ticksLeft(HubRequests.Request r) {
        long passed = (System.nanoTime() - receivedAt) / 50_000_000L;
        return (int) Math.max(0, r.ticksLeft() - passed);
    }

    public static void clear() {
        DISMISSED.clear();
        requests = List.of();
    }
}

