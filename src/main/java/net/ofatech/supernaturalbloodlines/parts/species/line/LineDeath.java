package net.ofatech.supernaturalbloodlines.parts.species.line;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * Line death ({@code original_line_death}, on by default; R122). When an Original really dies to white oak, everyone in their line
 * dies: online players at once (with {@link #DAMAGE_TYPE}, which gets past invulnerability, armor, effects and totems), offline
 * players the next time they log in, loaded creatures on the spot, unloaded ones when their chunk next loads. Nothing is loaded
 * to do it. The Original is never in their own line, and someone freed from it by a rite is not a member.
 */
public final class LineDeath {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "sireline"));

    private LineDeath() {
    }

    public static void register() {
        // last, and not for a death something else cancelled: the Original really died
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, LineDeath::onDeath);
        // before SireRecorder's login sync, which would index a sire that is about to be cleared
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(LineDeath::onJoin);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && SpeciesConfig.originalLineDeath() && OriginalRules.isOriginal(BloodlineAccess.get(p).species())
                && OriginalRules.isWhiteOakKill(e.getSource())) {
            run(p.server, p.getUUID());
        }
    }

    /** The one entry point: {@code root}'s whole line dies. A boss Original who dies to white oak calls it with {@code NpcOriginal.X.uuid()}. */
    public static void run(MinecraftServer server, UUID root) {
        LineIndex index = LineIndex.get(server);
        SirelineIndex sirelines = SirelineIndex.get(server.overworld());
        for (LineIndex.Member m : index.membersOf(root)) {
            if (!m.player()) {
                discard(server, sirelines, m.id());
                index.remove(m.id());
            } else if (SireCall.player(server, m.id()) instanceof ServerPlayer online) {
                online.sendSystemMessage(Component.translatable("supernaturalbloodlines.line.death", Lines.shownName(server, online, root)));
                if (slay(online)) {
                    index.remove(m.id());
                } else {
                    // something kept them alive: they stay in the line, owing the death for their next login
                    index.addOwed(m.id(), root);
                }
            } else {
                // offline: still a member, until they log in and pay
                index.addOwed(m.id(), root);
            }
        }
        if (NpcOriginal.byUuid(root).isPresent()) {
            index.markDead(root);
        }
        discardLoaded(server, root);
    }

    /**
     * Un-sired NPC vampires hold their line on the creature alone, so the index cannot list them: the ones that are loaded go now, the ones that
     * are not are turned away when they load ({@link #onJoin}, through {@code LineIndex.isDead}).
     */
    private static void discardLoaded(MinecraftServer server, UUID root) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> doomed = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof Player) && e.hasData(SpeciesRegistries.LINE) && root.equals(e.getData(SpeciesRegistries.LINE))) {
                    doomed.add(e);
                }
            }
            doomed.forEach(Entity::discard);
        }
    }

    /**
     * Kills the player (if they are not dead already) and, once they really are dead, ends their sire: the line is broken.
     * A player in Vampirism's dying state ignores every hit, so they are killed through it.
     *
     * @return whether the player is dead now
     */
    public static boolean slay(ServerPlayer p) {
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
    private static void discard(MinecraftServer server, SirelineIndex sirelines, UUID id) {
        sirelines.remove(id);
        for (ServerLevel level : server.getAllLevels()) {
            // getEntity only finds what is loaded: it never loads a chunk
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
                return;
            }
        }
        sirelines.addPendingMob(id);
    }

    /**
     * A player who was offline for the death pays now, once, if they are still in that line and still supernatural: someone cured or
     * freed in the meantime owes nothing. The Original is named only if the player has discovered them. A death the old sireline death
     * owed is turned into one of this line first (the Original found from the recorded sires), or dropped when there is none.
     */
    public static void onLogin(ServerPlayer p) {
        LineIndex index = LineIndex.get(p.server);
        if (SirelineIndex.get(p.serverLevel()).takeLegacyPendingPlayer(p.getUUID())) {
            Optional<UUID> root = Lines.heal(p).filter(r -> !r.equals(p.getUUID()));
            if (root.isPresent()) {
                index.addOwed(p.getUUID(), root.get());
            } else {
                SupernaturalBloodlines.LOGGER.warn("Dropping a sireline death owed to {}: no Original is recorded above them", p.getGameProfile().getName());
            }
        }
        index.takeOwed(p.getUUID()).ifPresent(root -> {
            if (!LineRules.owedStillApplies(root, index.rootOf(p.getUUID()), Supernatural.is(p))) {
                return;
            }
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.line.death_login", Lines.shownName(p.server, p, root)));
            if (slay(p)) {
                index.remove(p.getUUID());
            } else {
                index.addOwed(p.getUUID(), root);
            }
        });
    }

    /** A creature that was owed a death does not join the world, and neither does one of a line that is already dead. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || e.getEntity() instanceof Player) {
            return;
        }
        ServerLevel level = (ServerLevel) e.getLevel();
        Entity entity = e.getEntity();
        if (SirelineIndex.get(level).takePendingMob(entity.getUUID())
                || (entity.hasData(SpeciesRegistries.LINE) && LineIndex.get(level.getServer()).isDead(entity.getData(SpeciesRegistries.LINE)))) {
            e.setCanceled(true);
        }
    }
}
