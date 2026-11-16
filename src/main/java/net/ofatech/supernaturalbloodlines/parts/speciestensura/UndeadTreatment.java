package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.tensura.race.RaceUtils;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.world.entity.Entity;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesTensuraMixinPlugin;

/**
 * Called from {@code mixin.speciestensura.RaceUtilsMixin}: Tensura has no event or switch for its undead and
 * holy-exposure checks (docs/parts/species_tensura.md), so the mixin asks here. Switch: {@code undead_treatment_enabled}.
 */
public final class UndeadTreatment {
    private UndeadTreatment() {}

    /**
     * The mixin has been applied to Tensura's RaceUtils, so its undead answers really come from here. Loading the class
     * is what applies the mixin, so it is loaded first (it has not been when nothing asked Tensura about undead yet).
     */
    public static boolean mixinApplied() {
        return RaceUtils.class != null && SpeciesTensuraMixinPlugin.raceUtilsHooked();
    }

    /**
     * The switch is off and the entity is one of our apex races, which sit in {@code tensura:undead} through a data file that cannot be made conditional:
     * Tensura's undead and holy-exposure answers are then "no".
     */
    public static boolean apexSuppressed(Entity entity) {
        return !SpeciesTensuraConfig.undeadTreatment() && entity instanceof net.minecraft.world.entity.LivingEntity living
                && RaceAPI.getRaceFrom(living).getRace().filter(r -> SupernaturalBloodlines.MODID.equals(r.getRaceId().getNamespace()) && r.is(RaceTags.UNDEAD)).isPresent();
    }

    public static boolean applies(Entity entity) {
        return entity instanceof Player p && SpeciesLayer.treatAsUndead(p);
    }
}
