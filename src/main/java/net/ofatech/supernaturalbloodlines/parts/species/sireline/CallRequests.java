package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.HashMap;
import java.util.List;
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

    /** The token of the newest request still open for {@code sired}: what a button with no token answers. */
    public Optional<String> latestFor(UUID sired, long now) {
        return open.entrySet().stream()
                .filter(e -> e.getValue().sired().equals(sired) && e.getValue().expires() >= now)
                .max(java.util.Comparator.comparingLong(e -> e.getValue().expires())).map(Map.Entry::getKey);
    }

    /** Every request still open for {@code sired}, newest first, with its token. */
    public java.util.List<Map.Entry<String, Request>> openFor(UUID sired, long now) {
        return open.entrySet().stream()
                .filter(e -> e.getValue().sired().equals(sired) && e.getValue().expires() >= now)
                .sorted(java.util.Comparator.comparingLong((Map.Entry<String, Request> e) -> e.getValue().expires()).reversed())
                .map(e -> Map.entry(e.getKey(), e.getValue())).toList();
    }

    /** The open request under {@code token} for {@code sired}, left in place; an expired one is dropped and not returned. */
    public Optional<Request> peek(String token, UUID sired, long now) {
        Request r = open.get(token);
        if (r == null || !r.sired().equals(sired)) {
            return Optional.empty();
        }
        if (r.expires() < now) {
            open.remove(token);
            return Optional.empty();
        }
        return Optional.of(r);
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

    /** A player logged out: only the requests that were SENT TO them are void. What they asked of others stays open until it runs out (the other may still answer while they are away). */
    public void forgetFor(UUID sired) {
        open.values().removeIf(r -> r.sired().equals(sired));
    }

    /** Every request this player opened as the caller (a circle that closed takes its calls with it). */
    public List<UUID> forgetCalledBy(UUID caller) {
        List<UUID> asked = open.values().stream().filter(r -> r.caller().equals(caller)).map(Request::sired).toList();
        open.values().removeIf(r -> r.caller().equals(caller));
        return asked;
    }

    public void clear() {
        open.clear();
    }

    int size() {
        return open.size();
    }
}
