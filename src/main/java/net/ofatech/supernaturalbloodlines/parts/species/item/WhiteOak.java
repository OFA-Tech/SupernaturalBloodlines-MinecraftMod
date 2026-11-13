package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * White oak: a log, planks and sapling that grow a vanilla-shaped oak with our log (the tree is data:
 * {@code worldgen/configured_feature/white_oak.json}), and the stake. Anything in the item tag {@link #TAG}, held in
 * the attacker's main hand, kills an Original outright (R32).
 */
public final class WhiteOak {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalBloodlines.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    public static final TagKey<Item> TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "white_oak"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "white_oak"));

    private static final TreeGrower GROWER = new TreeGrower(SupernaturalBloodlines.MODID + ":white_oak",
            Optional.empty(), Optional.of(TREE), Optional.empty());

    public static final DeferredBlock<RotatedPillarBlock> LOG = BLOCKS.register("white_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> PLANKS = BLOCKS.register("white_oak_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.register("white_oak_sapling",
            () -> new SaplingBlock(GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerSimpleBlockItem(LOG);
    public static final DeferredItem<BlockItem> PLANKS_ITEM = ITEMS.registerSimpleBlockItem(PLANKS);
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerSimpleBlockItem(SAPLING);
    public static final DeferredItem<SwordItem> STAKE = ITEMS.register("white_oak_stake",
            () -> new SwordItem(Tiers.WOOD, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F))));

    private WhiteOak() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                e.accept(LOG_ITEM);
                e.accept(PLANKS_ITEM);
            } else if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                e.accept(LOG_ITEM);
                e.accept(SAPLING_ITEM);
            } else if (e.getTabKey() == CreativeModeTabs.COMBAT) {
                e.accept(STAKE);
            }
        });
    }
}
