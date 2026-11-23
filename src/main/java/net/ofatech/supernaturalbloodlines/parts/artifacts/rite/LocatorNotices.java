package net.ofatech.supernaturalbloodlines.parts.artifacts.rite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * What the one looked for is told (R317): once when the search starts, and once when it ends, both with no name, no place and no kind. The end is kept in
 * memory (a restart forgets it: the target simply never hears that it ended). An attunement that is cast again for the same person replaces the old end.
 */
public final class LocatorNotices {
    private static final Map<UUID, Long> ENDS = new HashMap<>();

    private LocatorNotices() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (!ENDS.isEmpty() && e.getServer().overworld().getGameTime() % 40 == 0) {
                sweep(e.getServer());
            }
        });
    }

    static ServerPlayer find(MinecraftServer server, UUID id) {
        ServerPlayer listed = server.getPlayerList().getPlayer(id);
        if (listed != null) {
            return listed;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer p) {
                return p;
            }
        }
        return null;
    }

    /** The search began: tells the target now, if they are here, and the day it ends. */
    static void tell(MinecraftServer server, UUID target, long until) {
        ServerPlayer p = find(server, target);
        if (p != null) {
            p.sendSystemMessage(Component.translatable(LocatorRiteFactory.TOLD));
            ENDS.put(target, until);
        }
    }

    /** Tells those whose search has ended, once. */
    @VisibleForTesting
    public static void sweep(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, Long>> it = ENDS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (now >= e.getValue()) {
                it.remove();
                ServerPlayer p = find(server, e.getKey());
                if (p != null) {
                    p.sendSystemMessage(Component.translatable(LocatorRiteFactory.ENDED));
                }
            }
        }
    }

    @VisibleForTesting
    public static void reset() {
        ENDS.clear();
    }
}
