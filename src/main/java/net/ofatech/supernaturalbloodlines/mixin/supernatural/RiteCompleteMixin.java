package net.ofatech.supernaturalbloodlines.mixin.supernatural;

import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.RiteSignal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The one "a rite worked" signal for every rite, Enchanted's own types and ours. Enchanted fires no event for it. In
 * {@code Rite.start()} the call {@code stop()} is reached only when {@code onStart} returned false, which means the rite is
 * finished; a rite that refuses calls {@code cancel()} first and then returns false too, so {@code cancel()} marks the instance and
 * the signal is sent only for an instance that was not cancelled. A rite that keeps running ({@code onStart} true) and ends later
 * in {@code tick()} is not signalled: none of the rites the ladders ask for runs over time.
 */
@Mixin(Rite.class)
public abstract class RiteCompleteMixin {
    @Shadow
    private Rite.RiteParams params;

    @Unique
    private boolean supernaturalbloodlines$cancelled;

    @Inject(method = "cancel", at = @At("HEAD"), require = 0)
    private void supernaturalbloodlines$riteCancelled(CallbackInfoReturnable<Boolean> cir) {
        supernaturalbloodlines$cancelled = true;
    }

    @Inject(method = "start", at = @At(value = "INVOKE",
            target = "Lnet/favouriteless/enchanted/common/enchanted/circle_magic/rites/Rite;stop()Z"), require = 0)
    private void supernaturalbloodlines$riteCompleted(CallbackInfo ci) {
        if (!supernaturalbloodlines$cancelled) {
            RiteSignal.completed((Rite) (Object) this, params);
        }
    }
}
