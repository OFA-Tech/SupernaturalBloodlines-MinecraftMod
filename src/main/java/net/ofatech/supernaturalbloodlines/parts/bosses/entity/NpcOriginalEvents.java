package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.DefeatRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/** What a blow does to an NPC Original ({@link DefeatRules}), and what their death does to the world ({@link LineDeath}). */
public final class NpcOriginalEvents {
    private static final ResourceLocation ASH_DAGGER = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "white_oak_ash_dagger");

    private NpcOriginalEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, NpcOriginalEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDamageEvent.Post.class, NpcOriginalEvents::afterDamage);
        // LOWEST, and never a death something else cancelled: a boss that did not die is no death
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, NpcOriginalEvents::onDeath);
    }

    static DefeatRules.Kind kindOf(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity a && BuiltInRegistries.ITEM.getKey(a.getMainHandItem().getItem()).equals(ASH_DAGGER)
                && source.getDirectEntity() == a) {
            return DefeatRules.Kind.ASH_DAGGER;
        }
        return OriginalRules.isWhiteOakKill(source) ? DefeatRules.Kind.WHITE_OAK : DefeatRules.Kind.OTHER;
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof NpcOriginalBoss boss) || boss.level().isClientSide) {
            return;
        }
        boss.takePendingDesiccation(); // one a blow cancelled further down the line would otherwise go off on the next one
        DamageSource source = e.getSource();
        DefeatRules.Kind kind = kindOf(source);
        // an operator's /kill and the void are not a fight: they end it
        if (kind == DefeatRules.Kind.OTHER && (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD))) {
            return;
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
        if (OriginalRules.isWhiteOakKill(e.getSource()) && SpeciesConfig.originalLineDeath()) {
            // permanent: the whole line dies with them (LineDeath.run also marks the root dead)
            LineDeath.run(level.getServer(), boss.original().uuid());
        }
    }
}
