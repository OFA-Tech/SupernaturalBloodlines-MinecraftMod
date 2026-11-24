package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Where a circle is: the gold chalk a player stands at, and who stands inside it (the radius the Linking Spell uses). */
public final class CircleSites {
    /** Flat blocks from the centre of the chalk that count as inside: the medium circle is 11 wide. */
    public static final double RADIUS = 5.5;
    /** How far above or below the chalk a pair of feet may be. */
    public static final double HEIGHT = 2;

    private CircleSites() {
    }

    /** The nearest gold chalk within the radius of {@code feet}; found by scanning the 11 x 5 x 11 box once per button press, never per tick. */
    @Nullable
    public static GoldChalkBlockEntity chalkNear(ServerLevel level, BlockPos feet) {
        GoldChalkBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-5, -2, -5), feet.offset(5, 2, 5))) {
            if (level.getBlockEntity(p) instanceof GoldChalkBlockEntity chalk) {
                double d = p.distToCenterSqr(feet.getX() + 0.5, p.getY() + 0.5, feet.getZ() + 0.5);
                if (d <= RADIUS * RADIUS && d < bestDistance) {
                    best = chalk;
                    bestDistance = d;
                }
            }
        }
        return best;
    }

    public static boolean inside(BlockPos chalk, Entity e) {
        double dx = e.getX() - (chalk.getX() + 0.5);
        double dz = e.getZ() - (chalk.getZ() + 0.5);
        return dx * dx + dz * dz <= RADIUS * RADIUS && Math.abs(e.getY() - chalk.getY()) <= HEIGHT;
    }
}