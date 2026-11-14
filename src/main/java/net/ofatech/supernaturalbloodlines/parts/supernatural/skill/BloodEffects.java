package net.ofatech.supernaturalbloodlines.parts.supernatural.skill;

import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HumanLikeBlood;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * The BLOOD skills: what a player gets when they feed. Keen Fangs (S-VAM-1) adds blood to the amount of a human-like feed, in
 * Vampirism's own {@link BloodDrinkEvent.PlayerDrinkBloodEvent} (its amount is settable and is what the vampire's blood level gets).
 * It runs after the Vervain rule (which sets the amount to 0 for a protected victim): a feed that gave nothing stays nothing.
 */
public final class BloodEffects {
    private BloodEffects() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, BloodEffects::onDrinkBlood);
    }

    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        if (e.getAmount() > 0 && source != null && e.getVampire().getRepresentingEntity() instanceof ServerPlayer p && HumanLikeBlood.test(source)) {
            e.setAmount(e.getAmount() + (int) LadderQuery.value(p, "S-VAM-1", "extra_blood", 0));
        }
    }
}
