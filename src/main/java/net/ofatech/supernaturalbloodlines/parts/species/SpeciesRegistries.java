package net.ofatech.supernaturalbloodlines.parts.species;

import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.item.DoppelgangerBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.item.HybridBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.item.HybridVenomEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.OriginalBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.item.UnstableEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.InfectionSire;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.VillagerDiscount;

/** Everything the species part registers: Original's Blood (item, effect, component, attachment), the downed attachment and the white oak set. */
public final class SpeciesRegistries {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SupernaturalBloodlines.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SupernaturalBloodlines.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SupernaturalBloodlines.MODID);

    public static final DeferredItem<OriginalBloodItem> ORIGINAL_BLOOD =
            ITEMS.register("original_blood", () -> new OriginalBloodItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<DoppelgangerBloodItem> DOPPELGANGER_BLOOD =
            ITEMS.register("doppelganger_blood", () -> new DoppelgangerBloodItem(new Item.Properties().stacksTo(16)));

    /** The only cure for werewolf venom. */
    public static final DeferredItem<HybridBloodItem> HYBRID_BLOOD =
            ITEMS.register("hybrid_blood", () -> new HybridBloodItem(new Item.Properties().stacksTo(16)));

    /** Ingredient of the Rite of the Moon (crafted: amethyst shard + Werewolves silver ingot; no loot source yet). */
    public static final DeferredItem<Item> MOONSTONE = ITEMS.registerSimpleItem("moonstone");

    /** Whose blood a bottle of Original's Blood is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BLOOD_SOURCE_COMPONENT =
            COMPONENTS.registerComponentType("blood_source", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<MobEffect, OriginalBloodEffect> ORIGINAL_BLOOD_EFFECT =
            EFFECTS.register("original_blood", OriginalBloodEffect::new);

    /** A failing hybrid transition: when it ends the player dies (see TransitionHandler). */
    public static final DeferredHolder<MobEffect, UnstableEffect> UNSTABLE_EFFECT =
            EFFECTS.register("unstable", UnstableEffect::new);

    /** Werewolf venom on a vampire (see {@code hybrid/Venom}). */
    public static final DeferredHolder<MobEffect, HybridVenomEffect> HYBRID_VENOM =
            EFFECTS.register("hybrid_venom", HybridVenomEffect::new);

    /**
     * The Original whose blood the player drank last (the last drink wins). Use {@code hasData} to tell "none" from the
     * default. Not copied on death: the death is handled while the player still has it.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> BLOOD_SOURCE =
            ATTACHMENTS.register("blood_source", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** Ticks left of the downed state (0 = not downed). Saved, so a relog resumes it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DOWNED =
            ATTACHMENTS.register("downed", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /**
     * Who sired this creature (a converted mob; players keep it in the bloodline state). Saved, not copied on death:
     * read it with {@code hasData}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> SIRE =
            ATTACHMENTS.register("sire", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** The player or creature who infected this one with sanguinare, for a day; see {@link InfectionSire}. Not copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InfectionSire>> INFECTION_SIRE =
            ATTACHMENTS.register("infection_sire", () -> AttachmentType.builder(() -> new InfectionSire(Util.NIL_UUID, "", 0))
                    .serialize(InfectionSire.CODEC).build());

    /** Game time of the player's last {@code /bloodline call}; use {@code hasData} to tell "never" from the default. Kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> LAST_CALL =
            ATTACHMENTS.register("last_call", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());

    /** The Original or hybrid whose compulsion made this lesser vampire theirs; it never hunts them or their line. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> COMPELLED_BY =
            ATTACHMENTS.register("compelled_by", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** The discount a compelled villager gives, until it runs out; see {@link VillagerDiscount}. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<VillagerDiscount>> VILLAGER_DISCOUNT =
            ATTACHMENTS.register("villager_discount", () -> AttachmentType.builder(() -> new VillagerDiscount(Util.NIL_UUID, 0, 0))
                    .serialize(VillagerDiscount.CODEC).build());

    private SpeciesRegistries() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        WhiteOak.register(modBus);
    }
}
