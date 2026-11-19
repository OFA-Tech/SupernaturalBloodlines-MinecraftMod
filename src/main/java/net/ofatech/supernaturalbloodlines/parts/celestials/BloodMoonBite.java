package net.ofatech.supernaturalbloodlines.parts.celestials;

import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;

/** A vampire's bite gives more blood on a Blood Moon. Links Vampirism: call only when it is loaded. Last, so after Keen Fangs and the vervain rule. */
final class BloodMoonBite {
    private BloodMoonBite() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BloodMoonBite::onDrinkBlood);
    }

    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        if (e.getAmount() > 0 && e.getBloodSource().getEntity().isPresent() && e.getVampire().getRepresentingEntity() instanceof ServerPlayer p) {
            e.setAmount(LunarMood.scaled(e.getAmount(), LunarMoods.biteFactor(p.level())));
        }
    }
}
