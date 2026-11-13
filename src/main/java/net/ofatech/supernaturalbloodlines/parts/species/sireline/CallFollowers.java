package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Sired creatures answering a call: for 30 seconds they are told once a second to walk to the caller. A plain navigation
 * order, so a creature that cannot path simply stays put. Only creatures that are already loaded are ever asked.
 */
public final class CallFollowers {
    static final long FOLLOW_TICKS = 30 * 20;
    private static final double SPEED = 1.2;

    private record Follow(UUID caller, ResourceKey<Level> dimension, long until) {
    }

    // server thread only
    private static final Map<UUID, Follow> FOLLOWING = new HashMap<>();

    private CallFollowers() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (!FOLLOWING.isEmpty() && e.getServer().getTickCount() % 20 == 0) {
                tick(e.getServer(), e.getServer().overworld().getGameTime());
            }
        });
    }

    static void follow(Mob mob, UUID caller, long now) {
        FOLLOWING.put(mob.getUUID(), new Follow(caller, mob.level().dimension(), now + FOLLOW_TICKS));
    }

    public static boolean isFollowing(UUID mob) {
        return FOLLOWING.containsKey(mob);
    }

    /** Stops everything that was called by this player. */
    public static void forget(UUID caller) {
        FOLLOWING.values().removeIf(f -> f.caller().equals(caller));
    }

    public static void tick(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, Follow>> it = FOLLOWING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Follow> entry = it.next();
            Follow f = entry.getValue();
            ServerLevel level = server.getLevel(f.dimension());
            // getEntity only looks at what is loaded: it never loads a chunk
            Mob mob = level != null && level.getEntity(entry.getKey()) instanceof Mob m && m.isAlive() ? m : null;
            ServerPlayer caller = SireCall.player(server, f.caller());
            if (mob == null || now >= f.until() || caller == null || caller.level() != level) {
                if (mob != null) {
                    mob.getNavigation().stop();
                }
                it.remove();
            } else {
                mob.getNavigation().moveTo(caller, SPEED);
            }
        }
    }
}
