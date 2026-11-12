package net.ofatech.supernaturalbloodlines.mixin.tensura;

import io.github.manasmods.tensura.race.vampire.VampireRace;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSunRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VampireRace.class)
public abstract class VampireRaceSunMixin {
    /** Tensura has no config switch for its burn; Vampirism's sun damage handles faction vampires instead. */
    @Inject(method = "shouldBurn", at = @At("HEAD"), cancellable = true, require = 0)
    private static void supernaturalbloodlines$noDoubleBurn(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (VampireSunRule.isFactionVampire(entity)) {
            cir.setReturnValue(false);
        }
    }
}
