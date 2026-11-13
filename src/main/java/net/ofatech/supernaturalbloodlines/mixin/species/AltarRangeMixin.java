package net.ofatech.supernaturalbloodlines.mixin.species;

import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Altar range x1.5 when a witch is at the altar as it loads. Enchanted reads {@code altarRange} in the three places that
 * matter: the observer created on the first tick, the block scan and {@code posWithinRange} (which blocks the observer
 * reports). The three must agree or the altar's block count drifts (a block added while the range was wide, removed
 * while it was narrow), so the range is decided on the first read and kept until the altar reloads.
 */
@Mixin(AltarBlockEntity.class)
public abstract class AltarRangeMixin {
    @Unique
    private int supernaturalbloodlines$range = -1;

    @Redirect(method = {"firstTick", "setupBlockData", "posWithinRange"},
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;"),
            require = 0)
    private Object supernaturalbloodlines$range(ModConfigSpec.IntValue range) {
        if (supernaturalbloodlines$range >= 0) {
            return supernaturalbloodlines$range;
        }
        AltarBlockEntity self = (AltarBlockEntity) (Object) this;
        int decided = WitchAltarBonus.rangeFor(self.getLevel(), self.getBlockPos(), range.get());
        if (self.getLevel() != null) { // a read before the altar has a level says nothing about who is near
            supernaturalbloodlines$range = decided;
        }
        return decided;
    }
}
