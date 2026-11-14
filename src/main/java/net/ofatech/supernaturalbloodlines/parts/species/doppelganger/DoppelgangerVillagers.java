package net.ofatech.supernaturalbloodlines.parts.species.doppelganger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.core.attachment.DoppelgangerRoll;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/**
 * Doppelgänger villagers (R14a). Every {@link Villager} is rolled once, the first time it joins a level without a saved roll:
 * a new spawn, a breeding, a cure, a village's own villagers (placed by worldgen, which does not go through
 * {@code FinalizeSpawnEvent}) and a villager from before the update. A villager that has a roll is never rolled again, whether it
 * is reloaded, moved between dimensions or loaded in a new world session. {@code EntityJoinLevelEvent} is the one hook, because it
 * is the only one that sees all of those.
 * <p>
 * The loaded doppelgängers are indexed per dimension on join and leave, so a search (the Vampire Compass) never scans the world.
 */
public final class DoppelgangerVillagers {
    private static final DoubleSupplier RANDOM = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile DoubleSupplier roll = RANDOM;

    private static final Map<ResourceKey<Level>, Map<UUID, Villager>> INDEX = new ConcurrentHashMap<>();

    private DoppelgangerVillagers() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DoppelgangerVillagers::onJoin);
        NeoForge.EVENT_BUS.addListener(DoppelgangerVillagers::onLeave);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> INDEX.clear());
    }

    /** For tests: makes the roll deterministic (a value in [0, 1)). */
    public static void useRoll(DoubleSupplier forced) {
        roll = forced;
    }

    public static void resetRoll() {
        roll = RANDOM;
    }

    /** True for a villager whose roll came up. */
    public static boolean isDoppelganger(Entity e) {
        return e.hasData(ModAttachments.DOPPELGANGER_VILLAGER) && e.getData(ModAttachments.DOPPELGANGER_VILLAGER).doppelganger();
    }

    /** The doppelgänger villagers loaded in {@code level} right now. */
    public static Collection<Villager> loaded(ServerLevel level) {
        Map<UUID, Villager> m = INDEX.get(level.dimension());
        return m == null ? List.of() : List.copyOf(m.values());
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof Villager v)) {
            return;
        }
        if (!v.hasData(ModAttachments.DOPPELGANGER_VILLAGER) || !v.getData(ModAttachments.DOPPELGANGER_VILLAGER).rolled()) {
            v.setData(ModAttachments.DOPPELGANGER_VILLAGER,
                    DoppelgangerRoll.of(roll.getAsDouble(), SpeciesConfig.doppelgangerVillagerChance()));
        }
        if (isDoppelganger(v)) {
            INDEX.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(v.getUUID(), v);
        }
    }

    private static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel() instanceof ServerLevel level && e.getEntity() instanceof Villager v) {
            Map<UUID, Villager> m = INDEX.get(level.dimension());
            if (m != null) {
                m.remove(v.getUUID());
            }
        }
    }
}
