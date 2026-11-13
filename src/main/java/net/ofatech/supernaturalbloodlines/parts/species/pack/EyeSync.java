package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload;
import net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color;

/**
 * Tells clients each werewolf's eye colour: to the player and everyone tracking them when it changes, to the player at
 * login and to a watcher who starts tracking a werewolf. A player with no lineage has nothing to show.
 */
public final class EyeSync {
    private EyeSync() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> {
            if (Color.of(change.before().wolf()) != Color.of(change.after().wolf())) {
                broadcast(p);
            }
        });
        // a new client player object (login, respawn, dimension change) knows nothing yet
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> sendToSelf(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> sendToSelf(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> sendToSelf(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer watcher && hasLineage(target)) {
                EyeColorPayload.send(watcher, payload(target));
            }
        });
    }

    private static void sendToSelf(Player e) {
        if (e instanceof ServerPlayer p && hasLineage(p)) {
            broadcast(p);
        }
    }

    private static boolean hasLineage(Player p) {
        return BloodlineAccess.get(p).wolf().lineage() != Lineage.NONE;
    }

    private static EyeColorPayload payload(Player p) {
        return new EyeColorPayload(p.getId(), Color.of(BloodlineAccess.get(p).wolf()));
    }

    private static void broadcast(ServerPlayer p) {
        EyeColorPayload payload = payload(p);
        EyeColorPayload.send(p, payload);
        for (ServerPlayer tracker : ((ServerLevel) p.level()).getChunkSource().chunkMap.getPlayersWatching(p)) {
            if (tracker != p) {
                EyeColorPayload.send(tracker, payload);
            }
        }
    }
}
