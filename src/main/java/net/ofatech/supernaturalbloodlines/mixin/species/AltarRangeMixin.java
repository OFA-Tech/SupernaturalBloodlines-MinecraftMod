package net.ofatech.supernaturalbloodlines.mixin.species;

import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Altar range x1.5 while a witch stands at the altar. Enchanted reads {@code altarRange} in the three places that matter:
 * the observer created on the first tick, the block scan and {@code posWithinRange} (which blocks the observer reports).
 */
@Mixin(AltarBlockEntity.class)
public abstract class AltarRangeMixin {
    @Redirect(method = {"firstTick", "setupBlockData", "posWithinRange"},
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;"),
            require = 0)
    private Object supernaturalbloodlines$range(ModConfigSpec.IntValue range) {
        AltarBlockEntity self = (AltarBlockEntity) (Object) this;
        return WitchAltarBonus.rangeFor(self.getLevel(), self.getBlockPos(), range.get());
    }
}
