package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * The Alpha spark (R93): stolen by killing an Alpha from outside their pack (the Deucalion factor stacks the victim's on the
 * killer's), inherited by the strongest second Alpha, else the strongest online Beta, when the Alpha leaves the werewolves
 * or is cured; given up to heal someone it is destroyed. A True Alpha keeps it: it cannot be stolen, inherited or given up, and a True Alpha never steals one. Also what a kill means for the killer's eyes (an innocent: a villager,
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
        BloodlineAccess.addListener(SparkHandler::onWolfLayerCleared);
    }

    /**
     * A cure (the Cure, the Silver Moon, a command) clears the wolf layer, rank included, before the faction is left, so
     * {@link #onFaction} would see a player who is no Alpha any more. The spark moves here instead: the layer is
     * cleared (lineage gone) while the player is still a werewolf. A True Alpha keeps the rank and is not cleared.
     */
    private static void onWolfLayerCleared(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        WolfState before = change.before().wolf();
        if (before.rank() == Rank.ALPHA && change.after().wolf().lineage() == Lineage.NONE && before.lineage() != Lineage.NONE
                && !p.level().isClientSide && VampirismSupernatural.isWerewolf(p)) {
            if (before.sparks() > 1) {
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.spark_reset", before.sparks() - 1));
            }
            passOn(p);
        }
    }

    /** The damage source's attacker, else the vanilla kill credit (the last attacker of a victim who fell or burned). */
    @Nullable
    public static Entity killerOf(LivingEntity victim, DamageSource source) {
        Entity killer = source.getEntity();
        killer = killer != null ? killer : victim.getKillCredit();
        // a pack member (a Brother, a Tensura subordinate) is never the killer of a spark, and gives its owner none
        boolean member = killer != null && net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers.bondOf(killer).isPresent();
        return killer == null || net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules.mayTakeSpark(killer instanceof ServerPlayer, member) ? killer : null;
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !(killerOf(victim, e.getSource()) instanceof ServerPlayer killer) || killer == victim) {
            return;
        }
        if (victim instanceof ServerPlayer v && PackAccess.isAlpha(PackAccess.wolf(v).rank())) {
            steal(killer, v);
        }
        // a kill inside the pack (or a moment after leaving it) is no innocent kill: no blue eyes, no True Alpha block
        if (isInnocent(killer, victim) && !(victim instanceof ServerPlayer v && PackIndex.samePack(killer, v))) {
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
     * A werewolf player kills an Alpha from <b>outside</b> their pack (the Deucalion factor): they take every spark the victim
     * had, on top of their own when they are an Alpha already (up to {@code max_stacked_sparks}); a werewolf who was no Alpha
     * becomes one. The victim, who had them all, is an Omega with no pack to lead. A True Alpha's spark is theirs by nature and
     * is never taken, and a True Alpha who kills takes nothing. A killer whose path to True Alpha is open and protected destroys the spark
     * instead of taking it ({@link PackRules#stealBy}).
     *
     * <p>A kill inside the pack, outside a duel, changes nothing at all (owner ruling): no spark, no lead, no rank. Anyone who
     * is no werewolf killing an Alpha changes nothing either.
     */
    static boolean steal(ServerPlayer killer, ServerPlayer victim) {
        if (!VampirismSupernatural.isWerewolf(killer)) {
            return false;
        }
        PackIndex index = PackIndex.get(victim.serverLevel());
        if (PackIndex.samePack(killer, victim)) {
            if (index.onlyByGrace(killer.getUUID(), victim.getUUID(), SpeciesConfig.leftPackGraceMinutes())) {
                // the one who left is told so, and so is a killer whose victim left
                boolean you = index.leftRecently(killer.getUUID(), SpeciesConfig.leftPackGraceMinutes());
                killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.grace_no_spark" + (you ? "_you" : "")));
            }
            return false;
        }
        boolean victimTrue = PackAccess.wolf(victim).rank() == Rank.TRUE_ALPHA;
        int taken = PackRules.transferableSparks(PackAccess.wolf(victim).sparks(), victimTrue);
        if (taken <= 0) {
            return false; // a True Alpha's spark is theirs by nature: nothing to take
        }
        PackRules.Steal verdict = PackRules.stealBy(PackAccess.wolf(killer));
        if (verdict == PackRules.Steal.NEVER) {
            return false; // a True Alpha takes nothing: the Alpha they killed keeps the spark
        }
        if (verdict == PackRules.Steal.PROTECTED) {
            // a kill cannot ask first: with the path still open and protected, the spark is destroyed and nobody takes it
            // an unprovoked kill is an innocent one and turns the eyes blue (the next lines of onDeath), which closes the path on its own: say so
            killer.sendSystemMessage(Component.translatable(isInnocent(killer, victim) ? "supernaturalbloodlines.pack.spark_destroyed_innocent" : "supernaturalbloodlines.pack.spark_destroyed"));
            PackMembers.tell(killer.server, victim.getUUID(), "supernaturalbloodlines.pack.spark_destroyed_victim", killer.getGameProfile().getName());
        } else {
            if (!takeSparks(killer, taken)) {
                return false;
            }
            // the victim learns it too (at their next login if away)
            PackMembers.tell(killer.server, victim.getUUID(), "supernaturalbloodlines.pack.spark_lost", killer.getGameProfile().getName());
        }
        boolean victimLeads = index.isLeader(victim.getUUID());
        PackAccess.applyPack(victim, PackEvent.SPARK_LOST, PackContext.defaults()); // the rules also clear every stacked spark with the rank
        if (!victimLeads) {
            // a second Alpha loses the spark and the pack, which goes on under its leader
            index.leave(victim.getUUID());
            index.markFormerAlpha(victim.getUUID());
        } else {
            // the strongest other Alpha leads the pack, and with none it dissolves
            UUID co = coAlphaHeir(victim);
            if (co != null) {
                handOverToCoAlpha(victim, co);
            } else {
                dissolve(victim);
            }
        }
        return true;
    }
    /**
     * What a spark taken from anybody does to its taker: the rank, the new count (up to {@code max_stacked_sparks}) and the two lines that tell them. A steal and
     * a boss's grant both end here. False when the pack rules refused the new rank (nothing changed).
     */
    private static boolean takeSparks(ServerPlayer killer, int taken) {
        boolean killerIsAlpha = PackAccess.isAlpha(PackAccess.wolf(killer).rank());
        int killerSparks = PackAccess.wolf(killer).sparks();
        if (PackAccess.applyPack(killer, PackEvent.KILLED_ALPHA, PackContext.defaults()).refused()) {
            return false;
        }
        int cap = SpeciesConfig.maxStackedSparks();
        int now = PackRules.sparksAfterSteal(killerSparks, killerIsAlpha, taken, cap);
        PackAccess.setSparks(killer, now);
        killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.spark_gained", now, cap));
        if (PackRules.capped(killerSparks, killerIsAlpha, taken, cap)) {
            killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.spark_capped", cap));
        }
        return true;
    }

    /**
     * A boss pays {@code sparks} to the one who killed it (plan 17, decision 8): the same ladder as {@link #steal} ({@link PackRules#stealBy}) and the same
     * {@code max_stacked_sparks}, but nothing is taken from anybody. A killer who is no werewolf gets nothing; a True Alpha takes nothing; an open and
     * protected path destroys them. Touched only behind {@code ModList.isLoaded("werewolves")}.
     */
    public static net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules.Grant grantFromBoss(ServerPlayer killer, int sparks) {
        boolean werewolf = VampirismSupernatural.isWerewolf(killer);
        WolfState wolf = PackAccess.wolf(killer);
        int cap = SpeciesConfig.maxStackedSparks();
        var grant = net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules.sparks(werewolf, PackRules.stealBy(wolf), PackAccess.isAlpha(wolf.rank()),
                wolf.sparks(), sparks, cap);
        switch (grant.outcome()) {
            case PROTECTED -> killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.spark_destroyed"));
            case GIVEN -> {
                if (!takeSparks(killer, sparks)) {
                    return new net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules.Grant(
                            net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules.Outcome.NEVER, wolf.sparks(), false);
                }
            }
            // no branch is silent: the one who did the most is told why they take nothing
            case NOT_A_WEREWOLF -> killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.deucalion.reward_no_werewolf"));
            case NEVER -> killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.deucalion.reward_true_alpha"));
        }
        return grant;
    }

    private static void onFaction(PlayerFactionEvent.FactionLevelChanged e) {
        // a dormant True Alpha is marked as not holding the spark while they are away (hasSpark, offline); rejoining wakes it
        if (e.getPlayer().getPlayer() instanceof ServerPlayer back && !back.level().isClientSide && e.getCurrentFaction() == WerewolfFaction.faction()
                && e.getOldFaction() != WerewolfFaction.faction() && PackAccess.wolf(back).rank() == Rank.TRUE_ALPHA) {
            PackIndex.get(back.serverLevel()).clearFormerAlpha(back.getUUID());
            return;
        }
        if (!(e.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide || e.getOldFaction() != WerewolfFaction.faction()
                || e.getCurrentFaction() == WerewolfFaction.faction() || !PackAccess.isAlpha(PackAccess.wolf(p).rank())) {
            return;
        }
        passOn(p);
    }

    /** The best heir of {@code alpha}: the highest-level online Beta of their pack who is still a werewolf. A protected True Alpha path covers kills only (owner ruling): the heir takes the spark even with an open path, and that closes it. */
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

    /**
     * The co-Alpha who takes the pack when {@code alpha} stops leading it: the strongest other Alpha of the pack, online or not
     * (an Alpha Pack hands over to an Alpha before any Beta is made one).
     */
    @Nullable
    static UUID coAlphaHeir(ServerPlayer alpha) {
        return coAlphaHeir(PackIndex.get(alpha.serverLevel()), alpha.getUUID());
    }

    /** The strongest other Alpha of the pack {@code leader} leads, online or not (the order of {@link PackLeadership}). */
    @Nullable
    static UUID coAlphaHeir(PackIndex index, UUID leader) {
        return coAlphaHeir(index, leader, id -> true);
    }

    /** {@link #coAlphaHeir(PackIndex, UUID)} among the Alphas {@code eligible} accepts (the absent leader's successor must be here). */
    @Nullable
    static UUID coAlphaHeir(PackIndex index, UUID leader, java.util.function.Predicate<UUID> eligible) {
        List<PackLeadership.Candidate> candidates = new java.util.ArrayList<>();
        for (UUID id : index.betasOf(leader)) {
            if (!eligible.test(id)) {
                continue;
            }
            candidates.add(new PackLeadership.Candidate(id, index.isFormerAlpha(id) ? 0 : index.strengthOf(id)));
        }
        return PackLeadership.pick(candidates, null).orElse(null);
    }

    /** The pack of {@code from} goes to the co-Alpha {@code heir}, who is an Alpha already: the index moves, the online Betas follow. */
    private static void handOverToCoAlpha(ServerPlayer from, UUID heir) {
        PackIndex index = PackIndex.get(from.serverLevel());
        List<UUID> members = index.betasOf(from.getUUID());
        index.transfer(from.getUUID(), heir);
        members.forEach(b -> repoint(from.server, b, heir));
        PackMembers.moved(from.server, members, heir);
        PartyLink.leadershipChanged(from.server, heir);
    }

    /** One line for an Alpha whose stacked sparks go with the rank (a cure, a heal, leaving the werewolves). */
    private static void tellReset(ServerPlayer alpha) {
        int extra = PackAccess.wolf(alpha).sparks() - 1;
        if (extra > 0) {
            alpha.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.spark_reset", extra));
        }
    }

    /** The Alpha leaves the werewolves or is cured: the spark goes to the heir, the Alpha is an Omega with no pack. A True Alpha only dissolves the pack. */
    static void passOn(ServerPlayer alpha) {
        PackIndex own = PackIndex.get(alpha.serverLevel());
        if (own.alphaOf(alpha.getUUID()).isPresent()) {
            // a co-Alpha has no pack of their own to hand over. Cured, they stay in the pack as a Packmate; out of the werewolves, they leave it
            own.markFormerAlpha(alpha.getUUID());
            PackAccess.applyPack(alpha, PackEvent.SPARK_LOST, PackContext.defaults());
            if (PackAccess.wolf(alpha).lineage() == Lineage.NONE) {
                own.setStrength(alpha.getUUID(), -1);
            } else {
                own.leave(alpha.getUUID());
            }
            return;
        }
        if (PackAccess.wolf(alpha).rank() == Rank.TRUE_ALPHA) {
            tellReset(alpha);
            PackAccess.setSparks(alpha, 1); // the stacked sparks go with the faction, the True spark stays
            // a True Alpha keeps the spark and the rank (dormant outside the faction), and the pack goes to its strongest second
            // Alpha, or dissolves when it has none. The tombstone says "not holding it now" for an offline check; onFaction clears it on rejoining
            UUID second = coAlphaHeir(alpha);
            if (second != null) {
                handOverToCoAlpha(alpha, second);
            } else {
                dissolve(alpha);
            }
            return;
        }
        tellReset(alpha);
        UUID coHeir = coAlphaHeir(alpha);
        ServerPlayer heir = coHeir == null ? heirOf(alpha) : null;
        if (coHeir != null) {
            handOverToCoAlpha(alpha, coHeir);
        } else if (heir != null) {
            handOver(alpha, heir);
        } else {
            dissolve(alpha);
        }
        PackAccess.applyPack(alpha, PackEvent.SPARK_LOST, PackContext.defaults());
    }

    /**
     * The Alpha gives the spark up to heal someone: it is destroyed, nobody inherits it and the healed player stays what they were (a Beta stays a Beta).
     * A second Alpha stays in the pack as a Beta of its leader. A leader hands the pack to the strongest other Alpha and stays as a Beta of it; with
     * no other Alpha the pack dissolves and they are an Omega.
     */
    static void giveUp(ServerPlayer alpha) {
        tellReset(alpha);
        PackIndex own = PackIndex.get(alpha.serverLevel());
        UUID theirLeader = own.alphaOf(alpha.getUUID()).orElse(null);
        if (theirLeader != null) {
            PackAccess.applyPack(alpha, PackEvent.SPARK_GIVEN_UP, context(theirLeader, alpha.level().getGameTime()));
            return;
        }
        UUID coHeir = coAlphaHeir(alpha);
        if (coHeir != null) {
            handOverToCoAlpha(alpha, coHeir);
            PackAccess.applyPack(alpha, PackEvent.SPARK_GIVEN_UP, context(coHeir, alpha.level().getGameTime()));
            return;
        }
        dissolve(alpha);
        PackAccess.applyPack(alpha, PackEvent.SPARK_LOST, PackContext.defaults());
    }

    private static void handOver(ServerPlayer from, ServerPlayer heir) {
        PackIndex index = PackIndex.get(from.serverLevel());
        PackAccess.applyPack(heir, PackEvent.SPARK_INHERITED, PackContext.defaults());
        List<UUID> betas = index.betasOf(from.getUUID());
        index.transfer(from.getUUID(), heir.getUUID());
        betas.forEach(b -> repoint(from.server, b, heir.getUUID()));
        PackMembers.moved(from.server, betas, heir.getUUID());
        PartyLink.leadershipChanged(from.server, heir.getUUID());
    }

    private static void dissolve(ServerPlayer alpha) {
        PackIndex index = PackIndex.get(alpha.serverLevel());
        for (UUID id : new ArrayList<>(index.betasOf(alpha.getUUID()))) {
            ServerPlayer online = SireCall.player(alpha.server, id);
            if (online != null) {
                PackAccess.applyPack(online, PackEvent.LEFT_PACK, PackContext.defaults());
            }
        }
        List<UUID> told = new ArrayList<>(index.betasOf(alpha.getUUID()));
        index.dissolve(alpha.getUUID());
        // every member is told: now, or at their next login
        for (UUID id : told) {
            ServerPlayer online = SireCall.player(alpha.server, id);
            if (online != null) {
                online.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.dissolved"));
                index.takeDissolved(id);
            } else {
                index.addNotice(id, "supernaturalbloodlines.pack.dissolved", "");
            }
        }
        PartyLink.leadershipChanged(alpha.server, null);
    }

    /** An online Beta now belongs to {@code newAlpha}; the index was moved already, so only their own state changes. */
    static void repoint(MinecraftServer server, UUID beta, UUID newAlpha) {
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
            // a dormant True Alpha (cured, outside the faction) holds the rank but cannot bite anyone into a pack
            return PackAccess.isAlpha(PackAccess.wolf(online).rank()) && VampirismSupernatural.isWerewolf(online);
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
