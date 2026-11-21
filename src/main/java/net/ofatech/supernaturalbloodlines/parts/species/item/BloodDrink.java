package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;

/**
 * What drinking an Original's blood does, for every drinkable blood that names a source (Original's Blood, Klaus's Blood): the source is
 * remembered for the transition, the {@code original_blood} effect starts, and the source becomes the drinker's sire (so their line is the
 * source's line). The first sire wins, and an Original is never sired. The line stays hidden until the drinker discovers it ({@code knownLine}).
 */
public final class BloodDrink {
    private BloodDrink() {
    }

    public static void drank(ServerPlayer player, UUID source) {
        player.setData(SpeciesRegistries.BLOOD_SOURCE, source);
        player.addEffect(new MobEffectInstance(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, OriginalBloodEffect.DURATION_TICKS));
        if (!OriginalRules.isOriginal(BloodlineAccess.get(player).species())) {
            SireRecorder.record(source, SireRecorder.nameOfId(player.server, source), player);
        }
    }
}
