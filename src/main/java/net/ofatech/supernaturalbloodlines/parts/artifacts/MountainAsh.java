package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock;
import net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity;

/**
 * Mountain ash (R95): a tree (sapling, log, leaves, planks; a rare tree in forests through the biome modifier
 * {@code mountain_ash_trees}), {@code mountain_ash_dust} cut from its logs, and the line the dust lays: it stops
 * vampires and werewolves (see {@link MountainAshLineBlock}).
 */
public final class MountainAsh {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalBloodlines.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SupernaturalBloodlines.MODID);

    public static final ResourceKey<ConfiguredFeature<?, ?>> TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "mountain_ash"));
    /** Mobs the emissary-strengthened line also stops (data: {@code tags/entity_type/supernatural_mobs.json}, plan 9 fills it). */
    public static final TagKey<EntityType<?>> SUPERNATURAL_MOBS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "supernatural_mobs"));

    private static final TreeGrower GROWER = new TreeGrower(SupernaturalBloodlines.MODID + ":mountain_ash",
            Optional.empty(), Optional.of(TREE), Optional.empty());

    public static final DeferredBlock<RotatedPillarBlock> LOG = BLOCKS.register("mountain_ash_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> PLANKS = BLOCKS.register("mountain_ash_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<LeavesBlock> LEAVES = BLOCKS.register("mountain_ash_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.register("mountain_ash_sapling",
            () -> new SaplingBlock(GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    /** Hardness is small but not 0: a 0 would make the destroy progress a division by zero, and "speed 0" must mean "never". */
    public static final DeferredBlock<MountainAshLineBlock> LINE = BLOCKS.register("mountain_ash_line",
            () -> new MountainAshLineBlock(BlockBehaviour.Properties.of().noOcclusion().pushReaction(PushReaction.DESTROY)
                    .strength(0.05f).sound(SoundType.SAND)));

    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerSimpleBlockItem(LOG);
    public static final DeferredItem<BlockItem> PLANKS_ITEM = ITEMS.registerSimpleBlockItem(PLANKS);
    public static final DeferredItem<BlockItem> LEAVES_ITEM = ITEMS.registerSimpleBlockItem(LEAVES);
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerSimpleBlockItem(SAPLING);
    /** Placed like redstone dust: a click on the top of a solid block lays a line. */
    public static final DeferredItem<ItemNameBlockItem> DUST = ITEMS.register("mountain_ash_dust",
            () -> new ItemNameBlockItem(LINE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MountainAshLineBlockEntity>> LINE_ENTITY =
            BLOCK_ENTITIES.register("mountain_ash_line", () -> BlockEntityType.Builder.of(MountainAshLineBlockEntity::new, LINE.get()).build(null));

    private MountainAsh() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                e.accept(LOG_ITEM);
                e.accept(PLANKS_ITEM);
            } else if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                e.accept(LOG_ITEM);
                e.accept(LEAVES_ITEM);
                e.accept(SAPLING_ITEM);
            } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(DUST);
            }
        });
        MountainAshRules.register();
    }
}
