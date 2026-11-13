package net.ofatech.supernaturalbloodlines.mixin.species;

import java.util.UUID;
import net.favouriteless.enchanted.api.altar.PowerConsumer;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ofatech.supernaturalbloodlines.parts.species.witch.ChalkCaster;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The rite power cost: x0.75 for a witch, and for the start cost siphoned power first (see {@link RiteCost}). Both places
 * Enchanted charges the altar: the start cost in {@code serverTick} (a cost of 0 skips the altar completely, which is how
 * a siphoner starts a rite with no altar) and the per-tick cost in {@code tryConsumePower} (discount only). The siphoned
 * power is taken after {@code Rite.start()}, unless the rite cancelled itself ({@link RiteCancelMixin}).
 */
@Mixin(GoldChalkBlockEntity.class)
public abstract class RiteCostMixin implements ChalkCaster {
    @Shadow
    private UUID caster;

    @Shadow
    public abstract PowerConsumer.PowerPosHolder getPosHolder();

    @Override
    public UUID supernaturalbloodlines$caster() {
        return caster;
    }

    @ModifyVariable(method = "serverTick", at = @At("STORE"), ordinal = 0, require = 0)
    private static int supernaturalbloodlines$startCost(int power, Level level, BlockPos pos, BlockState state, GoldChalkBlockEntity chalk) {
        return RiteCost.begin(level, ((ChalkCaster) chalk).supernaturalbloodlines$caster(), pos, chalk.getPosHolder(), power, true);
    }

    @Inject(method = "serverTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/favouriteless/enchanted/common/enchanted/circle_magic/rites/Rite;start()V"), require = 0)
    private static void supernaturalbloodlines$started(Level level, BlockPos pos, BlockState state, GoldChalkBlockEntity chalk, CallbackInfo ci) {
        RiteCost.commit();
    }

    @Inject(method = "serverTick", at = @At("RETURN"), require = 0)
    private static void supernaturalbloodlines$startDone(Level level, BlockPos pos, BlockState state, GoldChalkBlockEntity chalk, CallbackInfo ci) {
        RiteCost.discard();
    }

    @ModifyVariable(method = "tryConsumePower", at = @At("HEAD"), argsOnly = true, require = 0)
    private int supernaturalbloodlines$tickCost(int power) {
        GoldChalkBlockEntity self = (GoldChalkBlockEntity) (Object) this;
        return RiteCost.begin(self.getLevel(), caster, self.getBlockPos(), getPosHolder(), power, false);
    }
}
