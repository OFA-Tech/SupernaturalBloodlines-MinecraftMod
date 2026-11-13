package net.ofatech.supernaturalbloodlines.parts.artifacts.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;
import net.ofatech.supernaturalbloodlines.shared.werewolves.EmissaryLines;

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

    /** A line laid by a pack Emissary (the species part says who that is) is the stronger one. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p && EmissaryLines.isEmissary(p)
                && level.getBlockEntity(pos) instanceof MountainAshLineBlockEntity line) {
            line.setEmissary(true);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    /** Who this line stops. A projectile counts as its owner, a mount as its passengers. */
    public static boolean blocks(@Nullable Entity entity, BlockGetter level, BlockPos pos) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Projectile p) {
            return p.getOwner() != null && Supernatural.isPrey(p.getOwner());
        }
        if (Supernatural.isPrey(entity)) {
            return true;
        }
        if (entity.isVehicle()) {
            for (Entity passenger : entity.getIndirectPassengers()) {
                if (Supernatural.isPrey(passenger)) {
                    return true;
                }
            }
        }
        // the block entity is only read for the few mobs an emissary line adds
        return entity.getType().is(MountainAsh.SUPERNATURAL_MOBS)
                && level.getBlockEntity(pos) instanceof MountainAshLineBlockEntity line && line.isEmissary();
    }

    /**
     * Vanilla caches {@code hasLargeCollisionShape} from this method called with {@link EmptyBlockGetter} and an empty
     * context, and {@code BlockCollisions} skips the cells in the one block margin of a mover's box unless that flag is
     * set. A line that answers "nothing" there is never seen by an entity whose feet are above the line's own cell (a
     * jump), so that one call gets the wall; it only feeds the cache (not a full block, so nothing suffocates).
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (level == EmptyBlockGetter.INSTANCE) {
            return WALL;
        }
        // an unknown walker (no entity) and non-entity contexts pass
        return ctx instanceof EntityCollisionContext e && blocks(e.getEntity(), level, pos) ? WALL : Shapes.empty();
    }

    /**
     * The support shape defaults to the collision shape, and the cached one (the {@code EmptyBlockGetter} call above) is the
     * wall, so without this the line would count as a sturdy full face: torches, more dust and spawns would sit on it.
     */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
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
