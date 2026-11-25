package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import java.util.Optional;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** A rite that takes EP from the one it works on: a circle can split that price. */
public interface EpPriced {
    double epCost();

    /** The rite type's EP price, read from a throwaway rite ({@code RiteType.create} only builds the object); empty when the rite takes no EP. */
    static Optional<Double> priceOf(@Nullable RiteType type, ServerLevel level, BlockPos pos) {
        if (type == null) {
            return Optional.empty();
        }
        Rite probe = type.create(level, pos);
        return probe instanceof EpPriced priced ? Optional.of(priced.epCost()) : Optional.empty();
    }
}
