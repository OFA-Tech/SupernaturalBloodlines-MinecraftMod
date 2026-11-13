package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Open {@code /bloodline call} requests, one per (caller, sired), kept in memory. The token is what the [Accept] click
 * sends back; only the sired player it was made for can use it, once, within a minute.
 */
public final class CallRequests {
    public static final long TTL_TICKS = 60 * 20;

    public record Request(UUID caller, UUID sired, long expires) {
    }

    private final Map<String, Request> open = new HashMap<>();

    /** A new request replaces any open one for the same pair. */
    public String open(UUID caller, UUID sired, long now) {
        open.values().removeIf(r -> r.expires() < now || (r.caller().equals(caller) && r.sired().equals(sired)));
        String token = UUID.randomUUID().toString().substring(0, 8);
        open.put(token, new Request(caller, sired, now + TTL_TICKS));
        return token;
    }

    public Optional<String> tokenOf(UUID caller, UUID sired) {
        return open.entrySet().stream()
                .filter(e -> e.getValue().caller().equals(caller) && e.getValue().sired().equals(sired))
                .map(Map.Entry::getKey).findFirst();
    }

    public Optional<Request> take(String token, UUID sired, long now) {
        Request r = open.get(token);
        if (r == null || !r.sired().equals(sired)) {
            return Optional.empty();
        }
        open.remove(token);
        return r.expires() < now ? Optional.empty() : Optional.of(r);
    }

    /** A player logged out: their requests, as caller or as sired, are void. */
    public void forget(UUID player) {
        open.values().removeIf(r -> r.caller().equals(player) || r.sired().equals(player));
    }

    int size() {
        return open.size();
    }
}
