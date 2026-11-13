package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Called from {@code mixin.speciestensura.RaceUtilsMixin}: Tensura has no event or switch for its undead and
 * holy-exposure checks (docs/parts/species_tensura.md), so the mixin asks here. Switch: {@code undead_treatment_enabled}.
 */
public final class UndeadTreatment {
    private UndeadTreatment() {}

    public static boolean applies(Entity entity) {
        return entity instanceof Player p && SpeciesLayer.treatAsUndead(p);
    }
}
