package net.ofatech.supernaturalbloodlines.mixin.speciestensura;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ManasRaceInstance.class)
public abstract class RaceInstanceMixin {
    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernaturalbloodlines$offerApex(LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        cir.setReturnValue(ApexOffer.withApex((ManasRaceInstance) (Object) this, entity, cir.getReturnValue()));
    }
}
