package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.network.SupernaturalSyncPayload;

/**
 * The one door to a player's {@link SupernaturalState}: read it, change it, and listen for changes. A change is sent to
 * the owning player once per tick however often it is set.
 */
public final class SupernaturalAccess {
    public record SupernaturalChange(SupernaturalState before, SupernaturalState after) {
    }

    private static final List<BiConsumer<ServerPlayer, SupernaturalChange>> LISTENERS = new CopyOnWriteArrayList<>();
    // server thread only
    private static final Map<UUID, ServerPlayer> DIRTY = new LinkedHashMap<>();

    private static volatile boolean trackFakePlayers;

    private SupernaturalAccess() {
    }

    /** For the GameTest server only, whose players are all FakePlayers: lets them climb ladders. A real server never sets it. */
    public static void trackFakePlayers(boolean track) {
        trackFakePlayers = track;
    }

    public static boolean tracksFakePlayers() {
        return trackFakePlayers;
    }

    public static SupernaturalState get(Player p) {
        return p.getData(ModAttachments.SUPERNATURAL);
    }

    /**
     * Stores the state, queues the sync and tells the listeners (only when something changed).
     * shortcut: a listener that calls {@code set} sees its own change nested, with no ordering queue like
     * BloodlineAccess has; add one when a listener needs to change the state.
     */
    public static void set(ServerPlayer p, SupernaturalState s) {
        SupernaturalState before = get(p);
        if (before.equals(s)) {
            return;
        }
        p.setData(ModAttachments.SUPERNATURAL, s);
        DIRTY.put(p.getUUID(), p);
        SupernaturalChange change = new SupernaturalChange(before, s);
        for (BiConsumer<ServerPlayer, SupernaturalChange> l : LISTENERS) {
            try {
                l.accept(p, change);
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.error("Supernatural listener failed", e);
            }
        }
    }

    public static void addListener(BiConsumer<ServerPlayer, SupernaturalChange> l) {
        LISTENERS.add(l);
    }

    /** Sends the state at once (login, respawn, dimension change). */
    public static void syncToSelf(ServerPlayer p) {
        DIRTY.remove(p.getUUID());
        send(p);
    }

    /** Sends every queued change. Called once per server tick. */
    public static void flush() {
        if (DIRTY.isEmpty()) {
            return;
        }
        List<ServerPlayer> players = List.copyOf(DIRTY.values());
        DIRTY.clear();
        players.forEach(SupernaturalAccess::send);
    }

    private static void send(ServerPlayer p) {
        // mock/fake players and clients without the mod have no channel; sending would throw
        if (!(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(SupernaturalSyncPayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, new SupernaturalSyncPayload(get(p).clientView()));
        }
    }
}
