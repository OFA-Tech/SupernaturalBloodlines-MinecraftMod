package net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** Wolf lichen: a thin carpet that stands on any solid ground (worldgen puts it on stone in cold biomes). */
public class WolfLichenBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public WolfLichenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                     BlockPos neighbourPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
}
