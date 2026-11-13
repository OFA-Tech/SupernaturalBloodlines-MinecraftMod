package net.ofatech.supernaturalbloodlines.parts.artifacts.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;

/**
 * A line of mountain ash (R95), laid like redstone dust and carrying no power. Walkers that are {@link Supernatural#isPrey}
 * (vampire and werewolf types; witches and siphoners pass) meet a wall 1.5 blocks high; everyone else walks through, and so
 * do their arrows... except that a projectile whose owner is prey hits the wall. A line laid by a pack Emissary (the block
 * entity flag) also stops the mobs in {@code #supernaturalbloodlines:supernatural_mobs}.
 */
public class MountainAshLineBlock extends BaseEntityBlock {
    public static final MapCodec<MountainAshLineBlock> CODEC = simpleCodec(MountainAshLineBlock::new);
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape WALL = Shapes.box(0, 0, 0, 1, 1.5, 1);

    public MountainAshLineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MountainAshLineBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    /** Who this line stops. A projectile counts as its owner. */
    public static boolean blocks(@Nullable Entity entity, boolean emissary) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Projectile p) {
            return p.getOwner() != null && Supernatural.isPrey(p.getOwner());
        }
        return Supernatural.isPrey(entity) || (emissary && entity.getType().is(MountainAsh.SUPERNATURAL_MOBS));
    }

    private static boolean isEmissary(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MountainAshLineBlockEntity line && line.isEmissary();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // an unknown walker (no entity) and non-entity contexts pass; the emissary flag is only read once someone might be stopped
        return ctx instanceof EntityCollisionContext e && e.getEntity() != null && blocks(e.getEntity(), isEmissary(level, pos))
                ? WALL : Shapes.empty();
    }

    /** Mobs path around the line instead of walking into it. */
    @Nullable
    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return mob != null && blocks(mob, isEmissary(level, pos)) ? PathType.BLOCKED : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
}
