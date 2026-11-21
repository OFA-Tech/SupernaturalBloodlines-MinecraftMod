package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;

/**
 * What drinking an Original's blood does, for every drinkable blood that names a source (Original's Blood, Klaus's Blood): the source is remembered as
 * the drinker's <em>pending</em> sire for as long as the {@code original_blood} effect lasts, and the drinker is told so. Nobody is sired by a drink alone: the
 * source becomes the sire (and the line root, hidden until discovered) only when the blood leads to a turning, at the moment it completes: the hybrid transition
 * ({@code TransitionHandler}, {@code pendingSire}) or the vampire turning ({@code TurningHandler}). A human who drank and never turned has no sire and no line,
 * and nothing is said when the effect simply runs out.
 */
public final class BloodDrink {
    public static final String DRUNK = "supernaturalbloodlines.blood.drunk";

    private BloodDrink() {
    }

    public static void drank(ServerPlayer player, UUID source) {
        player.setData(SpeciesRegistries.BLOOD_SOURCE, source);
        player.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
        player.sendSystemMessage(Component.translatable(DRUNK, Lines.rootName(player.server, source), OriginalBloodEffect.DURATION_TICKS / 1200));
    }
}
