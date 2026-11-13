package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;
import net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity;

/**
 * Mountain ash (R95). The line is at x 7 in a row (y 1, z 1); walkers start at x 4 and try to reach x 9. Vampires are
 * Vampirism's own faction, so the tests do not need the species part; the werewolf side is in {@link MountainAshWerewolfGameTests}.
 * They use the 16x10x16 template {@code tall}: the framework puts a barrier ceiling right above a template, so a 1 high
 * one traps anything taller than a block (a walking player or mob, a growing tree).
 */
@PrefixGameTestTemplate(false)
public final class MountainAshGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    static final BlockPos LINE = new BlockPos(7, 1, 1);

    private MountainAshGameTests() {
    }

    // ---- helpers ----

    /** Stone floor under the walkway and a fresh line (and nothing else) on it. */
    static BlockState lay(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
            }
        }
        helper.setBlock(LINE, MountainAsh.LINE.get());
        return helper.getBlockState(LINE);
    }

    @SuppressWarnings("unchecked")
    private static Mob spawn(GameTestHelper helper, String id, BlockPos at) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)), at);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.setPos(helper.absoluteVec(new Vec3(4.5, 1, 1.5)));
        return p;
    }

    private static ServerPlayer vampirePlayer(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    /** Walks {@code e} 5 blocks along +x from x 4.5 and returns where it ends, relative to the test (x). */
    static double walk(GameTestHelper helper, Entity e) {
        return walk(helper, e, 1);
    }

    /** Like {@link #walk(GameTestHelper, Entity)} with the feet at relative height {@code feetY}, in the air (a jump). */
    static double walk(GameTestHelper helper, Entity e, double feetY) {
        e.setPos(helper.absoluteVec(new Vec3(4.5, feetY, 1.5)));
        e.setOnGround(false);
        e.move(MoverType.SELF, new Vec3(5, 0, 0));
        return helper.relativeVec(e.position()).x;
    }

    static void assertStopped(GameTestHelper helper, Entity e, String what) {
        double x = walk(helper, e);
        helper.assertTrue(x < LINE.getX(), what + " crossed the line: ended at x " + x);
    }

    static void assertPasses(GameTestHelper helper, Entity e, String what) {
        double x = walk(helper, e);
        helper.assertTrue(x > LINE.getX() + 1, what + " was stopped by the line: ended at x " + x);
    }

    // ---- crossing (R95) ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void vampireCannotCrossAshLine(GameTestHelper helper) {
        lay(helper);
        assertStopped(helper, spawn(helper, "vampirism:vampire", new BlockPos(4, 1, 1)), "a vampire mob");
        assertStopped(helper, vampirePlayer(helper), "a vampire player");
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void humanWalksThrough(GameTestHelper helper) {
        lay(helper);
        assertPasses(helper, player(helper), "a human player");
        assertPasses(helper, helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 1)), "a villager");
        assertPasses(helper, helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 1)), "a zombie");
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void witchWalksThrough(GameTestHelper helper) {
        lay(helper);
        ServerPlayer witch = player(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        assertPasses(helper, witch, "a witch");
        helper.succeed();
    }

    /**
     * The line is at y 1; feet at 2.2 are in the air, 1.2 above it and in the margin row that BlockCollisions skips for a
     * block whose cached "large collision shape" flag is false.
     */
    @GameTest(template = "tall", templateNamespace = NS)
    public static void jumpingPreyCannotCrossAshLine(GameTestHelper helper) {
        BlockState line = lay(helper);
        helper.assertTrue(line.hasLargeCollisionShape(), "the cached large-shape flag is off, so jumpers are never checked against the line");
        helper.assertTrue(!line.isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(LINE)), "the line counts as a full block");
        helper.assertTrue(!line.isSuffocating(helper.getLevel(), helper.absolutePos(LINE)), "the line suffocates");
        // the cached shape is also what a caller without a context gets (WalkNodeEvaluator's floor, say), so check that
        // a zombie's pathfinding still finds a way across the line
        BlockPos goal = helper.absolutePos(new BlockPos(10, 1, 1));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 1));
        zombie.setOnGround(true);
        var zPath = zombie.getNavigation().createPath(goal, 0);
        helper.assertTrue(zPath != null && zPath.canReach(), "a zombie finds no way across the line: " + zPath);

        for (double feetY : new double[] {2.2, 2.45}) {
            double v = walk(helper, spawn(helper, "vampirism:vampire", new BlockPos(4, 1, 1)), feetY);
            helper.assertTrue(v < LINE.getX(), "a vampire mob jumping at " + feetY + " crossed the line: ended at x " + v);
            double p = walk(helper, vampirePlayer(helper), feetY);
            helper.assertTrue(p < LINE.getX(), "a vampire player jumping at " + feetY + " crossed the line: ended at x " + p);
        }
        // above the wall (top at 2.5) nothing stops them: the limit is the wall's height
        double over = walk(helper, vampirePlayer(helper), 2.6);
        helper.assertTrue(over > LINE.getX() + 1, "a vampire above the wall was stopped: ended at x " + over);

        // the others still jump over and through
        double human = walk(helper, player(helper), 2.2);
        helper.assertTrue(human > LINE.getX() + 1, "a jumping human was stopped: ended at x " + human);
        ServerPlayer witch = player(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        double w = walk(helper, witch, 2.2);
        helper.assertTrue(w > LINE.getX() + 1, "a jumping witch was stopped: ended at x " + w);
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void mountedPreyCannotCrossAshLine(GameTestHelper helper) {
        lay(helper);
        Mob pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 1));
        Mob vampire = spawn(helper, "vampirism:vampire", new BlockPos(4, 1, 1));
        helper.assertTrue(vampire.startRiding(pig, true), "setup: the vampire did not mount");
        assertStopped(helper, pig, "a pig carrying a vampire");
        vampire.stopRiding();

        Mob pig2 = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 1));
        Mob villager = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 1));
        helper.assertTrue(villager.startRiding(pig2, true), "setup: the villager did not mount");
        assertPasses(helper, pig2, "a pig carrying a villager");
        helper.succeed();
    }

    /** A hand-made check of the shape itself, for the entities whose movement is not worth simulating. */
    @GameTest(template = "tall", templateNamespace = NS)
    public static void lineShapeIsOneAndAHalfBlocksForPreyOnly(GameTestHelper helper) {
        BlockState line = lay(helper);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(LINE);
        var vampire = vampirePlayer(helper);
        var human = player(helper);
        var shape = line.getCollisionShape(level, abs, net.minecraft.world.phys.shapes.CollisionContext.of(vampire));
        helper.assertTrue(!shape.isEmpty() && shape.max(Direction.Axis.Y) == 1.5, "prey shape: " + shape);
        helper.assertTrue(line.getCollisionShape(level, abs, net.minecraft.world.phys.shapes.CollisionContext.of(human)).isEmpty(), "a human has a shape");
        helper.assertTrue(line.getCollisionShape(level, abs, net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty(), "an unknown walker has a shape");
        helper.assertTrue(!line.isSignalSource() && line.getSignal(level, abs, Direction.UP) == 0, "the line gives power");
        helper.succeed();
    }

    /** The line's cached wall shape must not read as a floor: nothing sits on it. */
    @GameTest(template = "tall", templateNamespace = NS)
    public static void lineIsNoSturdyFloor(GameTestHelper helper) {
        BlockState line = lay(helper);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(LINE);
        helper.assertTrue(!line.isFaceSturdy(level, abs, Direction.UP), "the top of the line is a sturdy face");
        helper.assertTrue(!line.isFaceSturdy(level, abs, Direction.UP, net.minecraft.world.level.block.SupportType.CENTER), "the line supports a centre");
        helper.assertTrue(line.getBlockSupportShape(level, abs).isEmpty(), "the line has a support shape");
        // control: stone is sturdy
        helper.assertTrue(helper.getBlockState(LINE.below()).isFaceSturdy(level, abs.below(), Direction.UP), "control: stone is not sturdy");

        // no line on a line, no torch on a line
        BlockPos above = LINE.above();
        helper.assertTrue(!MountainAsh.LINE.get().defaultBlockState().canSurvive(level, helper.absolutePos(above)), "a line can stand on a line");
        helper.assertTrue(!Blocks.TORCH.defaultBlockState().canSurvive(level, helper.absolutePos(above)), "a torch can stand on a line");
        helper.assertTrue(!Blocks.REDSTONE_WIRE.defaultBlockState().canSurvive(level, helper.absolutePos(above)), "redstone dust can lie on a line");
        helper.assertTrue(Blocks.TORCH.defaultBlockState().canSurvive(level, helper.absolutePos(LINE.west())), "control: a torch cannot stand on stone");

        // and dust used on top of a line places nothing
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MountainAsh.DUST.get(), 4));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        p.getMainHandItem().useOn(new UseOnContext(level, p, InteractionHand.MAIN_HAND, p.getMainHandItem(), hit));
        helper.assertTrue(helper.getBlockState(above).isAir(), "dust was laid on top of a line: " + helper.getBlockState(above));

        // nothing spawns on it
        helper.assertTrue(net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, helper.absolutePos(LINE.west()), EntityType.ZOMBIE),
                "control: nothing spawns on stone");
        helper.assertTrue(!net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, helper.absolutePos(above), EntityType.ZOMBIE),
                "a mob can spawn on a line");
        helper.succeed();
    }

    // ---- breaking ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void supernaturalCannotBreakLine(GameTestHelper helper) {
        BlockState line = lay(helper);
        BlockPos abs = helper.absolutePos(LINE);
        ServerPlayer vampire = vampirePlayer(helper);
        ServerPlayer human = player(helper);

        var humanSpeed = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(human, line, 1.0f, abs));
        helper.assertTrue(humanSpeed.getNewSpeed() > 0, "a human cannot mine the line: " + humanSpeed.getNewSpeed());
        var vampSpeed = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(vampire, line, 1.0f, abs));
        helper.assertTrue(vampSpeed.getNewSpeed() == 0 || vampSpeed.isCanceled(), "a vampire mines the line at " + vampSpeed.getNewSpeed());

        // other blocks are none of its business
        var stone = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(vampire, Blocks.STONE.defaultBlockState(), 1.0f, abs.below()));
        helper.assertTrue(stone.getNewSpeed() == 1.0f && !stone.isCanceled(), "the rule touched another block: " + stone.getNewSpeed());

        // and a break that skips the speed (instant mining) is cancelled too
        helper.assertTrue(!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), abs, line, human)).isCanceled(), "a human's break was cancelled");
        helper.assertTrue(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), abs, line, vampire)).isCanceled(), "a vampire's break went through");
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void lineDropsDustAndDustPlacesLine(GameTestHelper helper) {
        BlockState line = lay(helper);
        BlockPos abs = helper.absolutePos(LINE);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(line, helper.getLevel(), abs, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(MountainAsh.DUST.get()), "the line drops " + drops);

        // dust on top of stone lays a line, like redstone dust
        ServerPlayer p = player(helper);
        BlockPos floor = new BlockPos(2, 0, 1);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MountainAsh.DUST.get(), 4));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(floor)).add(0, 0.5, 0), Direction.UP, helper.absolutePos(floor), false);
        InteractionResult result = p.getMainHandItem().useOn(new UseOnContext(helper.getLevel(), p, InteractionHand.MAIN_HAND, p.getMainHandItem(), hit));
        helper.assertTrue(result.consumesAction(), "the dust was not placed: " + result);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 1)).is(MountainAsh.LINE.get()), "no line at the click: " + helper.getBlockState(new BlockPos(2, 1, 1)));
        helper.assertTrue(p.getMainHandItem().getCount() == 3, "the dust was not used up: " + p.getMainHandItem());

        // it cannot be laid in the air
        helper.setBlock(new BlockPos(2, 0, 1), Blocks.AIR);
        helper.assertTrue(!MountainAsh.LINE.get().defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1))),
                "a line can hang in the air");
        helper.succeed();
    }

    // ---- projectiles ----

    private static Arrow arrow(GameTestHelper helper, Entity owner) {
        Arrow arrow = EntityType.ARROW.create(helper.getLevel());
        arrow.setOwner(owner);
        arrow.setPos(helper.absoluteVec(new Vec3(5.5, 1.5, 1.5)));
        return arrow;
    }

    private static BlockHitResult fly(GameTestHelper helper, Projectile p) {
        Vec3 from = helper.absoluteVec(new Vec3(5.5, 1.5, 1.5));
        Vec3 to = helper.absoluteVec(new Vec3(10.5, 1.5, 1.5));
        return helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void supernaturalProjectileStopped(GameTestHelper helper) {
        lay(helper);
        BlockPos abs = helper.absolutePos(LINE);

        Arrow fromVampire = arrow(helper, vampirePlayer(helper));
        BlockHitResult hit = fly(helper, fromVampire);
        helper.assertTrue(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(abs), "a vampire's arrow flew through: " + hit);
        // an arrow (or trident) sticks into the wall like into any block: the impact is left alone, not deleted
        var impact = NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(fromVampire, hit));
        helper.assertTrue(!fromVampire.isRemoved() && !impact.isCanceled(), "a vampire's arrow was deleted instead of sticking");
        var trident = EntityType.TRIDENT.create(helper.getLevel());
        trident.setOwner(vampirePlayer(helper));
        var tridentImpact = NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(trident, hit));
        helper.assertTrue(!trident.isRemoved() && !tridentImpact.isCanceled(), "a vampire's trident was deleted instead of sticking");
        // anything else (a snowball, a fireball) vanishes on contact
        var snowball = EntityType.SNOWBALL.create(helper.getLevel());
        snowball.setOwner(vampirePlayer(helper));
        var ballImpact = NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(snowball, hit));
        helper.assertTrue(snowball.isRemoved() && ballImpact.isCanceled(), "a vampire's snowball was not discarded on contact");

        Arrow fromHuman = arrow(helper, player(helper));
        BlockHitResult miss = fly(helper, fromHuman);
        helper.assertTrue(miss.getType() == net.minecraft.world.phys.HitResult.Type.MISS, "a human's arrow hit the line: " + miss);
        helper.assertTrue(!fromHuman.isRemoved(), "a human's arrow was discarded");

        // a vampire's arrow that hits something else is not the line's business
        Arrow other = arrow(helper, vampirePlayer(helper));
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(other, new BlockHitResult(Vec3.atCenterOf(abs.below()), Direction.UP, abs.below(), false)));
        helper.assertTrue(!other.isRemoved(), "an arrow that hit stone was discarded");
        helper.succeed();
    }

    // ---- emissary line and pathfinding ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void emissaryLineAlsoBlocksSupernaturalMobs(GameTestHelper helper) {
        lay(helper);
        Entity ghost = spawn(helper, "vampirism:ghost", new BlockPos(4, 1, 1));
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(LINE)) instanceof MountainAshLineBlockEntity be && !be.isEmissary(),
                "a new line is an emissary line");
        assertPasses(helper, ghost, "a ghost on a plain line");

        MountainAshLineBlockEntity be = (MountainAshLineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(LINE));
        be.setEmissary(true);
        assertStopped(helper, ghost, "a ghost on an emissary line");
        assertPasses(helper, helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 1)), "a zombie on an emissary line");
        assertPasses(helper, player(helper), "a human on an emissary line");

        // the flag is saved with the block
        var tag = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        MountainAshLineBlockEntity copy = new MountainAshLineBlockEntity(be.getBlockPos(), be.getBlockState());
        helper.assertTrue(!copy.isEmissary(), "setup: a new entity is an emissary");
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(copy.isEmissary(), "the emissary flag was not saved");
        helper.succeed();
    }

    // ---- the tree and the data ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void mountainAshDataLoads(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var recipes = helper.getLevel().getRecipeManager();
        for (String name : List.of("mountain_ash_planks", "mountain_ash_dust")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, name)).isPresent(), "recipe " + name + " is missing");
        }
        var recipe = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, "mountain_ash_dust")).orElseThrow().value();
        helper.assertTrue(recipe instanceof net.minecraft.world.item.crafting.StonecutterRecipe, "the dust recipe is not a stonecutter recipe: " + recipe);
        helper.assertTrue(recipe.getResultItem(access).is(MountainAsh.DUST.get()), "the dust recipe makes " + recipe.getResultItem(access));

        helper.assertTrue(access.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(MountainAsh.TREE), "no mountain_ash configured feature");
        var placedKey = net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(NS, "mountain_ash"));
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE).getHolder(placedKey);
        helper.assertTrue(placed.isPresent(), "no mountain_ash placed feature");

        // the biome modifier put it into the forest
        var forest = access.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST);
        boolean inForest = forest.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).stream()
                .anyMatch(h -> h.is(placedKey));
        helper.assertTrue(inForest, "mountain ash is not generated in the forest");
        var desert = access.registryOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
        helper.assertTrue(desert.getGenerationSettings().features().get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).stream().noneMatch(h -> h.is(placedKey)),
                "mountain ash is generated in the desert");

        // tags
        helper.assertTrue(MountainAsh.LOG.get().defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS), "the log is not a log");
        helper.assertTrue(MountainAsh.PLANKS.get().defaultBlockState().is(net.minecraft.tags.BlockTags.PLANKS), "the planks are not planks");
        helper.assertTrue(MountainAsh.LEAVES.get().defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES), "the leaves are not leaves");
        helper.assertTrue(MountainAsh.SAPLING.get().defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS), "the sapling is not a sapling");
        helper.assertTrue(MountainAsh.LOG.get().defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE), "the log is not axe-mineable");
        for (var block : List.of(MountainAsh.LOG.get(), MountainAsh.PLANKS.get(), MountainAsh.SAPLING.get())) {
            List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(), helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()), block + " drops " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void saplingGrowsMountainAshTree(GameTestHelper helper) {
        BlockPos base = new BlockPos(8, 1, 8);
        helper.setBlock(base.below(), Blocks.DIRT);
        helper.setBlock(base, MountainAsh.SAPLING.get());
        var level = helper.getLevel();
        var sapling = MountainAsh.SAPLING.get();
        sapling.advanceTree(level, helper.absolutePos(base), helper.getBlockState(base).setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1), level.getRandom());
        helper.assertTrue(helper.getBlockState(base).is(MountainAsh.LOG.get()), "the sapling did not grow a mountain ash log: " + helper.getBlockState(base));
        helper.assertTrue(helper.getBlockState(base.above(3)).is(MountainAsh.LOG.get()), "the trunk is short");
        boolean leaves = false;
        for (int y = 3; y < 9; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                leaves |= helper.getBlockState(base.offset(dx, y, 0)).is(MountainAsh.LEAVES.get());
            }
        }
        helper.assertTrue(leaves, "the tree has no mountain ash leaves");
        helper.succeed();
    }
}
