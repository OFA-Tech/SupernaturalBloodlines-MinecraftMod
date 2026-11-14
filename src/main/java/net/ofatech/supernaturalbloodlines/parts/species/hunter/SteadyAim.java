package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/** Steady Aim (S-HUN-1): a hunter who owns it deals 5% more damage to prey (R81). Nothing changes for anyone else. */
public final class SteadyAim {
    private SteadyAim() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SteadyAim::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && MarkTracker.isPrey(e.getEntity())) {
            e.setAmount((float) (e.getAmount() * LadderQuery.value(p, "S-HUN-1", "prey_damage_factor", 1.0)));
        }
    }
}