package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import com.mojang.serialization.JsonOps;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSealCodec;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodDrink;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodKeys;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodTooltip;
import net.ofatech.supernaturalbloodlines.parts.species.item.BottleBloodAction;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Blood bottles carry their source sealed, and tell no client (R210, R211). Needs no Werewolves: see {@code BloodBottleWerewolfGameTests} for the werewolf drinkers. */
@PrefixGameTestTemplate(false)
public final class BloodBottleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation BOTTLE = ResourceLocation.parse("vampirism:vampire_blood_bottle");
    private static final List<BloodKind> SECRET = List.of(BloodKind.ORIGINAL, BloodKind.HYBRID, BloodKind.ORIGINAL_HYBRID, BloodKind.TRIBRID, BloodKind.HERETIC);
    /** A real Original: a sealed bottle that names a sire only counts while its source still is one. */
    private static final UUID MIKAEL = NpcOriginal.MIKAEL.uuid();

    private BloodBottleGameTests() {
    }

    private static Item bottleItem() {
        return BuiltInRegistries.ITEM.get(BOTTLE);
    }

    private static ItemStack stamped(BloodKind kind, UUID id, String name) {
        ItemStack s = new ItemStack(bottleItem());
        BloodStamp.write(s, new BloodSource(id, name, false, kind));
        return s;
    }

    /** What a client would hold after the server sent it this stack. */
    private static ItemStack overTheWire(GameTestHelper helper, ItemStack stack, byte[][] bytesOut) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, stack);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        if (bytesOut != null) {
            bytesOut[0] = bytes;
        }
        return ItemStack.STREAM_CODEC.decode(buf);
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static byte[] uuidBytes(UUID id) {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(16);
        b.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return b.array();
    }

    private static List<String> keys(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component c : lines) {
            out.add(c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString());
        }
        return out;
    }

    private static boolean click(ServerPlayer p) {
        var click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(click);
        return click.isCanceled() && click.getCancellationResult().consumesAction();
    }

    private static void reset(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static BloodlineState species(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    private static void assertNoSecret(GameTestHelper helper, String where, String text, UUID id, String name) {
        helper.assertTrue(!text.contains(id.toString()) && !text.contains(name) && !text.contains("[I;"), where + " shows the source: " + text);
        for (BloodKind k : BloodKind.values()) {
            helper.assertTrue(k == BloodKind.COMMON_VAMPIRE || !text.contains(k.name()), where + " shows the kind " + k + ": " + text);
        }
    }

    // ---- seal, read, label ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stampSealsTheSourceAndLabelsIt(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ItemStack s = stamped(BloodKind.ORIGINAL, id, "Elena");
        BloodSource read = BloodStamp.read(s).orElse(null);
        helper.assertTrue(read != null && read.equals(new BloodSource(id, "Elena", false, BloodKind.ORIGINAL)), "read back " + read);
        helper.assertTrue("?".equals(s.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), "the label is " + s.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()));
        helper.assertTrue(BloodStamp.sealed(s) && !s.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()) && !s.has(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get()),
                "the readable source is still on the stack");
        ItemStack common = stamped(BloodKind.COMMON_VAMPIRE, id, "Bob");
        helper.assertTrue("Bob".equals(common.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), "a common vampire's label is its name");
        helper.assertTrue(BloodStamp.kindOf(new ItemStack(bottleItem())) == BloodKind.COMMON_VAMPIRE && BloodStamp.read(new ItemStack(bottleItem())).isEmpty(),
                "an unstamped bottle is not common vampire blood");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWireTheSaveAndAHoverShowOnlyCiphertext(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        var ra = helper.getLevel().registryAccess();
        int sealLength = -1;
        for (BloodKind kind : SECRET) {
            ItemStack stack = stamped(kind, id, "Elena");
            byte[][] sent = new byte[1][];
            ItemStack client = overTheWire(helper, stack, sent);
            helper.assertTrue(!client.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()) && !client.has(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get()),
                    kind + ": the client got a plain source");
            helper.assertTrue("?".equals(client.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), kind + ": the label is " + client.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()));
            helper.assertTrue(!contains(sent[0], uuidBytes(id)) && !contains(sent[0], "Elena".getBytes(StandardCharsets.UTF_8))
                    && !contains(sent[0], kind.name().getBytes(StandardCharsets.UTF_8)), kind + ": the wire bytes hold a secret");
            // what a client lacks is the key: with another one the stack it holds is no source at all
            byte[] real = BloodKeys.key();
            BloodKeys.overrideKey(BloodSealCodec.newKey());
            try {
                helper.assertTrue(BloodStamp.read(client).isEmpty(), kind + ": a stack opens without the world's key");
            } finally {
                BloodKeys.overrideKey(null);
            }
            helper.assertTrue(real != null && BloodStamp.read(client).isPresent(), kind + ": the server cannot open its own seal");
            // the saved form (a decorated pot's update tag, a block entity, level data) and a chat hover
            Tag saved = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, ra), stack).getOrThrow();
            assertNoSecret(helper, kind + ": ItemStack.CODEC", saved.toString(), id, "Elena");
            assertNoSecret(helper, kind + ": ItemStack.save", stack.save(ra).toString(), id, "Elena");
            Component hover = Component.literal("x").withStyle(st -> st.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack))));
            assertNoSecret(helper, kind + ": a chat hover", ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, ra), hover).getOrThrow().toString(), id, "Elena");
            int len = stack.get(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get()).length();
            helper.assertTrue(sealLength == -1 || sealLength == len, kind + ": the seal's length tells it from an Original's");
            sealLength = len;
            // the save survives a round trip
            ItemStack back = ItemStack.parse(ra, stack.save(ra)).orElse(ItemStack.EMPTY);
            helper.assertTrue(BloodStamp.read(back).equals(BloodStamp.read(stack)) && BloodStamp.read(back).isPresent(), kind + ": a saved stack lost its source");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTamperedOrForeignSealIsUnstamped(GameTestHelper helper) {
        ItemStack s = stamped(BloodKind.ORIGINAL, MIKAEL, "Mikael");
        String seal = s.get(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get());
        char flipped = seal.charAt(20) == 'A' ? 'B' : 'A';
        ItemStack tampered = s.copy();
        tampered.set(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get(), seal.substring(0, 20) + flipped + seal.substring(21));
        helper.assertTrue(BloodStamp.read(tampered).isEmpty() && BloodStamp.kindOf(tampered) == BloodKind.COMMON_VAMPIRE, "a tampered seal opened");
        ItemStack forged = s.copy();
        forged.set(SpeciesRegistries.BLOOD_SEAL_COMPONENT.get(), "AAAA");
        helper.assertTrue(BloodStamp.read(forged).isEmpty(), "a forged seal opened");
        helper.assertTrue(BloodBottles.sourceOf(helper.getLevel().getServer(), tampered).kind() == BloodKind.COMMON_VAMPIRE, "a tampered seal is not common vampire blood to the drink");
        BloodKeys.overrideKey(BloodSealCodec.newKey());
        try {
            helper.assertTrue(BloodStamp.read(s).isEmpty(), "a seal of another world (or a lost key) opened");
        } finally {
            BloodKeys.overrideKey(null);
        }
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, tampered);
            helper.assertTrue(click(p) && BloodlineAccess.get(p).vampireBloodUntil() != 0 && !p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT),
                    "a tampered bottle is not plain common blood: it named a sire or did nothing");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOldPlainBottleIsReadAndMovedIntoASeal(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ItemStack old = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        old.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), id);
        old.set(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get(), "ORIGINAL");
        helper.assertTrue(BloodStamp.has(old) && BloodStamp.read(old).filter(s -> s.id().equals(id) && s.kind() == BloodKind.ORIGINAL).isPresent(), "an old bottle is not read");
        ItemStack noSpecies = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        noSpecies.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), id);
        helper.assertTrue(BloodStamp.kindOf(noSpecies) == BloodKind.ORIGINAL_HYBRID, "an old bottle with no species is not an Original hybrid's");
        helper.assertTrue(BloodStamp.migrate(old) && BloodStamp.sealed(old) && !old.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get())
                && !old.has(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get()), "the old bottle was not sealed");
        helper.assertTrue(BloodStamp.read(old).filter(s -> s.id().equals(id) && s.kind() == BloodKind.ORIGINAL).isPresent() && "?".equals(old.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())),
                "the sealed bottle lost its source");
        assertNoSecret(helper, "a migrated bottle", old.save(helper.getLevel().registryAccess()).toString(), id, "Elena");
        helper.assertTrue(!BloodStamp.migrate(old), "a sealed bottle was sealed again");
        RecordingPlayer p = RecordingPlayer.create(helper);
        ItemStack another = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        another.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), id);
        p.getInventory().add(another);
        helper.assertTrue(BloodBottles.sealOld(p) == 1 && BloodStamp.sealed(p.getInventory().getItem(0)), "the login sweep did not seal the player's bottle");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siresBloodIsSealedAndStillWorks(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ItemStack sires = SiresBloodItem.of(id, "Damon");
        helper.assertTrue(BloodStamp.sealed(sires) && !sires.has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()), "Sire's Blood is not sealed");
        helper.assertTrue(id.equals(SiresBloodItem.sourceOf(sires)), "the server lost the source");
        helper.assertTrue("Damon".equals(sires.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT.get())) && !sires.has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()),
                "its own public name is gone, or it was labelled as drinkable blood");
        String saved = sires.save(helper.getLevel().registryAccess()).toString();
        helper.assertTrue(!saved.contains(id.toString()) && !saved.contains("[I;"), "the saved Sire's Blood shows the source: " + saved);
        ItemStack old = new ItemStack(SpeciesRegistries.SIRES_BLOOD.get());
        old.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), id);
        old.set(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT.get(), "Damon");
        helper.assertTrue(id.equals(SiresBloodItem.sourceOf(old)), "an old Sire's Blood does not work");
        helper.assertTrue(BloodStamp.migrate(old) && id.equals(SiresBloodItem.sourceOf(old)) && "Damon".equals(old.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT.get()))
                && !old.has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()), "an old Sire's Blood stopped working when sealed");
        helper.assertTrue(SiresBloodItem.sourceOf(new ItemStack(SpeciesRegistries.SIRES_BLOOD.get())) == null, "blood from nobody has a source");
        helper.succeed();
    }

    // ---- tooltips ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void tooltipNeverNamesASecretAndWarns(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        for (BloodKind kind : SECRET) {
            for (ItemStack shown : List.of(stamped(kind, id, "Elena"), overTheWire(helper, stamped(kind, id, "Elena"), null))) {
                List<Component> lines = new ArrayList<>();
                BloodTooltip.append(shown, lines);
                helper.assertTrue(keys(lines).equals(List.of(BloodTooltip.STRANGE, BloodTooltip.WARNING)), kind + ": the tooltip is " + keys(lines));
                List<Component> wolf = new ArrayList<>();
                BloodTooltip.append(shown, wolf, true);
                helper.assertTrue(keys(wolf).equals(List.of(BloodTooltip.STRANGE, BloodTooltip.WARNING, BloodTooltip.TRANSITION)), kind + ": a werewolf in transition sees " + keys(wolf));
            }
        }
        List<Component> common = new ArrayList<>();
        BloodTooltip.append(overTheWire(helper, stamped(BloodKind.COMMON_VAMPIRE, id, "Bob"), null), common);
        helper.assertTrue(keys(common).equals(List.of(BloodTooltip.COMMON, BloodTooltip.FROM))
                && common.get(1).getContents() instanceof TranslatableContents t && t.getArgs()[0].equals("Bob"), "a common vampire's tooltip is " + keys(common));
        helper.assertTrue(common.get(1).getStyle().getColor() != null && common.get(1).getStyle().getColor().equals(common.get(0).getStyle().getColor()), "the 'From' line is not the same grey");
        List<Component> plain = new ArrayList<>();
        BloodTooltip.append(new ItemStack(bottleItem()), plain);
        helper.assertTrue(keys(plain).equals(List.of(BloodTooltip.COMMON)), "an unstamped bottle's tooltip is " + keys(plain));
        List<Component> glass = new ArrayList<>();
        BloodTooltip.append(new ItemStack(Items.GLASS_BOTTLE), glass, true);
        helper.assertTrue(glass.isEmpty(), "a glass bottle got a blood line");
        helper.succeed();
    }

    // ---- where bottles are made ----

    private static ItemEntity dropOf(GameTestHelper helper, ItemStack stack) {
        return new ItemEntity(helper.getLevel(), 0, 0, 0, stack);
    }

    private static LivingEntity vampireMob(GameTestHelper helper) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:advanced_vampire"));
        helper.assertTrue(type != null && type.create(helper.getLevel()) instanceof LivingEntity, "no advanced vampire");
        return (LivingEntity) type.create(helper.getLevel());
    }

    private static void dies(GameTestHelper helper, LivingEntity mob, ItemEntity... drops) {
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(mob, helper.getLevel().damageSources().generic(), new ArrayList<>(List.of(drops)), true));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCommonVampireMobsDropIsNotStampedSoItKeepsStacking(GameTestHelper helper) {
        LivingEntity mob = vampireMob(helper);
        ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
        ItemEntity flesh = dropOf(helper, new ItemStack(Items.ROTTEN_FLESH));
        dies(helper, mob, bottle, flesh);
        helper.assertTrue(!BloodStamp.has(bottle.getItem()) && !bottle.getItem().has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()), "a common vampire's drop was stamped");
        helper.assertTrue(ItemStack.isSameItemSameComponents(bottle.getItem(), new ItemStack(bottleItem())), "the drop no longer stacks with another bottle");
        helper.assertTrue(!flesh.getItem().has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()), "another drop was stamped");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anNpcOriginalsDropIsStampedWithItsRootAndKind(GameTestHelper helper) {
        for (NpcOriginal o : List.of(NpcOriginal.MIKAEL, NpcOriginal.KLAUS)) {
            LivingEntity mob = vampireMob(helper);
            mob.setData(SpeciesRegistries.ORIGINAL_ROOT, o.id());
            ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
            dies(helper, mob, bottle);
            BloodKind want = o == NpcOriginal.KLAUS ? BloodKind.ORIGINAL_HYBRID : BloodKind.ORIGINAL;
            BloodSource s = BloodStamp.read(bottle.getItem()).orElse(null);
            helper.assertTrue(s != null && s.kind() == want && s.id().equals(o.uuid()) && s.npc() && !s.id().equals(mob.getUUID()), o + ": the drop is " + s);
            helper.assertTrue("?".equals(bottle.getItem().get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), o + ": the label shows");
            helper.assertTrue(BloodBottles.sourceOf(helper.getLevel().getServer(), bottle.getItem()).equals(s), o + ": the drink does not believe it");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noBottleFromAHuman(GameTestHelper helper) {
        LivingEntity zombie = EntityType.ZOMBIE.create(helper.getLevel());
        ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
        dies(helper, zombie, bottle);
        helper.assertTrue(!BloodStamp.has(bottle.getItem()), "a zombie's bottle was stamped");
        ServerPlayer player = TestPlayers.createMortal(helper);
        ItemEntity mine = dropOf(helper, new ItemStack(bottleItem()));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(player, helper.getLevel().damageSources().generic(), new ArrayList<>(List.of(mine)), true));
        helper.assertTrue(!BloodStamp.has(mine.getItem()), "a player's bottle was stamped");
        helper.succeed();
    }

    // ---- forging ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSealedOriginalBottleOnlyCountsWhileItsSourceStillIsThatOriginal(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL, MIKAEL, "Mikael")).id().equals(MIKAEL), "a real Original's bottle was refused");
        helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL_HYBRID, NpcOriginal.KLAUS.uuid(), "Klaus")).kind() == BloodKind.ORIGINAL_HYBRID, "Klaus's bottle was refused");
        helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL_HYBRID, MIKAEL, "Mikael")) == BloodBottles.UNSTAMPED, "Mikael is an Original hybrid");
        helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL, UUID.randomUUID(), "Nobody")) == BloodBottles.UNSTAMPED, "a bottle of nobody named a sire");
        ServerPlayer source = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(source, species(Species.ORIGINAL));
            helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL, source.getUUID(), "Elena")).id().equals(source.getUUID()), "an online Original's bottle was refused");
            BloodlineAccess.set(source, BloodlineState.NONE);
            helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.ORIGINAL, source.getUUID(), "Elena")) == BloodBottles.UNSTAMPED, "a cured Original's old bottle still names them");
            helper.assertTrue(BloodBottles.sourceOf(server, stamped(BloodKind.HYBRID, UUID.randomUUID(), "Anyone")).kind() == BloodKind.HYBRID, "a hybrid's bottle is checked like an Original's");
        } finally {
            reset(source);
        }
        helper.succeed();
    }

    // ---- drinking ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unstampedBottleMarksAHumanAndWarnsThem(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        ServerPlayer reference = TestPlayers.createMortal(helper);
        try {
            net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler.drankVampireBlood(reference);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem()));
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(BloodlineAccess.get(p).equals(BloodlineAccess.get(reference)) && BloodlineAccess.get(p).vampireBloodUntil() != 0,
                    "state " + BloodlineAccess.get(p) + " is not what the old drink gave: " + BloodlineAccess.get(reference));
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT), "common blood named a sire");
            helper.assertTrue(p.received(BloodBottles.MARKED), "the human was not warned: " + p.heard());
        } finally {
            reset(p, reference);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalsStampNamesThePendingSireAndWarns(GameTestHelper helper) {
        RecordingPlayer warned = RecordingPlayer.create(helper);
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            warned.setItemInHand(InteractionHand.MAIN_HAND, stamped(BloodKind.ORIGINAL, MIKAEL, "Mikael"));
            helper.assertTrue(click(warned) && warned.received(BloodDrink.DRUNK_MARKED) && !warned.received(BloodDrink.DRUNK), "the human was not warned: " + warned.heard());
            p.setItemInHand(InteractionHand.MAIN_HAND, stamped(BloodKind.ORIGINAL, MIKAEL, "Mikael"));
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && MIKAEL.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE))
                    && "ORIGINAL".equals(p.getData(SpeciesRegistries.BLOOD_SOURCE_SPECIES)), "the blood is not the pending sire");
            helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() != 0, "an Original's blood does not also mark (Q12)");
            helper.assertTrue(Lines.shownName(helper.getLevel().getServer(), p, MIKAEL).getContents() instanceof TranslatableContents t && t.getKey().equals("supernaturalbloodlines.line.unknown_original"),
                    "an undiscovered line is named");
            p.hurt(helper.getLevel().damageSources().generic(), 1000f);
            helper.assertTrue(BloodlineAccess.get(p).inTurning() && MIKAEL.equals(BloodlineAccess.get(p).pendingSire()), "the pending sire is " + BloodlineAccess.get(p));
        } finally {
            reset(p, warned);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalBloodItemMarksAHumanToo(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            ItemStack blood = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            BloodStamp.write(blood, new BloodSource(MIKAEL, "Mikael", false, BloodKind.ORIGINAL));
            p.setItemInHand(InteractionHand.MAIN_HAND, blood.finishUsingItem(helper.getLevel(), p));
            helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() != 0 && MIKAEL.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE)), "one bottle is not enough (Q12)");
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanDrinksEveryKindAndOnlyAnOriginalNamesASire(GameTestHelper helper) {
        for (BloodKind kind : BloodKind.values()) {
            ServerPlayer p = TestPlayers.createMortal(helper);
            try {
                UUID id = kind.namesSire() ? MIKAEL : UUID.randomUUID();
                ItemStack bottle = stamped(kind == BloodKind.ORIGINAL_HYBRID ? BloodKind.ORIGINAL : kind, id, "Someone");
                p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
                helper.assertTrue(click(p) && BloodlineAccess.get(p).vampireBloodUntil() != 0, kind + ": a human was not marked");
                helper.assertTrue(p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) == kind.namesSire(), kind + ": the sire naming is wrong");
            } finally {
                reset(p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulDrinksByTheSameRules(GameTestHelper helper) {
        ServerPlayer ghoul = TestPlayers.createMortal(helper);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(ghoul).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
            BloodlineAccess.set(ghoul, BloodlineState.NONE.toBuilder().ghoul(true).build());
            helper.assertTrue(BloodBottles.drinkerOf(ghoul) == BloodBottleRules.Drinker.HUMAN, "a Ghoul is not a human drinker");
            ghoul.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem()));
            helper.assertTrue(click(ghoul) && BloodlineAccess.get(ghoul).vampireBloodUntil() != 0 && ghoul.getMainHandItem().is(Items.GLASS_BOTTLE), "a Ghoul's click was not taken over");
            ghoul.setItemInHand(InteractionHand.MAIN_HAND, stamped(BloodKind.ORIGINAL, MIKAEL, "Mikael"));
            helper.assertTrue(click(ghoul) && MIKAEL.equals(ghoul.getData(SpeciesRegistries.BLOOD_SOURCE)), "a Ghoul did not name the sire");
        } finally {
            VampirismAPI.factionPlayerHandler(ghoul).leaveFaction(false);
            reset(ghoul);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireAndHunterKeepTheBottleAndAreTold(GameTestHelper helper) {
        RecordingPlayer vampire = RecordingPlayer.create(helper);
        RecordingPlayer hunter = RecordingPlayer.create(helper);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
            for (RecordingPlayer p : List.of(vampire, hunter)) {
                ItemStack bottle = stamped(BloodKind.COMMON_VAMPIRE, UUID.randomUUID(), "Bob");
                p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
                helper.assertTrue(!click(p), "a click was taken over");
                helper.assertTrue(p.getMainHandItem() == bottle && bottle.getCount() == 1, "the bottle was used up");
                helper.assertTrue(BloodBottles.decide(p, BloodBottles.sourceOf(helper.getLevel().getServer(), bottle)) == BloodBottleRules.Effect.REFUSED, "the drink is not refused");
                helper.assertTrue(p.receivedOnActionBar(BloodBottles.REFUSED), "nobody said why: " + p.heard());
            }
            helper.assertTrue(BloodlineAccess.get(vampire).vampireBloodUntil() == 0 && BloodlineAccess.get(hunter).vampireBloodUntil() == 0, "someone was marked");
        } finally {
            VampirismAPI.factionPlayerHandler(vampire).leaveFaction(false);
            VampirismAPI.factionPlayerHandler(hunter).leaveFaction(false);
            reset(vampire, hunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMarkedPlayerKeepsTheBottle(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem()));
            helper.assertTrue(click(p), "the first drink did not work");
            ItemStack second = new ItemStack(bottleItem());
            p.setItemInHand(InteractionHand.MAIN_HAND, second);
            helper.assertTrue(!click(p) && p.getMainHandItem() == second, "a second drink in the same tick gained nothing and still used the bottle up");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theClientSideOfOriginalBloodDrinksNothing(GameTestHelper helper) {
        ItemStack blood = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        BloodStamp.write(blood, new BloodSource(MIKAEL, "Mikael", false, BloodKind.ORIGINAL));
        helper.assertTrue(blood.finishUsingItem(helper.getLevel(), EntityType.ZOMBIE.create(helper.getLevel())) == blood, "a mob drank it");
        helper.succeed();
    }

    // ---- bottle my blood ----

    private static ServerPlayer vampireWithBottle(GameTestHelper helper, Species species, boolean glass) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, species(species));
        p.getInventory().clearContent();
        if (glass) {
            p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE, 2));
        }
        p.setHealth(20f);
        return p;
    }

    private static ItemStack found(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(bottleItem())) {
                return p.getInventory().getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private static int glass(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(Items.GLASS_BOTTLE)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private static String say(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodGivesAStampedBottle(GameTestHelper helper) {
        ServerPlayer p = vampireWithBottle(helper, Species.HYBRID, true);
        try {
            helper.assertTrue(BottleBloodAction.INSTANCE.available(p), "a hybrid cannot bottle their blood");
            helper.assertTrue(BottleBloodAction.INSTANCE.trigger(p) == null, "the action was refused");
            ItemStack out = found(p);
            BloodSource s = BloodStamp.read(out).orElse(null);
            helper.assertTrue(s != null && s.kind() == BloodKind.HYBRID && s.id().equals(p.getUUID()) && !s.npc(), "the bottle is " + s);
            helper.assertTrue("?".equals(out.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), "the label is " + out.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()));
            helper.assertTrue(glass(p) == 1, "glass bottles left: " + glass(p));
            helper.assertTrue(p.getHealth() == 20f - (float) SpeciesConfig.bottleBloodHealthCost(), "health is " + p.getHealth());
            helper.assertTrue(ActionClock.left(p, ExtraActions.BOTTLE_BLOOD) == SpeciesConfig.bottleBloodCooldownSeconds() * 20
                    && BottleBloodAction.INSTANCE.cooldownLeft(p) > 0, "no cooldown");
            helper.assertTrue(BottleBloodAction.COOLDOWN.equals(say(BottleBloodAction.INSTANCE.trigger(p))), "a second bottle came at once");
            helper.assertTrue(glass(p) == 1, "the refused action used a bottle");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodNeverKillsAndSaysWhenTooWeak(GameTestHelper helper) {
        ServerPlayer p = vampireWithBottle(helper, Species.HERETIC, true);
        try {
            float cost = (float) SpeciesConfig.bottleBloodHealthCost();
            p.setHealth(cost);
            helper.assertTrue(BottleBloodAction.TOO_WEAK.equals(say(BottleBloodAction.INSTANCE.trigger(p))), "at the cost it was not refused as too weak");
            helper.assertTrue(p.getHealth() == cost && p.isAlive() && glass(p) == 2 && found(p).isEmpty() && ActionClock.ready(p, ExtraActions.BOTTLE_BLOOD), "a refused action cost something");
            p.setHealth(cost + 0.5f);
            helper.assertTrue(BottleBloodAction.INSTANCE.trigger(p) == null && p.getHealth() == 0.5f && p.isAlive(), "just above the cost it did not work, health " + p.getHealth());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodSaysWhatIsMissingAndKeepsTheVagueLineForOriginals(GameTestHelper helper) {
        ServerPlayer noGlass = vampireWithBottle(helper, Species.HYBRID, false);
        ServerPlayer originalNoGlass = vampireWithBottle(helper, Species.ORIGINAL, false);
        List<ServerPlayer> originals = List.of(vampireWithBottle(helper, Species.ORIGINAL, true), vampireWithBottle(helper, Species.ORIGINAL_HYBRID, true), originalNoGlass);
        try {
            helper.assertTrue(BottleBloodAction.NEED_GLASS.equals(say(BottleBloodAction.INSTANCE.trigger(noGlass))), "a hybrid with no glass bottle was not told so");
            for (ServerPlayer o : originals) {
                helper.assertTrue(BottleBloodAction.NOTHING.equals(say(BottleBloodAction.INSTANCE.trigger(o))), "an Original heard " + say(BottleBloodAction.INSTANCE.trigger(o)));
                helper.assertTrue(!BottleBloodAction.INSTANCE.available(o), "an Original is offered the action");
                helper.assertTrue(o.getHealth() == 20f && found(o).isEmpty() && ActionClock.ready(o, ExtraActions.BOTTLE_BLOOD), "an Original paid for nothing");
            }
        } finally {
            reset(noGlass);
            originals.forEach(o -> reset(o));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodTellsWhenTheBottleFallsAndStopsAtTheDailyLimit(GameTestHelper helper) {
        RecordingPlayer full = RecordingPlayer.create(helper);
        ServerPlayer limited = vampireWithBottle(helper, Species.TRIBRID, true);
        try {
            BloodlineAccess.set(full, species(Species.HYBRID));
            full.getInventory().clearContent();
            full.getInventory().add(new ItemStack(Items.GLASS_BOTTLE, 2));
            for (int i = 0; i < full.getInventory().items.size(); i++) {
                if (full.getInventory().items.get(i).isEmpty()) {
                    full.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
                }
            }
            full.setHealth(20f);
            helper.assertTrue(BottleBloodAction.INSTANCE.trigger(full) == null && full.receivedOnActionBar(BottleBloodAction.DROPPED), "a full inventory was not told: " + full.heard());
            for (int i = 0; i < SpeciesConfig.bottleBloodDailyLimit() - 1; i++) {
                ActionClock.countUse(limited, ExtraActions.BOTTLE_BLOOD);
            }
            helper.assertTrue(BottleBloodAction.INSTANCE.trigger(limited) == null, "the last bottle of the day was refused");
            helper.assertTrue(ActionClock.usesToday(limited, ExtraActions.BOTTLE_BLOOD) == SpeciesConfig.bottleBloodDailyLimit(), "the day's count is " + ActionClock.usesToday(limited, ExtraActions.BOTTLE_BLOOD));
            limited.setHealth(20f);
            ActionClock.start(limited, ExtraActions.BOTTLE_BLOOD, 0);
            helper.assertTrue(BottleBloodAction.DAILY.equals(say(BottleBloodAction.INSTANCE.trigger(limited))), "one more than the daily limit was given");
        } finally {
            reset(full, limited);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodNoteQuotesTheConfig(GameTestHelper helper) {
        var args = ((TranslatableContents) BottleBloodAction.INSTANCE.note().getContents()).getArgs();
        helper.assertTrue(args.length == 3 && args[0].equals("3") && args[1].equals(SpeciesConfig.bottleBloodCooldownSeconds())
                && args[2].equals(SpeciesConfig.bottleBloodDailyLimit()), "the note quotes " + java.util.Arrays.toString(args));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void actionClockSurvivesDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ActionClock.start(p, ExtraActions.BOTTLE_BLOOD, 100);
        ServerPlayer reborn = TestPlayers.createMortal(helper);
        reborn.copyAttachmentsFrom(p, true);
        helper.assertTrue(ActionClock.left(reborn, ExtraActions.BOTTLE_BLOOD) == 100, "the reborn player's clock reads " + ActionClock.left(reborn, ExtraActions.BOTTLE_BLOOD));
        helper.assertTrue(ActionClock.ready(p, ResourceLocation.fromNamespaceAndPath(NS, "another")), "one clock holds up another action");
        helper.succeed();
    }
}
