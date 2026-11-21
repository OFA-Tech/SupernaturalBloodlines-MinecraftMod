package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * What drinking an Original's blood does, for every drinkable blood that names a source (Original's Blood, Klaus's Blood): the source is remembered as
 * the drinker's <em>pending</em> sire for as long as the {@code original_blood} effect lasts. Nobody is sired by a drink alone: the source becomes the sire
 * (and the line root, hidden until discovered) only when the blood leads to a turning, at the moment it completes: the hybrid transition
 * ({@code TransitionHandler}, {@code pendingSire}) or the vampire turning ({@code TurningHandler}). A human who drank and never turned has no sire and no line.
 */
public final class BloodDrink {
    private BloodDrink() {
    }

    public static void drank(ServerPlayer player, UUID source) {
        player.setData(SpeciesRegistries.BLOOD_SOURCE, source);
        player.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
    }
}
