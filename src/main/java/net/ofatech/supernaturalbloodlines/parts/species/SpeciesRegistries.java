package net.ofatech.supernaturalbloodlines.parts.species;

import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
import net.ofatech.supernaturalbloodlines.parts.species.item.ShockedEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.item.Taser;
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

    /** Wes's syringe (R202): makes a plain vampire an Augustine. */
    public static final DeferredItem<net.ofatech.supernaturalbloodlines.parts.species.item.AugustineSyringeItem> AUGUSTINE_SYRINGE =
            ITEMS.register("augustine_syringe", () -> new net.ofatech.supernaturalbloodlines.parts.species.item.AugustineSyringeItem(new Item.Properties().stacksTo(16)));

    /** Ingredient of the Rite of the Moon (crafted: amethyst shard + Werewolves silver ingot; silver spawns as ore). */
    public static final DeferredItem<Item> MOONSTONE = ITEMS.registerSimpleItem("moonstone");

    /** R101b: a bottle of blood taken from someone with a silver blade; bound to their UUID and name (the blood-source components). Consumed by the Rite of Severance. */
    public static final DeferredItem<SiresBloodItem> SIRES_BLOOD =
            ITEMS.register("sires_blood", () -> new SiresBloodItem(new Item.Properties().stacksTo(1)));

    /** A small silver blade for taking Sire's Blood where no other silver blade exists (the blades are the {@code silver_blades} item tag). */
    public static final DeferredItem<SwordItem> SILVER_DAGGER = ITEMS.register("silver_dagger",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 1, -1.8F))));

    /** The hunter's Taser (shocks supernaturals). Recipe: lightning rod over redstone over an iron ingot. */
    public static final DeferredItem<Taser> TASER = ITEMS.register("taser", () -> new Taser(new Item.Properties()));

    /** Electricity's mark (see {@code pack/Electricity}). */
    public static final DeferredHolder<MobEffect, ShockedEffect> SHOCKED = EFFECTS.register("shocked", ShockedEffect::new);

    /** A snapped neck on a creature that is not a player (R214): it lies down for the duration. */
    public static final DeferredHolder<MobEffect, net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect> SNAPPED =
            EFFECTS.register("snapped", net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect::new);

    /**
     * Game time until which a player or creature whose snap just ended cannot be snapped again (R214, {@code neck_snap_grace_seconds}). Saved, so a logout does
     * not end it. Not kept through death.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> SNAP_GRACE_UNTIL =
            ATTACHMENTS.register("snap_grace_until", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    /** The lore a Torn Page carries ({@link net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable#loreIds}): what reading it teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RITE_LORE_COMPONENT =
            COMPONENTS.registerComponentType("rite_lore", b -> b.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** A page torn from an old grimoire: read once, it teaches the rites of its lore (plan rites-unlock). Found in loot and dropped by Finn and Elijah. */
    public static final DeferredItem<net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem> RITE_PAGE =
            ITEMS.register("rite_page", () -> new net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Whose blood a bottle of Original's Blood is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOOD_SOURCE_NAME_COMPONENT =
            COMPONENTS.registerComponentType("blood_source_name", b -> b.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /**
     * OLD plain form (R40a), never written any more: the species ({@code Species} name) of the one a bottle was taken from. A stack that still has it (and
     * {@link #BLOOD_SOURCE_COMPONENT}) was made before sealing; {@code BloodStamp} reads it and moves it into {@link #BLOOD_SEAL_COMPONENT}. It syncs as
     * nothing, so a client never sees it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOOD_SOURCE_SPECIES_COMPONENT =
            COMPONENTS.registerComponentType("blood_source_species", b -> b.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(unit("")));

    /** OLD plain form, never written any more: the source's UUID (see {@link #BLOOD_SOURCE_SPECIES_COMPONENT}). Syncs as nothing. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BLOOD_SOURCE_COMPONENT =
            COMPONENTS.registerComponentType("blood_source", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(unit(Util.NIL_UUID)));

    /**
     * Whose blood a bottle holds, sealed (R210): one opaque string (AES-GCM under the world's key, see {@code BloodSealCodec}). The only place the source lives
     * on a stack, so the saved item, the wire, a decorated pot and a chat hover show nothing but ciphertext. {@code blood_source} and
     * {@code blood_source_species} above are the old plain form: read and moved into a seal, never written.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOOD_SEAL_COMPONENT =
            COMPONENTS.registerComponentType("blood_seal", b -> b.persistent(Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** What a tooltip shows: {@code "?"} for every secret kind, a common vampire's name otherwise. The only synced part of a stamped bottle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOOD_LABEL_COMPONENT =
            COMPONENTS.registerComponentType("blood_label", b -> b.persistent(Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

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

    /** The species ({@code Species} name) of the one whose blood the player drank last, for the hybrid transition's source check at death; absent for blood that names none. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> BLOOD_SOURCE_SPECIES =
            ATTACHMENTS.register("blood_source_species", () -> AttachmentType.builder(() -> "").serialize(com.mojang.serialization.Codec.STRING).build());

    /** Ticks left of the downed state (0 = not downed). Saved, so a relog resumes it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DOWNED =
            ATTACHMENTS.register("downed", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Why the player is downed: {@code Downed.MODE_DYING} (an Original hybrid) or {@code Downed.MODE_TURNING} (vampire blood in them). Saved with {@link #DOWNED}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DOWNED_MODE =
            ATTACHMENTS.register("downed_mode", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Game time each of this player's timed actions is ready again, by action id (see {@code ActionClock}). Saved and kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.Map<String, Long>>> ACTION_READY =
            ATTACHMENTS.register("action_ready", () -> AttachmentType.<java.util.Map<String, Long>>builder(() -> new java.util.HashMap<String, Long>())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG)).copyOnDeath().build());

    /**
     * Who sired this creature (a converted mob; players keep it in the bloodline state). Saved, not copied on death:
     * read it with {@code hasData}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> SIRE =
            ATTACHMENTS.register("sire", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** The root Original of this creature's line: the mob's mirror of its {@code LineIndex} entry. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> LINE =
            ATTACHMENTS.register("line", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** An {@code NpcOriginal} id on an entity that is that NPC Original (set by the boss entities); rites and line death tell Originals apart by it. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> ORIGINAL_ROOT =
            ATTACHMENTS.register("original_root", () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /** An NPC Original that is desiccated (open to white oak): the boss entities set it, the Imprisoned Original rite reads it ({@code Imprisonment.isDown}). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> NPC_OPEN =
            ATTACHMENTS.register("npc_open", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

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

    /** The Alpha whose bite is turning this player (the sanguinem effect is running); NIL when none. Saved, not copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> PENDING_ALPHA =
            ATTACHMENTS.register("pending_alpha", () -> AttachmentType.builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    /** True while a wild werewolf's bite (or one with no known biter) is turning this player. Saved, not copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PENDING_WILD =
            ATTACHMENTS.register("pending_wild", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Marks a mob as a wild NPC werewolf of a spawned pack or a Wolf Spirit (plan 15b); absent on every other mob. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark>> NPC_MARK =
            ATTACHMENTS.register("npc_mark", () -> AttachmentType.<net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark>builder(() -> net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark.wild(0, 0))
                    .serialize(net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark.CODEC).build());

    /** Makes a mob a member of a player's pack (plan 15d, R148, R149): whose, how it came, its order. Absent on every other mob. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond>> NPC_BOND =
            ATTACHMENTS.register("npc_bond", () -> AttachmentType.<net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond>builder(() -> net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.NONE)
                    .serialize(net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.CODEC).build());

    /** Marks the boss of an Alpha Trial (plan 15c, R147): the challenger, the beasts' pack id, the night, the phase and how long the challenger has been away. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark>> TRIAL_MARK =
            ATTACHMENTS.register("trial_mark", () -> AttachmentType.<net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark>builder(() -> new net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark(null, 0, 0, 0, 0))
                    .serialize(net.ofatech.supernaturalbloodlines.core.bloodline.TrialMark.CODEC).build());

    /** What waits of this player's delayed werewolf awakening (R142): the day time it is due and the game time it was queued; due 0 is nothing. Kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules.Pending>> PENDING_AWAKENING =
            ATTACHMENTS.register("pending_awakening", () -> AttachmentType.builder(() -> net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules.Pending.NONE)
                    .serialize(net.ofatech.supernaturalbloodlines.core.bloodline.AwakeningRules.Pending.CODEC).copyOnDeath().build());

    /** The discount a compelled villager gives, until it runs out; see {@link VillagerDiscount}. Use {@code hasData}. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<VillagerDiscount>> VILLAGER_DISCOUNT =
            ATTACHMENTS.register("villager_discount", () -> AttachmentType.builder(() -> new VillagerDiscount(Util.NIL_UUID, 0, 0))
                    .serialize(VillagerDiscount.CODEC).build());

    /** Sends nothing and reads as {@code value}: vanilla's {@code StreamCodec.unit} refuses to encode anything else, and a secret must not be sent at all. */
    private static <T> net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, T> unit(T value) {
        return net.minecraft.network.codec.StreamCodec.of((buf, ignored) -> {
        }, buf -> value);
    }

    private SpeciesRegistries() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        WhiteOak.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.COMBAT) {
                e.accept(TASER);
                e.accept(SILVER_DAGGER);
            }
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                for (String lore : net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable.loreIds()) {
                    if (net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable.isPage(lore)) {
                        e.accept(net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem.of(lore));
                    }
                }
            }
        });
    }
}
