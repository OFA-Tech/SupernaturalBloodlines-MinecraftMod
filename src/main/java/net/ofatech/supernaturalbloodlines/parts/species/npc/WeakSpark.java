package net.ofatech.supernaturalbloodlines.parts.species.npc;

import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfAlpha;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WeakSparkRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * The weaker spark of an Alpha of the Werewolves mod (R145): a werewolf who kills one, a natural forest Alpha or one a pack spawned, gets it straight
 * away (no item to steal or dupe). It lives in {@code WolfState.weak}, so it counts for nothing and cannot be stolen; it grows while its holder is
 * online and, when grown, becomes a normal spark by the rules of a steal. Links Werewolves: registered only when it is loaded.
 */
public final class WeakSpark {
    /** Growth is written every 10 seconds, not every tick: a holder who logs out loses at most that. */
    static final int PERIOD = 200;
    private static final String KEY = "supernaturalbloodlines.pack.";

    private WeakSpark() {
    }

    public static void register() {
        // LOW and not receiving cancelled events, next to SparkHandler: a death something prevented is no kill
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, WeakSpark::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % PERIOD == 0 && PackAccess.wolf(p).weakSparks() > 0) {
                grow(p, PERIOD);
            }
        });
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !(victim instanceof IWerewolfAlpha)) {
            return;
        }
        Entity credit = e.getSource().getEntity() != null ? e.getSource().getEntity() : victim.getKillCredit();
        if (credit instanceof ServerPlayer killer) {
            killedAlpha(killer);
        }
    }

    /** What the kill of an Alpha of the Werewolves mod means for {@code killer}. */
    public static WeakSparkRules.Grant killedAlpha(ServerPlayer killer) {
        SpeciesConfig.NpcTuning t = SpeciesConfig.npc();
        if (!t.alphaSpark() || !VampirismSupernatural.isWerewolf(killer)) {
            return WeakSparkRules.Grant.NEVER;
        }
        long today = WeakSparkRules.day(killer.level().getGameTime());
        WolfState wolf = PackAccess.wolf(killer);
        WeakSparkRules.Grant grant = WeakSparkRules.grant(wolf, today, t.maxHeld(), t.cooldownDays());
        switch (grant) {
            case GRANTED -> {
                setWeak(killer, WeakSparkRules.given(wolf.weak(), today));
                killer.sendSystemMessage(Component.translatable(KEY + "weak_spark_gained", t.growDays()));
            }
            case DESTROYED -> killer.sendSystemMessage(Component.translatable(KEY + "weak_spark_destroyed"));
            case FULL -> killer.sendSystemMessage(Component.translatable(KEY + "weak_spark_full", t.maxHeld()));
            case COOLDOWN -> killer.sendSystemMessage(Component.translatable(KEY + "weak_spark_cooldown", t.cooldownDays()));
            case NEVER -> {
            }
        }
        return grant;
    }

    /** {@code ticks} more online ticks of growth for {@code p}; a spark that finishes becomes a normal one, or withers to protect an open path. */
    public static void grow(ServerPlayer p, int ticks) {
        SpeciesConfig.NpcTuning t = SpeciesConfig.npc();
        WolfState wolf = PackAccess.wolf(p);
        WeakSparkRules.Grown g = WeakSparkRules.grow(wolf.weak(), ticks, t.growDays() * 24000);
        setWeak(p, g.weak());
        if (!g.grown()) {
            return;
        }
        PackRules.Steal verdict = WeakSparkRules.onGrown(PackAccess.wolf(p));
        switch (verdict) {
            case TAKES -> promote(p);
            case PROTECTED -> p.sendSystemMessage(Component.translatable(KEY + "weak_spark_withered"));
            case NEVER -> {
            }
        }
    }

    /** A grown weak spark: the holder takes one normal spark by the rules of a steal (an Omega or a Beta becomes an Alpha; the cap holds). */
    private static void promote(ServerPlayer p) {
        WolfState before = PackAccess.wolf(p);
        boolean alpha = PackAccess.isAlpha(before.rank());
        if (PackAccess.applyPack(p, PackEvent.KILLED_ALPHA, PackContext.defaults()).refused()) {
            return;
        }
        int cap = SpeciesConfig.maxStackedSparks();
        int now = PackRules.sparksAfterSteal(before.sparks(), alpha, 1, cap);
        PackAccess.setSparks(p, now);
        p.sendSystemMessage(Component.translatable(KEY + "weak_spark_grown", now, cap));
    }

    private static void setWeak(ServerPlayer p, WolfState.Weak weak) {
        BloodlineState s = BloodlineAccess.get(p);
        if (!s.wolf().weak().equals(weak)) {
            BloodlineAccess.set(p, s.toBuilder().wolf(s.wolf().toBuilder().weak(weak).build()).build());
        }
    }
}
