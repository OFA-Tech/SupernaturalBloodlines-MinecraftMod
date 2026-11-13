package net.ofatech.supernaturalbloodlines.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.network.BloodlineSyncPayload;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/** The one door to a player's bloodline state: read it, change it (synced), and listen for species, lineage and rank changes. */
public final class BloodlineAccess {
    public record BloodlineChange(BloodlineState before, BloodlineState after) {
    }

    static final int MAX_DRAINED = 16;

    private static final String GUARD_KEY = "core:bloodline_listeners";

    private static final List<BiConsumer<ServerPlayer, BloodlineChange>> LISTENERS = new CopyOnWriteArrayList<>();
    // changes made by listeners while a player's fan-out is running; only exists during the outer call
    private static final ThreadLocal<Map<UUID, Deque<BloodlineChange>>> PENDING = ThreadLocal.withInitial(HashMap::new);

    private BloodlineAccess() {
    }

    public static BloodlineState get(Player p) {
        return p.getData(ModAttachments.BLOODLINE);
    }

    public static void set(ServerPlayer p, BloodlineState s) {
        BloodlineState before = get(p);
        p.setData(ModAttachments.BLOODLINE, s);
        // fake/mock players have no connection channel; sending to them would throw
        if (canReceive(p)) {
            PacketDistributor.sendToPlayer(p, new BloodlineSyncPayload(p.getId(), clientView(s, true)));
        }
        // one send per tracker, so a fake tracker cannot stop the real ones
        BloodlineSyncPayload others = new BloodlineSyncPayload(p.getId(), clientView(s, false));
        for (ServerPlayer tracker : ((ServerLevel) p.level()).getChunkSource().chunkMap.getPlayersWatching(p)) {
            if (tracker != p && canReceive(tracker)) {
                PacketDistributor.sendToPlayer(tracker, others);
            }
        }
        notifyListeners(LISTENERS, p, p.getUUID(), before, s);
    }

    /** Applies the rules and stores the result. Faction actions in the Outcome are left to the parts. */
    public static Outcome applyEvent(ServerPlayer p, BloodlineEvent e, BloodlineContext c) {
        Outcome out = BloodlineRules.apply(get(p), e, c);
        if (!out.state().equals(get(p))) {
            set(p, out.state());
        }
        return out;
    }

    /**
     * What a client may know: the hidden gene stays on the server, and only the subject sees their own sire.
     * Build every payload through this, so a new send path cannot leak.
     */
    public static BloodlineState clientView(BloodlineState s, boolean viewerIsSubject) {
        BloodlineState.Builder b = s.toBuilder().pendingSire(null)
                .wolf(s.wolf().toBuilder().latentGene(false).geneRolled(false).build());
        if (!viewerIsSubject) {
            b.sire(null);
        }
        return b.build();
    }

    public static void addListener(BiConsumer<ServerPlayer, BloodlineChange> l) {
        LISTENERS.add(l);
    }

    public static void syncToSelf(ServerPlayer p) {
        sendTo(p, p);
    }

    /** Sends {@code subject}'s state to {@code to}. */
    public static void sendTo(ServerPlayer to, Player subject) {
        if (canReceive(to)) {
            PacketDistributor.sendToPlayer(to, new BloodlineSyncPayload(subject.getId(), clientView(get(subject), to == subject)));
        }
    }

    // Mock/fake players and clients without the mod have no channel for the payload; sending would throw.
    private static boolean canReceive(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(BloodlineSyncPayload.TYPE);
    }

    /**
     * Listeners hear about changes of species, wolf lineage, wolf rank or the hunter's mark kills only (they scale the
     * mark's perks, so a debug event or command must reach them too), not about thirst ticks, granted
     * skills and the like. A change made by a
     * listener is queued, not fanned out in place: the outer call finishes the current change for every
     * listener, then drains the queue in order, so each listener sees every change and the last one is final.
     * At most {@link #MAX_DRAINED} queued changes are drained per outer call (listeners that flip forever);
     * the state is already stored, so only the extra notifications are dropped.
     * A throwing listener is logged and does not stop the others.
     */
    static <P> void notifyListeners(List<BiConsumer<P, BloodlineChange>> listeners, P player, UUID playerId,
                                    BloodlineState before, BloodlineState after) {
        if (before.species() == after.species() && before.wolf().lineage() == after.wolf().lineage()
                && before.wolf().rank() == after.wolf().rank() && before.markKills() == after.markKills()) {
            return;
        }
        BloodlineChange change = new BloodlineChange(before, after);
        boolean outer = ReentrancyGuard.run(playerId, GUARD_KEY, () -> {
            Deque<BloodlineChange> queue = new ArrayDeque<>();
            PENDING.get().put(playerId, queue);
            try {
                deliver(listeners, player, change);
                int drained = 0;
                while (!queue.isEmpty()) {
                    if (drained++ >= MAX_DRAINED) {
                        SupernaturalBloodlines.LOGGER.error(
                                "Bloodline listeners keep changing the species of {}; dropped {} queued notifications",
                                playerId, queue.size());
                        break;
                    }
                    deliver(listeners, player, queue.poll());
                }
            } finally {
                PENDING.get().remove(playerId);
            }
        });
        if (!outer) {
            PENDING.get().get(playerId).add(change);
        }
    }

    private static <P> void deliver(List<BiConsumer<P, BloodlineChange>> listeners, P player, BloodlineChange change) {
        for (BiConsumer<P, BloodlineChange> l : listeners) {
            try {
                l.accept(player, change);
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.error("Bloodline listener failed", e);
            }
        }
    }
}
