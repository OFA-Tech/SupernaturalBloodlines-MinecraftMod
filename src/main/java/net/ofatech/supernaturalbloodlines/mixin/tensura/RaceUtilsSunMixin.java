package net.ofatech.supernaturalbloodlines.mixin.tensura;

import io.github.manasmods.tensura.race.RaceUtils;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSunRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RaceUtils.class)
public abstract class RaceUtilsSunMixin {
    /** The Weakness, Slowness and Mining Fatigue in VampireRace.onTick (and the bat form check) hang off this, not off shouldBurn. */
    @Inject(method = "isUnderSun", at = @At("HEAD"), cancellable = true, require = 0)
    private static void supernaturalbloodlines$vampirismOwnsTheSun(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VampireSunRule.isFactionVampire(entity)) {
            cir.setReturnValue(false);
        }
    }
}
