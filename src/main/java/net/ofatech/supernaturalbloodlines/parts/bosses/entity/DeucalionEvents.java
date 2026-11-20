package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.DemonRoarRules;
import net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.supernatural.GrantedSkills;
import org.jetbrains.annotations.Nullable;

/**
 * What Deucalion's death does (R171, R172) and what his pack never gives. A real death (not a prevented one) writes him down as dead and the day he died
 * (for {@code deucalion_respawn}), takes the pack with him, and pays the first time: {@link SparkHandler#grantFromBoss} for the killer, and the Demon Wolf
 * Roar to a werewolf killer. Only a player can be the killer: a mob's blow takes nothing and gives nothing, and the reward is spent all the same. The
 * Alphas of his pack give no drops and no experience (and no weak spark: {@code WeakSpark} skips them).
 */
public final class DeucalionEvents {
    private DeucalionEvents() {
    }

    public static void register() {
        // the skill counts while the one who holds it is a werewolf: a cured killer keeps it in the saved skills, inactive
        GrantedSkills.register(DemonRoarRules.SKILL, VampirismSupernatural::isWerewolf);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDeathEvent.class, DeucalionEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(LivingDropsEvent.class, e -> {
            if (isBossPack(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingExperienceDropEvent.class, e -> {
            if (isBossPack(e.getEntity())) {
                e.setCanceled(true);
            }
        });
    }

    public static boolean isBossPack(Entity e) {
        return e.hasData(SpeciesRegistries.NPC_MARK) && e.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.BOSS_PACK;
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof DeucalionEntity d) || !(d.level() instanceof ServerLevel level)) {
            return;
        }
        BossIndex index = BossIndex.get(level.getServer());
        index.apply(BossId.DEUCALION, BossRules.Event.KILLED);
        index.of(BossId.DEUCALION).returnDay = BossClock.today(level);
        d.dismissPack();
        payReward(level, SparkHandler.killerOf(d, e.getSource()) instanceof ServerPlayer killer ? killer : null);
        index.setDirty();
        BossIndex.flush(level.getServer());
    }

    /** The reward, once: the sparks and the Roar for {@code killer}, who may be null. Whether it was paid. */
    public static boolean payReward(ServerLevel level, @Nullable ServerPlayer killer) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.DEUCALION);
        if (!RewardRules.pays(BossesConfig.rewardOn(), RewardRules.Defeat.DIED, entry.rewarded)) {
            return false;
        }
        entry.rewarded = true;
        if (killer != null) {
            SparkHandler.grantFromBoss(killer, BossesConfig.deucalionSparks());
            giveRoar(killer);
        }
        return true;
    }

    /** The Demon Wolf Roar goes into the killer's saved skills, for a werewolf only (the rule keeps it inactive for one who stops being one). */
    public static void giveRoar(ServerPlayer killer) {
        if (!DeucalionRewardRules.givesRoar(VampirismSupernatural.isWerewolf(killer))) {
            return;
        }
        var state = SupernaturalAccess.get(killer);
        if (state.skills().contains(DemonRoarRules.SKILL)) {
            return;
        }
        Set<String> skills = new HashSet<>(state.skills());
        skills.add(DemonRoarRules.SKILL);
        SupernaturalAccess.set(killer, state.toBuilder().skills(skills).build());
        killer.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.deucalion.roar_given"));
    }
}
