package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.BoundRingItem;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.ResurrectionRingItem;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.VervainBlock;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.VervainTeaItem;

/** Everything the artifacts part registers. */
public final class ArtifactItems {
    /** Ten minutes: how long the tea gives the {@code vervain} effect. */
    public static final int VERVAIN_TICKS = 10 * 60 * 20;
    public static final int RING_CHARGES = 3;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalBloodlines.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SupernaturalBloodlines.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SupernaturalBloodlines.MODID);

    /** The player a ring belongs to. A ring without it is unbound and does nothing. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOUND_TO =
            COMPONENTS.registerComponentType("bound_to", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    // built eagerly: the ring's default charges are an Item.Properties value, and the item registry can fill before the component registry
    private static final DataComponentType<Integer> CHARGES_TYPE = DataComponentType.<Integer>builder()
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build();
    /** Uses a Resurrection Ring has left. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGES = COMPONENTS.register("charges", () -> CHARGES_TYPE);

    public static final DeferredItem<BoundRingItem> SUN_RING = ITEMS.register("sun_ring", BoundRingItem::new);

    /** Null without Werewolves: the item is not registered then. */
    public static final DeferredItem<BoundRingItem> MOON_RING = ModList.get().isLoaded("werewolves")
            ? ITEMS.register("moon_ring", BoundRingItem::new)
            : null;

    public static final DeferredItem<ResurrectionRingItem> RESURRECTION_RING =
            ITEMS.register("resurrection_ring", () -> new ResurrectionRingItem(CHARGES_TYPE, RING_CHARGES));

    // ---- vervain ----

    public static final DeferredHolder<MobEffect, MobEffect> VERVAIN_EFFECT =
            EFFECTS.register("vervain", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x5C8A3C) {
            });

    public static final DeferredBlock<VervainBlock> VERVAIN_CROP =
            BLOCKS.register("vervain", () -> new VervainBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<ItemNameBlockItem> VERVAIN_SEEDS =
            ITEMS.register("vervain_seeds", () -> new ItemNameBlockItem(VERVAIN_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> VERVAIN = ITEMS.register("vervain", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build())));
    public static final DeferredItem<VervainTeaItem> VERVAIN_TEA = ITEMS.register("vervain_tea", () -> new VervainTeaItem(new Item.Properties()
            .stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(VERVAIN_EFFECT, VERVAIN_TICKS), 1.0f)
                    .usingConvertsTo(Items.GLASS_BOTTLE).build())));

    private ArtifactItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        EFFECTS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(SUN_RING);
                if (MOON_RING != null) {
                    e.accept(MOON_RING);
                }
                e.accept(RESURRECTION_RING);
            } else if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                e.accept(VERVAIN_SEEDS);
            } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(VERVAIN);
            } else if (e.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
                e.accept(VERVAIN_TEA);
            }
        });
    }

    /** Is a copy of {@code item} bound to this player worn (Curios slot or inventory)? Another player's ring does not hide their own. */
    public static boolean isBoundWorn(Player p, Item item) {
        return ArtifactSlots.findWorn(p, item, s -> isBoundTo(s, p.getUUID())).isPresent();
    }

    public static boolean isBoundTo(ItemStack stack, UUID id) {
        return id.equals(stack.get(BOUND_TO.get()));
    }
}
