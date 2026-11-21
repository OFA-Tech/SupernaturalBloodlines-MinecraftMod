package net.ofatech.supernaturalbloodlines.parts.species.gametest;

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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodTooltip;
import net.ofatech.supernaturalbloodlines.parts.species.item.BottleBloodAction;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Blood bottles carry their source and tell no client (R210, R211). Needs no Werewolves: see {@code BloodBottleWerewolfGameTests} for the werewolf drinkers. */
@PrefixGameTestTemplate(false)
public final class BloodBottleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation BOTTLE = ResourceLocation.parse("vampirism:vampire_blood_bottle");
    private static final List<BloodKind> SECRET = List.of(BloodKind.ORIGINAL, BloodKind.HYBRID, BloodKind.ORIGINAL_HYBRID, BloodKind.TRIBRID, BloodKind.HERETIC);

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

    // ---- stamp, read, label ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stampWritesTheComponents(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ItemStack s = stamped(BloodKind.ORIGINAL, id, "Elena");
        BloodSource read = BloodStamp.read(s).orElse(null);
        helper.assertTrue(read != null && read.equals(new BloodSource(id, "Elena", false, BloodKind.ORIGINAL)), "read back " + read);
        helper.assertTrue("?".equals(s.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), "the label is " + s.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()));
        helper.assertTrue("ORIGINAL".equals(s.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get())), "species " + s.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get()));
        ItemStack common = stamped(BloodKind.COMMON_VAMPIRE, id, "Bob");
        helper.assertTrue("Bob".equals(common.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())) && "NONE".equals(common.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get())),
                "a common vampire's label is its name");
        helper.assertTrue(BloodStamp.kindOf(new ItemStack(bottleItem())) == BloodKind.COMMON_VAMPIRE && BloodStamp.read(new ItemStack(bottleItem())).isEmpty(),
                "an unstamped bottle is not common vampire blood");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nothingSecretIsSynced(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        byte[] first = null;
        for (BloodKind kind : SECRET) {
            byte[][] sent = new byte[1][];
            ItemStack client = overTheWire(helper, stamped(kind, id, "Elena"), sent);
            helper.assertTrue(Util.NIL_UUID.equals(client.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get())), kind + ": the client got a source");
            helper.assertTrue("".equals(client.get(SpeciesRegistries.BLOOD_SOURCE_SPECIES_COMPONENT.get())), kind + ": the client got a species");
            helper.assertTrue("".equals(client.get(SpeciesRegistries.BLOOD_SOURCE_PRIVATE_NAME_COMPONENT.get())), kind + ": the client got a name");
            helper.assertTrue(Boolean.FALSE.equals(client.get(SpeciesRegistries.BLOOD_SOURCE_NPC_COMPONENT.get())), kind + ": the client got the mob flag");
            helper.assertTrue("?".equals(client.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), kind + ": the label is " + client.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()));
            helper.assertTrue(BloodStamp.read(client).isEmpty(), kind + ": the client's copy reads as stamped");
            helper.assertTrue(!contains(sent[0], uuidBytes(id)) && !contains(sent[0], "Elena".getBytes(StandardCharsets.UTF_8))
                    && !contains(sent[0], kind.name().getBytes(StandardCharsets.UTF_8)), kind + ": the wire bytes hold a secret");
            if (first == null) {
                first = sent[0];
            }
            helper.assertTrue(java.util.Arrays.equals(first, sent[0]), kind + ": the wire bytes tell it from an Original's bottle");
        }
        byte[][] sentCommon = new byte[1][];
        ItemStack common = overTheWire(helper, stamped(BloodKind.COMMON_VAMPIRE, id, "Bob"), sentCommon);
        helper.assertTrue("Bob".equals(common.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())) && !contains(sentCommon[0], uuidBytes(id)), "a common vampire's label or UUID");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void tooltipNeverNamesASecret(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        for (BloodKind kind : SECRET) {
            for (ItemStack shown : List.of(stamped(kind, id, "Elena"), overTheWire(helper, stamped(kind, id, "Elena"), null))) {
                List<Component> lines = new ArrayList<>();
                BloodTooltip.append(shown, lines);
                helper.assertTrue(keys(lines).equals(List.of(BloodTooltip.STRANGE)), kind + ": the tooltip is " + keys(lines));
            }
        }
        List<Component> common = new ArrayList<>();
        BloodTooltip.append(overTheWire(helper, stamped(BloodKind.COMMON_VAMPIRE, id, "Bob"), null), common);
        helper.assertTrue(keys(common).equals(List.of(BloodTooltip.COMMON, BloodTooltip.FROM))
                && common.get(1).getContents() instanceof TranslatableContents t && t.getArgs()[0].equals("Bob"), "a common vampire's tooltip is " + keys(common));
        List<Component> plain = new ArrayList<>();
        BloodTooltip.append(new ItemStack(bottleItem()), plain);
        helper.assertTrue(keys(plain).equals(List.of(BloodTooltip.COMMON)), "an unstamped bottle's tooltip is " + keys(plain));
        List<Component> glass = new ArrayList<>();
        BloodTooltip.append(new ItemStack(Items.GLASS_BOTTLE), glass);
        helper.assertTrue(glass.isEmpty(), "a glass bottle got a blood line");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creativeRoundTripReadsAsUnstamped(GameTestHelper helper) {
        ItemStack back = overTheWire(helper, stamped(BloodKind.ORIGINAL, UUID.randomUUID(), "Elena"), null);
        helper.assertTrue(BloodStamp.read(back).isEmpty() && BloodStamp.kindOf(back) == BloodKind.COMMON_VAMPIRE, "a moved bottle is not common vampire blood");
        helper.assertTrue(BloodBottles.sourceOf(back).kind() == BloodKind.COMMON_VAMPIRE, "a moved bottle is not common vampire blood to the drink");
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, back);
            helper.assertTrue(click(p) && BloodlineAccess.get(p).vampireBloodUntil() != 0, "a moved bottle did not mark a human");
        } finally {
            reset(p);
        }
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
    public static void vampireMobDropIsStamped(GameTestHelper helper) {
        LivingEntity mob = vampireMob(helper);
        ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
        ItemEntity flesh = dropOf(helper, new ItemStack(Items.ROTTEN_FLESH));
        dies(helper, mob, bottle, flesh);
        BloodSource s = BloodStamp.read(bottle.getItem()).orElse(null);
        helper.assertTrue(s != null && s.kind() == BloodKind.COMMON_VAMPIRE && s.id().equals(mob.getUUID()) && s.npc(), "the drop is stamped " + s);
        helper.assertTrue(mob.getName().getString().equals(bottle.getItem().get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())), "the label is not the mob's name");
        helper.assertTrue(!flesh.getItem().has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()), "another drop was stamped");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anNpcOriginalsDropIsStampedAsItsKind(GameTestHelper helper) {
        for (NpcOriginal o : List.of(NpcOriginal.MIKAEL, NpcOriginal.KLAUS)) {
            LivingEntity mob = vampireMob(helper);
            mob.setData(SpeciesRegistries.ORIGINAL_ROOT, o.id());
            ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
            dies(helper, mob, bottle);
            BloodKind want = o == NpcOriginal.KLAUS ? BloodKind.ORIGINAL_HYBRID : BloodKind.ORIGINAL;
            helper.assertTrue(BloodStamp.kindOf(bottle.getItem()) == want && "?".equals(bottle.getItem().get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get())),
                    o + ": the drop is " + BloodStamp.kindOf(bottle.getItem()));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noBottleFromAHuman(GameTestHelper helper) {
        LivingEntity zombie = EntityType.ZOMBIE.create(helper.getLevel());
        ItemEntity bottle = dropOf(helper, new ItemStack(bottleItem()));
        dies(helper, zombie, bottle);
        helper.assertTrue(!bottle.getItem().has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()), "a zombie's bottle was stamped");
        ServerPlayer player = TestPlayers.createMortal(helper);
        ItemEntity mine = dropOf(helper, new ItemStack(bottleItem()));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(player, helper.getLevel().damageSources().generic(), new ArrayList<>(List.of(mine)), true));
        helper.assertTrue(!mine.getItem().has(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()), "a player's bottle was stamped");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siresBloodKeepsItsNameAndSyncsNoSource(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ItemStack sires = SiresBloodItem.of(id, "Damon");
        ItemStack client = overTheWire(helper, sires, null);
        helper.assertTrue("Damon".equals(client.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT.get())), "the name was not kept");
        helper.assertTrue(Util.NIL_UUID.equals(client.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get())), "the source was synced");
        helper.assertTrue(id.equals(SiresBloodItem.sourceOf(sires)), "the server lost the source");
        helper.assertTrue(!sires.has(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get()), "Sire's Blood was labelled as drinkable blood");
        helper.succeed();
    }

    // ---- drinking ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unstampedBottleMarksAHuman(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ServerPlayer reference = TestPlayers.createMortal(helper);
        try {
            net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler.drankVampireBlood(reference);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem()));
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(BloodlineAccess.get(p).equals(BloodlineAccess.get(reference)) && BloodlineAccess.get(p).vampireBloodUntil() != 0,
                    "state " + BloodlineAccess.get(p) + " is not what the old drink gave: " + BloodlineAccess.get(reference));
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT), "common blood named a sire");
        } finally {
            reset(p, reference);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalStampNamesThePendingSire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        UUID original = UUID.randomUUID();
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, stamped(BloodKind.ORIGINAL, original, "Elena"));
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && original.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE))
                    && "ORIGINAL".equals(p.getData(SpeciesRegistries.BLOOD_SOURCE_SPECIES)), "the blood is not the pending sire");
            helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() != 0, "an Original's blood does not also mark (Q12)");
            helper.assertTrue(Lines.shownName(helper.getLevel().getServer(), p, original).getContents() instanceof TranslatableContents t && t.getKey().equals("supernaturalbloodlines.line.unknown_original"),
                    "an undiscovered line is named");
            p.hurt(helper.getLevel().damageSources().generic(), 1000f);
            helper.assertTrue(BloodlineAccess.get(p).inTurning() && original.equals(BloodlineAccess.get(p).pendingSire()), "the pending sire is " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalBloodItemMarksAHumanToo(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        UUID original = UUID.randomUUID();
        try {
            ItemStack blood = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
            BloodStamp.write(blood, new BloodSource(original, "Elena", false, BloodKind.ORIGINAL));
            p.setItemInHand(InteractionHand.MAIN_HAND, blood.finishUsingItem(helper.getLevel(), p));
            helper.assertTrue(BloodlineAccess.get(p).vampireBloodUntil() != 0 && original.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE)), "one bottle is not enough (Q12)");
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireAndHunterKeepTheBottle(GameTestHelper helper) {
        ServerPlayer vampire = TestPlayers.createMortal(helper);
        ServerPlayer hunter = TestPlayers.createMortal(helper);
        try {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
            helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
            for (ServerPlayer p : List.of(vampire, hunter)) {
                ItemStack bottle = stamped(BloodKind.COMMON_VAMPIRE, UUID.randomUUID(), "Bob");
                p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
                helper.assertTrue(!click(p), p.getName().getString() + "'s click was taken over");
                helper.assertTrue(p.getMainHandItem() == bottle && bottle.getCount() == 1, "the bottle was used up");
                helper.assertTrue(BloodBottles.decide(p, BloodBottles.sourceOf(bottle)) == BloodBottleRules.Effect.REFUSED, "the drink is not refused");
            }
            helper.assertTrue(BloodlineAccess.get(vampire).vampireBloodUntil() == 0 && BloodlineAccess.get(hunter).vampireBloodUntil() == 0, "someone was marked");
        } finally {
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
            Component again = BottleBloodAction.INSTANCE.trigger(p);
            helper.assertTrue(again != null && again.getContents() instanceof TranslatableContents t && t.getKey().equals(BottleBloodAction.COOLDOWN), "a second bottle came at once");
            helper.assertTrue(glass(p) == 1, "the refused action used a bottle");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodNeverKills(GameTestHelper helper) {
        ServerPlayer p = vampireWithBottle(helper, Species.HERETIC, true);
        try {
            p.setHealth(2f);
            helper.assertTrue(BottleBloodAction.INSTANCE.trigger(p) == null && p.getHealth() == 1f && p.isAlive(), "health is " + p.getHealth());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bottleBloodRefusesOriginalsLikeAnEmptyPlayer(GameTestHelper helper) {
        ServerPlayer noGlass = vampireWithBottle(helper, Species.HYBRID, false);
        List<ServerPlayer> originals = List.of(vampireWithBottle(helper, Species.ORIGINAL, true), vampireWithBottle(helper, Species.ORIGINAL_HYBRID, true));
        try {
            Component empty = BottleBloodAction.INSTANCE.trigger(noGlass);
            helper.assertTrue(empty != null && empty.getContents() instanceof TranslatableContents e && e.getKey().equals(BottleBloodAction.NOTHING), "an empty-handed hybrid heard " + empty);
            for (ServerPlayer o : originals) {
                Component said = BottleBloodAction.INSTANCE.trigger(o);
                helper.assertTrue(said != null && said.equals(empty), "an Original heard " + said);
                helper.assertTrue(!BottleBloodAction.INSTANCE.available(o), "an Original is offered the action");
                helper.assertTrue(o.getHealth() == 20f && glass(o) == 2 && found(o).isEmpty() && ActionClock.ready(o, ExtraActions.BOTTLE_BLOOD), "an Original paid for nothing");
            }
        } finally {
            reset(noGlass);
            originals.forEach(o -> reset(o));
        }
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
