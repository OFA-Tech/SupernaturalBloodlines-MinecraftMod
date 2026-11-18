package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one model of what is waiting for a player to answer: a sire's call, a pack or alliance invitation, a leadership challenge, a compulsion's
 * choice. The species part (which owns the indexes) installs the source; the hub's Home and its corner toast read it through one payload.
 * Only what the chat line already told the player: who asked, the token that answers, the ticks left.
 */
public final class HubRequests {
    /** DUEL is not a request: it is the player's own duel (token {@code asked}, {@code on} or {@code paused}), sent on the same channel so the Profile can show it. */
    public enum Kind { SIRE_CALL, PACK_INVITE, ALLY_INVITE, CHALLENGE, COMPULSION, DUEL }

    /** The id used when nobody asked (a compulsion is the player's own). */
    public static final UUID NOBODY = new UUID(0, 0);

    /**
     * @param token   what the answer command takes; empty for a challenge (it is answered by {@code pack challenge accept} or {@code decline})
     * @param fromName empty for a compulsion
     */
    public record Request(Kind kind, UUID from, String fromName, String token, int ticksLeft) {
    }

    private static volatile Function<ServerPlayer, List<Request>> source = p -> List.of();
    private static volatile Consumer<ServerPlayer> changed = p -> { };

    private HubRequests() {}

    public static void set(Function<ServerPlayer, List<Request>> function) {
        source = function;
    }

    /** What is open for {@code p} now, newest first within a kind. */
    public static List<Request> of(ServerPlayer p) {
        return List.copyOf(source.apply(p));
    }

    /** The hub (supernatural part) installs what to do when the requests of a player changed: push them to the client. */
    public static void onChanged(Consumer<ServerPlayer> hook) {
        changed = hook;
    }

    /** The species part calls it after it opens or answers a request of {@code p}. */
    public static void changed(ServerPlayer p) {
        changed.accept(p);
    }
}

