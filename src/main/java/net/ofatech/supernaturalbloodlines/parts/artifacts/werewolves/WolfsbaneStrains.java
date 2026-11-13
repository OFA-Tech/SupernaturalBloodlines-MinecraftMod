package net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.artifacts.VampirismSupernatural;

/**
 * R95: the wolfsbane strains and their effects on werewolf types. Needs Werewolves: nothing here is registered without
 * it ({@code ArtifactsPart} calls {@link #register} behind {@code isLoaded("werewolves")}). The common (purple) strain
 * is the existing Werewolves and Enchanted plant; it has no effect or ash of its own. The rules live in
 * {@link WolfsbaneRules}.
 */
public final class WolfsbaneStrains {
    /** Hits a coated weapon carries. */
    public static final int COATING_HITS = 8;
    public static final ResourceKey<DamageType> POISON = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "wolfsbane_poison"));
    /** Every common wolfsbane: Werewolves' and Enchanted's plant (the tag entries are optional). */
    public static final TagKey<Item> COMMON_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "wolfsbane/common"));
    /** The two plants of {@link #COMMON_TAG}, by id, to make them edible (tags are not loaded when default components are built). */
    public static final List<ResourceLocation> COMMON_PLANTS = List.of(
            ResourceLocation.parse("werewolves:wolfsbane"), ResourceLocation.parse("enchanted:wolfsbane_flower"));

    public enum Strain implements StringRepresentable {
        COMMON("common"), YELLOW("yellow"), NORTHERN_BLUE("northern_blue"), LICHEN("lichen");

        public static final Codec<Strain> CODEC = StringRepresentable.fromEnum(Strain::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Strain> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Strain::ordinal).cast();

        private final String id;

        Strain(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /** The effect it leaves, or null for the common strain (vanilla Weakness and Nausea). */
        public Holder<MobEffect> effect() {
            return switch (this) {
                case COMMON -> null;
                case YELLOW -> YELLOW_EFFECT;
                case NORTHERN_BLUE -> NORTHERN_BLUE_EFFECT;
                case LICHEN -> LICHEN_EFFECT;
            };
        }

        /** The ash that cures it, or null for the common strain. */
        public Item ash() {
            return switch (this) {
                case COMMON -> null;
                case YELLOW -> YELLOW_ASH.get();
                case NORTHERN_BLUE -> NORTHERN_BLUE_ASH.get();
                case LICHEN -> LICHEN_ASH.get();
            };
        }
    }

    /** A weapon's coating: the strain it applies and the hits it has left. */
    public record Coating(Strain strain, int hits) {
        public static final Codec<Coating> CODEC = RecordCodecBuilder.create(i -> i.group(
                Strain.CODEC.fieldOf("strain").forGetter(Coating::strain),
                ExtraCodecs.POSITIVE_INT.fieldOf("hits").forGetter(Coating::hits)).apply(i, Coating::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Coating> STREAM_CODEC =
                StreamCodec.composite(Strain.STREAM_CODEC, Coating::strain, ByteBufCodecs.VAR_INT, Coating::hits, Coating::new);
    }

    /** Pulses through {@link #pulse}; cures are only the strain's ash (milk and the like do nothing). */
    static final class WolfsbaneEffect extends MobEffect {
        private final Strain strain;

        WolfsbaneEffect(int colour, Strain strain) {
            super(MobEffectCategory.HARMFUL, colour);
            this.strain = strain;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return switch (strain) {
                case YELLOW -> duration % 40 == 0;
                case NORTHERN_BLUE -> duration % 100 == 0 || duration == 1;
                default -> false;
            };
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            MobEffectInstance instance = entity.getEffect(strain.effect());
            pulse(entity, strain, instance == null ? 100 : instance.getDuration());
            return true;
        }

        @Override
        public void fillEffectCures(java.util.Set<EffectCure> cures, MobEffectInstance effectInstance) {
        }
    }

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalBloodlines.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SupernaturalBloodlines.MODID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, SupernaturalBloodlines.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SupernaturalBloodlines.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Coating>> COATING =
            COMPONENTS.registerComponentType("wolfsbane_coating", b -> b.persistent(Coating.CODEC).networkSynchronized(Coating.STREAM_CODEC));

    public static final DeferredHolder<MobEffect, MobEffect> YELLOW_EFFECT =
            EFFECTS.register("wolfsbane_yellow", () -> new WolfsbaneEffect(0xD8C21C, Strain.YELLOW));
    public static final DeferredHolder<MobEffect, MobEffect> NORTHERN_BLUE_EFFECT =
            EFFECTS.register("wolfsbane_northern_blue", () -> new WolfsbaneEffect(0x2B4FB8, Strain.NORTHERN_BLUE));
    public static final DeferredHolder<MobEffect, MobEffect> LICHEN_EFFECT =
            EFFECTS.register("wolf_lichen", () -> new WolfsbaneEffect(0x6E8F5A, Strain.LICHEN));

    public static final DeferredHolder<Potion, Potion> YELLOW_POTION = POTIONS.register("wolfsbane_yellow",
            () -> new Potion("wolfsbane_yellow", new MobEffectInstance(YELLOW_EFFECT, 1200)));
    public static final DeferredHolder<Potion, Potion> NORTHERN_BLUE_POTION = POTIONS.register("wolfsbane_northern_blue",
            () -> new Potion("wolfsbane_northern_blue", new MobEffectInstance(NORTHERN_BLUE_EFFECT, 6000)));
    public static final DeferredHolder<Potion, Potion> LICHEN_POTION = POTIONS.register("wolf_lichen",
            () -> new Potion("wolf_lichen", new MobEffectInstance(LICHEN_EFFECT, 1200)));

    /** Food that is always edible: eating is how a plant takes effect. */
    static final FoodProperties PLANT_FOOD = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().build();
    private static final FoodProperties ASH_FOOD = new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible().build();

    public static final DeferredBlock<WolfsbaneCropBlock> YELLOW_CROP = BLOCKS.register("yellow_wolfsbane",
            () -> new WolfsbaneCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> WolfsbaneStrains.YELLOW_SEEDS.get()));
    public static final DeferredBlock<WolfsbaneCropBlock> NORTHERN_BLUE_CROP = BLOCKS.register("northern_blue_wolfsbane",
            () -> new WolfsbaneCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> WolfsbaneStrains.NORTHERN_BLUE_SEEDS.get()));
    public static final DeferredBlock<WolfLichenBlock> LICHEN_BLOCK = BLOCKS.register("wolf_lichen",
            () -> new WolfLichenBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.MOSS_CARPET)));

    public static final DeferredItem<ItemNameBlockItem> YELLOW_SEEDS = ITEMS.register("yellow_wolfsbane_seeds",
            () -> new ItemNameBlockItem(YELLOW_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> YELLOW_PLANT = ITEMS.register("yellow_wolfsbane", () -> new Item(new Item.Properties().food(PLANT_FOOD)));
    public static final DeferredItem<Item> YELLOW_ASH = ITEMS.register("yellow_wolfsbane_ash", () -> new Item(new Item.Properties().food(ASH_FOOD)));
    public static final DeferredItem<ItemNameBlockItem> NORTHERN_BLUE_SEEDS = ITEMS.register("northern_blue_wolfsbane_seeds",
            () -> new ItemNameBlockItem(NORTHERN_BLUE_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> NORTHERN_BLUE_PLANT = ITEMS.register("northern_blue_wolfsbane", () -> new Item(new Item.Properties().food(PLANT_FOOD)));
    public static final DeferredItem<Item> NORTHERN_BLUE_ASH = ITEMS.register("northern_blue_wolfsbane_ash", () -> new Item(new Item.Properties().food(ASH_FOOD)));
    /** The lichen is both the plant and the block it grows as. */
    public static final DeferredItem<BlockItem> LICHEN = ITEMS.register("wolf_lichen",
            () -> new BlockItem(LICHEN_BLOCK.get(), new Item.Properties().food(PLANT_FOOD)));
    public static final DeferredItem<Item> LICHEN_ASH = ITEMS.register("wolf_lichen_ash", () -> new Item(new Item.Properties().food(ASH_FOOD)));

    private WolfsbaneStrains() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        EFFECTS.register(modBus);
        POTIONS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                e.accept(YELLOW_SEEDS);
                e.accept(NORTHERN_BLUE_SEEDS);
                e.accept(LICHEN);
            } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(YELLOW_PLANT);
                e.accept(NORTHERN_BLUE_PLANT);
                e.accept(YELLOW_ASH);
                e.accept(NORTHERN_BLUE_ASH);
                e.accept(LICHEN_ASH);
            }
        });
    }

    // ---- who and what ----

    /** The strain a plant item carries, or null. */
    public static Strain strainOfPlant(ItemStack s) {
        if (s.is(YELLOW_PLANT.get())) {
            return Strain.YELLOW;
        } else if (s.is(NORTHERN_BLUE_PLANT.get())) {
            return Strain.NORTHERN_BLUE;
        } else if (s.is(LICHEN.get())) {
            return Strain.LICHEN;
        }
        return s.is(COMMON_TAG) ? Strain.COMMON : null;
    }

    /** The strain an ash cures, or null. */
    public static Strain strainOfAsh(ItemStack s) {
        for (Strain strain : Strain.values()) {
            if (strain.ash() != null && s.is(strain.ash())) {
                return strain;
            }
        }
        return null;
    }

    /**
     * Werewolf types: the werewolf faction (players and Werewolves mobs; hybrids, Original hybrids and tribrids are in
     * it), and the hybrid species whatever their faction. {@code Supernatural.isPrey} alone would also match vampires.
     */
    public static boolean isWerewolfType(Entity e) {
        if (VampirismSupernatural.isWerewolf(e)) {
            return true;
        }
        if (e instanceof Player p) {
            Species s = BloodlineAccess.get(p).species();
            return s == Species.HYBRID || s == Species.ORIGINAL_HYBRID || s == Species.TRIBRID;
        }
        return false;
    }

    /**
     * Gives {@code target} the strain's effect. Returns false for anything that is not a werewolf type (the item is then
     * just food). A northern blue already running is not restarted.
     */
    public static boolean apply(LivingEntity target, Strain strain) {
        if (!isWerewolfType(target)) {
            return false;
        }
        switch (strain) {
            case COMMON -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
            }
            case YELLOW -> target.addEffect(new MobEffectInstance(YELLOW_EFFECT, 1200));
            case NORTHERN_BLUE -> {
                if (!target.hasEffect(NORTHERN_BLUE_EFFECT)) {
                    target.addEffect(new MobEffectInstance(NORTHERN_BLUE_EFFECT, 6000));
                }
            }
            case LICHEN -> target.addEffect(new MobEffectInstance(LICHEN_EFFECT, 1200));
        }
        return true;
    }

    /** Removes the strain's effect (the common strain just wears off). */
    public static void cure(LivingEntity target, Strain strain) {
        if (strain.effect() != null) {
            target.removeEffect(strain.effect());
        }
    }

    /**
     * One pulse of a damaging strain, {@code remaining} ticks before the effect runs out: 1 damage that never takes the
     * last half heart (like poison); northern blue kills on its last tick. Creative and spectator players are skipped.
     */
    public static void pulse(LivingEntity target, Strain strain, int remaining) {
        if (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        if (strain == Strain.NORTHERN_BLUE && remaining <= 1) {
            target.hurt(source(target), Float.MAX_VALUE);
        } else if (target.getHealth() > 1f) {
            target.hurt(source(target), 1f);
        }
    }

    private static DamageSource source(LivingEntity target) {
        return new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(POISON));
    }
}
