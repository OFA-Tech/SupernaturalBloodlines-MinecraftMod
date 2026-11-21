package net.ofatech.supernaturalbloodlines.parts.species.item;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * A vampire mob's blood bottle drop remembers whose blood it is (R210): a common vampire, or, for an NPC Original, an Original (Klaus: an Original hybrid).
 * Every other source of a bottle (loot, trades, task rewards) stays unstamped, which counts as common vampire blood.
 */
public final class BloodDrops {
    private BloodDrops() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BloodDrops::onDrops);
    }

    private static void onDrops(LivingDropsEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || victim instanceof Player || VampirismAPI.factionRegistry().getFaction(victim) != VReference.VAMPIRE_FACTION) {
            return;
        }
        BloodSource source = new BloodSource(victim.getUUID(), victim.getName().getString(), true, kindOf(victim));
        for (ItemEntity drop : e.getDrops()) {
            if (drop.getItem().is(TurningHandler.VAMPIRE_BLOOD) && !BloodStamp.read(drop.getItem()).isPresent()) {
                BloodStamp.write(drop.getItem(), source);
            }
        }
    }

    static BloodKind kindOf(LivingEntity victim) {
        if (!victim.hasData(SpeciesRegistries.ORIGINAL_ROOT)) {
            return BloodKind.COMMON_VAMPIRE;
        }
        return NpcOriginal.byId(victim.getData(SpeciesRegistries.ORIGINAL_ROOT))
                .map(o -> o == NpcOriginal.KLAUS ? BloodKind.ORIGINAL_HYBRID : BloodKind.ORIGINAL)
                .orElse(BloodKind.COMMON_VAMPIRE);
    }
}
