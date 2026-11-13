package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import io.github.manasmods.manascore.race.api.ManasRace;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.LayerSpec;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;

/**
 * R100: a species gives a bonus to the Tensura evolution requirements of the current line. Tensura has no hook per
 * requirement (they are read live from the player's state), so the progress it reports is scaled instead: called from the
 * same mixin as {@link ApexOffer}, at the end of {@code ManasRaceInstance.getEvolutionProgress}.
 */
public final class EvolutionBoost {
    private EvolutionBoost() {}

    /** The progress (0 to 100) times the multiplier, never above 100 and never below what Tensura reported. */
    public static float boost(float progress, double multiplier) {
        return Math.max(progress, Math.min(100.0F, (float) (progress * multiplier)));
    }

    public static float boosted(LivingEntity entity, ManasRace target, float progress) {
        if (!(entity instanceof Player p) || target == null || BloodlineAccess.get(p).species() == Species.NONE
                || SpeciesLayer.spec(p).equals(LayerSpec.NONE)) {
            return progress;
        }
        return boost(progress, SpeciesTensuraConfig.evolutionMultiplier());
    }
}
