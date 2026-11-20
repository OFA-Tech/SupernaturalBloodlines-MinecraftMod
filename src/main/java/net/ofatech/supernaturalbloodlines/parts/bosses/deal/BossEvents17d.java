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

/** The listeners phase 17d adds: a blow that breaks Elijah''s oath, the Elixir that saves from a death, and the Cure that sends Rebekah. */
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
        if (e.getEntity() instanceof NpcOriginalBoss victim && !victim.level().isClientSide && e.getSource().getEntity() instanceof ServerPlayer attacker && e.getAmount() > 0F) {
            Deals.struck(attacker, victim);
        }
    }

    static ElixirRules.Kind kindOf(ServerPlayer p, DamageSource source) {
        if (source.is(LineDeath.DAMAGE_TYPE)) {
            return ElixirRules.Kind.LINE_DEATH;
        }
        if (source.is(DamageTypes.GENERIC_KILL)) {
            return ElixirRules.Kind.KILL_COMMAND;
        }
        // an Original who is killed by white oak is the end that starts a line death: nothing may keep them from it
        if (OriginalRules.isOriginal(BloodlineAccess.get(p).species()) && OriginalRules.isWhiteOakKill(source)) {
            return ElixirRules.Kind.LINE_DEATH;
        }
        return ElixirRules.Kind.OTHER;
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