package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Packs that are allies cannot hurt each other (plan 14): a hit between two players of allied packs is cancelled. They share no perks. */
public final class AllyProtection {
    private AllyProtection() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, AllyProtection::onDamage);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer victim && e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim
                && PackIndex.get(victim.serverLevel()).alliedPlayers(attacker.getUUID(), victim.getUUID())) {
            e.setCanceled(true);
        }
    }
}
