package net.ofatech.supernaturalbloodlines.mixin.speciestensura;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.UndeadTreatment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RaceUtils.class)
public abstract class RaceUtilsMixin {
    @Inject(method = "isUndead", at = @At("HEAD"), cancellable = true, require = 0)
    private static void supernaturalbloodlines$layerIsUndead(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (UndeadTreatment.applies(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isAffectedByHolyExposure", at = @At("HEAD"), cancellable = true, require = 0)
    private static void supernaturalbloodlines$layerIsAffectedByHoly(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (UndeadTreatment.applies(entity)) {
            cir.setReturnValue(true);
        }
    }

    /** An apex race (our `bitable` tag) has blood even though Tensura's `no_blood` race tag includes the undead ones: its Drain, Blood Mist and VampireRace feeding must reach them. */
    @Inject(method = "isBloodless", at = @At("HEAD"), cancellable = true, require = 0)
    private static void supernaturalbloodlines$bitableIsNotBloodless(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living && RaceTags.isBitable(RaceAPI.getRaceFrom(living).getRace().orElse(null))) {
            cir.setReturnValue(false);
        }
    }
}
