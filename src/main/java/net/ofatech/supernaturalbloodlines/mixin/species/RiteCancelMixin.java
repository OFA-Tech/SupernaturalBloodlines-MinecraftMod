package net.ofatech.supernaturalbloodlines.mixin.species;

import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A rite that cancels itself in {@code onStart} (wrong target, refused by the rules) gives the siphoner's power back. */
@Mixin(Rite.class)
public abstract class RiteCancelMixin {
    @Inject(method = "cancel", at = @At("HEAD"), require = 0)
    private void supernaturalbloodlines$cancelled(CallbackInfoReturnable<Boolean> cir) {
        RiteCost.discard();
    }
}
