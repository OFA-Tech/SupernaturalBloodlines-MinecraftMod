package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.boss.CureMadeEvent;
import net.ofatech.supernaturalbloodlines.core.boss.ElixirRules;
import net.ofatech.supernaturalbloodlines.core.boss.RingProbe;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/** The listeners phase 17d adds: a blow that breaks Elijah's oath, the Elixir that saves from a death, and the Cure that sends Rebekah. */
public final class BossEvents17d {
    private BossEvents17d() {
    }

    public static void register() {
        // first: the attempt is the attack, whatever a later listener makes of the damage (a fallen Original takes none)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LivingIncomingDamageEvent.class, BossEvents17d::onIncoming);
        // NORMAL, like the Resurrection Ring, and never a death something else cancelled; the ring is asked, so it goes first whatever the order
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, BossEvents17d::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, CureMadeEvent.class, BossDirector::onCureMade);
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof NpcOriginalBoss victim && !victim.level().isClientSide && e.getAmount() > 0F) {
            // a tamed animal or a minion strikes for whoever owns it: the oath is about what you set on them too
            ServerPlayer attacker = Deals.playerBehind(victim.level().getServer(), e.getSource().getEntity());
            if (attacker != null && Deals.counts(attacker, e.getSource().getEntity(), victim)) {
                Deals.struck(attacker, victim);
            }
        }
    }

    static ElixirRules.Kind kindOf(ServerPlayer p, DamageSource source) {
        var state = BloodlineAccess.get(p);
        // an Original who is killed by white oak is the end that starts a line death: nothing may keep them from it
        boolean originalEnd = OriginalRules.isOriginal(state.species()) && OriginalRules.isWhiteOakKill(source);
        boolean turning = state.vampireBloodUntil() > p.level().getGameTime()
                || net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.ORIGINAL_BLOOD_EFFECT.isBound()
                && p.hasEffect(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.ORIGINAL_BLOOD_EFFECT)
                || state.inTransition() && state.unstable();
        return ElixirRules.classify(source.is(LineDeath.DAMAGE_TYPE), source.is(DamageTypes.GENERIC_KILL), originalEnd, turning,
                source.is(DamageTypes.FELL_OUT_OF_WORLD));
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Inventory inv = p.getInventory();
        ItemStack elixir = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize() && elixir.isEmpty(); i++) {
            if (inv.getItem(i).is(BossItems.THE_ELIXIR.get())) {
                elixir = inv.getItem(i);
            }
        }
        if (elixir.isEmpty() || !ElixirRules.saves(kindOf(p, e.getSource()), RingProbe.wouldSave(p, e.getSource()))) {
            return;
        }
        elixir.shrink(1);
        e.setCanceled(true);
        p.setHealth(ElixirRules.healTo(p.getMaxHealth(), BossesConfig.elixirHealFraction()));
        p.removeAllEffects();
        p.setRemainingFireTicks(0);
        p.fallDistance = 0F;
        p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.boss.elixir.used"));
    }
}