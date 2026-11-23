package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.DefeatRules;
import net.ofatech.supernaturalbloodlines.core.boss.NoticeRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/** What a blow does to an NPC Original ({@link DefeatRules}), and what their death does to the world ({@link LineDeath}). */
public final class NpcOriginalEvents {
    /** Either dagger: an NPC Original is never an Original hybrid to R33 (Klaus included), so the plain one works on him as it always did. */
    private static final java.util.Set<ResourceLocation> ASH_DAGGERS = java.util.Set.of(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "white_oak_ash_dagger"),
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "cursed_white_oak_ash_dagger"));
    /** When each player was last told that white oak needs a fallen Original (game ticks). */
    private static final Map<UUID, Long> HINTED = new ConcurrentHashMap<>();

    private NpcOriginalEvents() {
    }

    public static void register() {
        // last, so the damage the other mods and the game have worked out is the one that is capped
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingIncomingDamageEvent.class, NpcOriginalEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDamageEvent.Post.class, NpcOriginalEvents::afterDamage);
        // LOWEST, and never a death something else cancelled: a boss that did not die is no death
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, NpcOriginalEvents::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> HINTED.clear());
    }

    static DefeatRules.Kind kindOf(DamageSource source) {
        // only a player's dagger: a mob's (a zombie with one) would desiccate a boss and pay a reward to nobody
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player a && ASH_DAGGERS.contains(BuiltInRegistries.ITEM.getKey(a.getMainHandItem().getItem()))
                && source.getDirectEntity() == a) {
            return DefeatRules.Kind.ASH_DAGGER;
        }
        return OriginalRules.isWhiteOakKill(source) ? DefeatRules.Kind.WHITE_OAK : DefeatRules.Kind.OTHER;
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof NpcOriginalBoss boss) || boss.level().isClientSide || net.ofatech.supernaturalbloodlines.core.death.DamageProbe.active()) {
            return; // a probe deals nothing: it must not desiccate, spare or hint
        }
        boss.takePendingDesiccation(); // one a blow cancelled further down the line would otherwise go off on the next one
        DamageSource source = e.getSource();
        DefeatRules.Kind kind = kindOf(source);
        // an operator's /kill and the void are not a fight: they end it
        if (kind == DefeatRules.Kind.OTHER && (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD))) {
            return;
        }
        if (kind == DefeatRules.Kind.WHITE_OAK && !boss.isDesiccated() && BossesConfig.stakeNeedsOpen() && source.getEntity() instanceof ServerPlayer attacker) {
            hint(attacker);
        }
        float amount = e.getAmount();
        boolean lethal = amount >= boss.getHealth() + boss.getAbsorptionAmount();
        DefeatRules.Verdict verdict = DefeatRules.verdict(kind, lethal, boss.isDesiccated(), BossesConfig.stakeNeedsOpen());
        switch (verdict) {
            case HURT, DIE -> {
            }
            case SPARE -> leaveOne(e, boss);
            case DESICCATE -> {
                // the blow lands first (an Ash Dagger's backlash needs a blow that landed), and he goes down after it, at 1 health
                if (boss.getHealth() - 1.0F > 0.0F) {
                    leaveOne(e, boss);
                    boss.desiccateAfterHit();
                } else {
                    e.setCanceled(true);
                    boss.desiccate();
                }
            }
        }
    }

    /** R159: "White oak only works on an Original who is down", on the action bar, at most once every few seconds for each player. */
    private static void hint(ServerPlayer attacker) {
        long now = attacker.level().getGameTime();
        Long last = HINTED.get(attacker.getUUID());
        if (NoticeRules.due(last == null ? -1L : last, now)) {
            HINTED.put(attacker.getUUID(), now);
            attacker.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.white_oak_closed"), true);
        }
    }

    /** The blow lands but cannot take the last health. */
    private static void leaveOne(LivingIncomingDamageEvent e, NpcOriginalBoss boss) {
        float room = boss.getHealth() - 1.0F;
        if (room <= 0.0F) {
            e.setCanceled(true);
        } else {
            e.setAmount(Math.min(e.getAmount(), room));
        }
    }

    private static void afterDamage(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof NpcOriginalBoss boss && !boss.level().isClientSide && boss.takePendingDesiccation() && boss.isAlive()) {
            boss.desiccate();
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof NpcOriginalBoss boss) || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BossIndex.get(level.getServer()).apply(boss.boss(), BossRules.Event.KILLED);
        boss.pay(RewardRules.Defeat.DIED);
        LineWarning.closed(boss.original().uuid());
        // permanent, and only for a white oak death the rules allow: a closed Original that the stake cannot reach (npc_original_stake_needs_open) takes none
        if (OriginalRules.isWhiteOakKill(e.getSource()) && SpeciesConfig.originalLineDeath() && (boss.isDesiccated() || !BossesConfig.stakeNeedsOpen())) {
            LineDeath.run(level.getServer(), boss.original().uuid());
        }
    }
}
