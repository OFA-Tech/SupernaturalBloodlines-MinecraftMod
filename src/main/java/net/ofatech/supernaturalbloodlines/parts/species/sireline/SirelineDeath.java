package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesData;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/**
 * The optional sireline death ({@code sireline_death}, off by default). When an Original or Original hybrid really dies to white
 * oak, everyone below them in the sireline dies: online players at once (once, with {@link #DAMAGE_TYPE}, which gets past
 * invulnerability, armor, effects and totems), offline players the next time they log in, loaded creatures on the spot, unloaded ones
 * when their chunk next loads. Nothing is loaded to do it. An Original further down the line (our state allows a sired
 * Original) survives with their branch. The dead leave the index and lose their sire.
 */
public final class SirelineDeath {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "sireline"));
    private static final String PLAYER = "minecraft:player";

    private SirelineDeath() {
    }

    public static void register() {
        // last, and not for a death something else cancelled: the Original really died
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, SirelineDeath::onDeath);
        // before SireRecorder's login sync, which would index a sire that is about to be cleared
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(SirelineDeath::onJoin);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && SpeciesConfig.sirelineDeath() && OriginalRules.isOriginal(BloodlineAccess.get(p).species())
                && OriginalRules.isWhiteOakKill(e.getSource())) {
            run(p);
        }
    }

    /** Walks everyone below the Original (each only once, so a cycle ends) and sentences them. */
    public static void run(ServerPlayer original) {
        MinecraftServer server = original.server;
        SirelineIndex index = SirelineIndex.get(original.serverLevel());
        SpeciesData originals = SpeciesData.get(server);
        Set<UUID> seen = new HashSet<>();
        seen.add(original.getUUID());
        Deque<UUID> work = new ArrayDeque<>();
        work.add(original.getUUID());
        List<SirelineIndex.Node> doomed = new ArrayList<>();
        while (!work.isEmpty()) {
            for (SirelineIndex.Node child : index.childrenOf(work.poll())) {
                if (!seen.add(child.id()) || (PLAYER.equals(child.entityType()) && isOriginal(server, originals, child.id()))) {
                    continue;
                }
                doomed.add(child);
                work.add(child.id());
            }
        }
        Set<UUID> kept = new HashSet<>();
        for (SirelineIndex.Node n : doomed) {
            if (!PLAYER.equals(n.entityType())) {
                discard(server, index, n.id());
            } else if (SireCall.player(server, n.id()) instanceof ServerPlayer online) {
                if (!slay(online)) {
                    // something kept them alive: they stay in the line, owing the death for their next login
                    index.addPendingPlayer(n.id());
                    kept.add(n.id());
                }
            } else {
                index.addPendingPlayer(n.id());
            }
        }
        doomed.stream().filter(n -> !kept.contains(n.id())).forEach(n -> index.remove(n.id()));
    }

    private static boolean isOriginal(MinecraftServer server, SpeciesData originals, UUID id) {
        ServerPlayer online = SireCall.player(server, id);
        return online != null ? OriginalRules.isOriginal(BloodlineAccess.get(online).species()) : originals.isOriginal(id);
    }

    /**
     * Kills the player (if they are not dead already) and, once they really are dead, ends their sire: the line is broken.
     * A player in Vampirism's dying state ignores every hit, so they are killed through it.
     *
     * @return whether the player is dead now
     */
    static boolean slay(ServerPlayer p) {
        if (!p.isDeadOrDying()) {
            DamageSource blow = p.damageSources().source(DAMAGE_TYPE);
            if (!OriginalRules.killThroughDying(p, blow)) {
                p.hurt(blow, Float.MAX_VALUE);
            }
            if (!p.isDeadOrDying()) {
                p.kill();
            }
        }
        if (!p.isDeadOrDying()) {
            return false;
        }
        BloodlineState s = BloodlineAccess.get(p);
        if (s.sire() != null) {
            BloodlineAccess.set(p, s.toBuilder().sire(null).build());
        }
        return true;
    }

    /** A creature in a level that is loaded goes; one that is not is owed, and goes when it loads. */
    private static void discard(MinecraftServer server, SirelineIndex index, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            // getEntity only finds what is loaded: it never loads a chunk
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
                return;
            }
        }
        index.addPendingMob(id);
    }

    /** A player who was offline for the death pays now, once. */
    public static void onLogin(ServerPlayer p) {
        if (SirelineIndex.get(p.serverLevel()).takePendingPlayer(p.getUUID())) {
            slay(p);
        }
    }

    /** A creature that was owed a death does not join the world. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || e.getEntity() instanceof Player) {
            return;
        }
        if (SirelineIndex.get((ServerLevel) e.getLevel()).takePendingMob(e.getEntity().getUUID())) {
            e.setCanceled(true);
        }
    }
}
