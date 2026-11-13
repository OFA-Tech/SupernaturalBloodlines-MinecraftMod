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
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Creatures answering a call (30 seconds) or a compulsion (5 minutes): they are told once a second to walk to the caller.
 * A plain navigation order, so a creature that cannot path simply stays put. Only creatures that are already loaded are ever asked.
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
        follow(mob, caller, now, FOLLOW_TICKS);
    }

    static void follow(Mob mob, UUID caller, long now, long ticks) {
        FOLLOWING.put(mob.getUUID(), new Follow(caller, mob.level().dimension(), now + ticks));
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
                if (mob instanceof Villager v) {
                    // a villager's brain re-plans its own walk every few ticks; this is the memory it walks by
                    v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(caller, false), (float) SPEED, 2));
                }
            }
        }
    }
}
