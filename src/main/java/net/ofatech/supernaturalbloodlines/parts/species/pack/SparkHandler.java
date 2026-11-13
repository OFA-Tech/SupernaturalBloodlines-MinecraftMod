package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * The Alpha spark (R93): stolen by killing the Alpha, inherited by the strongest online Beta when the Alpha leaves the
 * werewolves or is cured, given up to heal someone. Also what a kill means for the killer's eyes (an innocent: a villager,
 * or a player who was not attacking them). Ordinary deaths keep rank and spark. Touched only behind
 * {@code ModList.isLoaded("werewolves")}.
 * <p>
 * The pack moves with the spark: {@link PackIndex#transfer} moves the index, an online Beta's own {@code packAlpha} is
 * rewritten here and an offline one follows the index when they log in ({@link PackIndex#login}). A pack with nobody
 * online to take it over dissolves ({@link PackIndex#dissolve}). Online means the player list, or the test lookup of
 * {@link SireCall#player}.
 */
public final class SparkHandler {
    // what a given-up spark cures: Werewolves' wolfsbane, our strains and the hybrid venom. Matched by id: the strains
    // belong to the artifacts part and may not be there
    private static final List<ResourceLocation> CURED = List.of(
            ResourceLocation.parse("supernaturalbloodlines:hybrid_venom"),
            ResourceLocation.parse("werewolves:wolfsbane"),
            ResourceLocation.parse("supernaturalbloodlines:wolfsbane_yellow"),
            ResourceLocation.parse("supernaturalbloodlines:wolfsbane_northern_blue"),
            ResourceLocation.parse("supernaturalbloodlines:wolf_lichen"));

    private SparkHandler() {
    }

    public static void register() {
        // LOW and not receiving cancelled events, like GeneHandler (registered first, so an awakening comes before the
        // innocent kill): a death something prevented is no kill
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, SparkHandler::onDeath);
        NeoForge.EVENT_BUS.addListener(SparkHandler::onFaction);
    }

    /** The damage source's attacker, else the vanilla kill credit (the last attacker of a victim who fell or burned). */
    @Nullable
    static Entity killerOf(LivingEntity victim, DamageSource source) {
        Entity killer = source.getEntity();
        return killer != null ? killer : victim.getKillCredit();
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !(killerOf(victim, e.getSource()) instanceof ServerPlayer killer) || killer == victim) {
            return;
        }
        if (victim instanceof ServerPlayer v && PackAccess.isAlpha(PackAccess.wolf(v).rank())) {
            steal(killer, v);
        }
        if (isInnocent(killer, victim)) {
            PackAccess.applyPack(killer, PackEvent.KILLED_INNOCENT, PackContext.defaults());
        }
    }

    /** A player who attacked the killer within this many ticks of dying was fighting them, not innocent. */
    static final int FIGHT_WINDOW = 200;

    /** A villager, or a player who was not fighting the killer (did not hit them in the last 10 seconds). */
    static boolean isInnocent(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Villager) {
            return true;
        }
        return victim instanceof Player p && !(p.getLastHurtMob() == killer && p.tickCount - p.getLastHurtMobTimestamp() <= FIGHT_WINDOW);
    }

    private static PackContext context(@Nullable UUID otherAlpha, long now) {
        return new PackContext(otherAlpha, 0, now, 0, 30L * 24000, 300, otherAlpha != null);
    }

    /**
     * A werewolf player kills an Alpha: they take the spark (and the pack: the killer's own pack stays theirs and the
     * victim's Betas join it), the victim becomes an Omega. Anyone else killing an Alpha changes nothing.
     */
    static boolean steal(ServerPlayer killer, ServerPlayer victim) {
        if (!VampirismSupernatural.isWerewolf(killer) || PackAccess.applyPack(killer, PackEvent.KILLED_ALPHA, PackContext.defaults()).refused()) {
            return false;
        }
        PackIndex index = PackIndex.get(victim.serverLevel());
        List<UUID> betas = index.betasOf(victim.getUUID());
        PackAccess.applyPack(victim, PackEvent.SPARK_LOST, PackContext.defaults());
        index.transfer(victim.getUUID(), killer.getUUID());
        betas.forEach(b -> repoint(victim.server, b, killer.getUUID()));
        return true;
    }

    private static void onFaction(PlayerFactionEvent.FactionLevelChanged e) {
        if (!(e.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide || e.getOldFaction() != WerewolfFaction.faction()
                || e.getCurrentFaction() == WerewolfFaction.faction() || !PackAccess.isAlpha(PackAccess.wolf(p).rank())) {
            return;
        }
        passOn(p);
    }

    /** The best heir of {@code alpha}: the highest-level online Beta of their pack who is still a werewolf. */
    @Nullable
    static ServerPlayer heirOf(ServerPlayer alpha) {
        ServerPlayer best = null;
        int bestLevel = -1;
        for (UUID id : PackIndex.get(alpha.serverLevel()).betasOf(alpha.getUUID())) {
            ServerPlayer b = SireCall.player(alpha.server, id);
            if (b == null || b == alpha || !VampirismSupernatural.isWerewolf(b) || PackAccess.wolf(b).rank() != Rank.BETA
                    || !alpha.getUUID().equals(PackAccess.wolf(b).packAlpha())) {
                continue;
            }
            int level = VampirismAPI.factionPlayerHandler(b).getCurrentLevel();
            if (level > bestLevel) {
                best = b;
                bestLevel = level;
            }
        }
        return best;
    }

    /** The Alpha leaves the werewolves or is cured: the spark goes to the heir, the Alpha is an Omega with no pack. */
    static void passOn(ServerPlayer alpha) {
        ServerPlayer heir = heirOf(alpha);
        if (heir != null) {
            handOver(alpha, heir);
        } else {
            dissolve(alpha);
        }
        PackAccess.applyPack(alpha, PackEvent.SPARK_LOST, PackContext.defaults());
    }

    /**
     * The Alpha sacrifices the spark: it goes to the heir and the Alpha stays in the pack as a Beta. With nobody to take it
     * the pack dissolves and the Alpha is an Omega.
     */
    static void giveUp(ServerPlayer alpha) {
        ServerPlayer heir = heirOf(alpha);
        if (heir == null) {
            dissolve(alpha);
            PackAccess.applyPack(alpha, PackEvent.SPARK_LOST, PackContext.defaults());
            return;
        }
        handOver(alpha, heir);
        PackAccess.applyPack(alpha, PackEvent.SPARK_GIVEN_UP, context(heir.getUUID(), alpha.level().getGameTime()));
    }

    private static void handOver(ServerPlayer from, ServerPlayer heir) {
        PackIndex index = PackIndex.get(from.serverLevel());
        PackAccess.applyPack(heir, PackEvent.SPARK_INHERITED, PackContext.defaults());
        List<UUID> betas = index.betasOf(from.getUUID());
        index.transfer(from.getUUID(), heir.getUUID());
        betas.forEach(b -> repoint(from.server, b, heir.getUUID()));
    }

    private static void dissolve(ServerPlayer alpha) {
        PackIndex index = PackIndex.get(alpha.serverLevel());
        for (UUID id : new ArrayList<>(index.betasOf(alpha.getUUID()))) {
            ServerPlayer online = SireCall.player(alpha.server, id);
            if (online != null) {
                PackAccess.applyPack(online, PackEvent.LEFT_PACK, PackContext.defaults());
            }
        }
        index.dissolve(alpha.getUUID());
    }

    /** An online Beta now belongs to {@code newAlpha}; the index was moved already, so only their own state changes. */
    private static void repoint(MinecraftServer server, UUID beta, UUID newAlpha) {
        ServerPlayer b = SireCall.player(server, beta);
        WolfState w = b == null ? null : PackAccess.wolf(b);
        if (w != null && w.lineage() != Lineage.NONE && w.packAlpha() != null && !beta.equals(newAlpha)) {
            BloodlineAccess.set(b, BloodlineAccess.get(b).toBuilder().wolf(w.toBuilder().packAlpha(newAlpha).build()).build());
        }
    }

    /** Whether {@code alpha} still holds the spark: an online player by rank, an offline one unless the index saw them lose it. */
    public static boolean hasSpark(MinecraftServer server, UUID alpha) {
        ServerPlayer online = SireCall.player(server, alpha);
        if (online != null) {
            return PackAccess.isAlpha(PackAccess.wolf(online).rank());
        }
        return !PackIndex.get(server.overworld()).isFormerAlpha(alpha);
    }

    /** Full heal and everything the spark can cure. */
    static void cure(ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        for (ResourceLocation id : CURED) {
            BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(target::removeEffect);
        }
    }

}
