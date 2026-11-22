package net.ofatech.supernaturalbloodlines.parts.species.item;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Optional;
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
 * A drop of vampire blood from an NPC Original is stamped with that Original (R210): their root id (the one a sire is named by) and kind (Klaus an Original
 * hybrid), so drinking it names them. A common vampire mob's drop is not stamped at all: unstamped is common vampire blood, and unstamped bottles keep
 * stacking. Every other source of a bottle (loot, trades, task rewards) stays unstamped too.
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
        Optional<NpcOriginal> original = originalOf(victim);
        if (original.isEmpty()) {
            return;
        }
        BloodKind kind = original.get() == NpcOriginal.KLAUS ? BloodKind.ORIGINAL_HYBRID : BloodKind.ORIGINAL;
        BloodSource source = new BloodSource(original.get().uuid(), victim.getName().getString(), true, kind);
        for (ItemEntity drop : e.getDrops()) {
            if (drop.getItem().is(TurningHandler.VAMPIRE_BLOOD) && !BloodStamp.has(drop.getItem())) {
                BloodStamp.write(drop.getItem(), source);
            }
        }
    }

    static Optional<NpcOriginal> originalOf(LivingEntity victim) {
        return victim.hasData(SpeciesRegistries.ORIGINAL_ROOT) ? NpcOriginal.byId(victim.getData(SpeciesRegistries.ORIGINAL_ROOT)) : Optional.empty();
    }
}
