package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SiresBlood;

/** The Rite of the Broken Line (R125) and the data of both line rites. The Imprisoned rite's world is in {@link LineRiteRingGameTests}. */
@PrefixGameTestTemplate(false)
public final class LineRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    static final String BROKEN = "rite_of_the_broken_line";
    static final String IMPRISONED = "rite_of_the_imprisoned_original";
    static final String OTHERWHERE = "enchanted:spirit_of_otherwhere";
    static final String ASH = "supernaturalbloodlines:white_oak_ash";

    private LineRiteGameTests() {
    }

    /** What the hub is told of a rite is what the rite file says: the creatures it takes, the full moon, the chalk of its circle. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hubKnowsTheSacrificesTheMoonAndTheChalk(GameTestHelper helper) {
        var rites = net.ofatech.supernaturalbloodlines.parts.species.SpeciesHub.rites(helper.getLevel().getServer());
        var immortality = rites.stream().filter(r -> r.id().equals(NS + ":immortality")).findFirst().orElse(null);
        helper.assertTrue(immortality != null, "immortality is not in the rites sent to the hub");
        helper.assertTrue(immortality.sacrifices().equals(List.of("minecraft:villager")), "sacrifices " + immortality.sacrifices());
        helper.assertTrue(immortality.fullMoon(), "immortality is a full-moon rite");
        helper.assertTrue(immortality.shapes().equals(List.of("enchanted:medium_circle=enchanted:ritual_chalk")), "shapes " + immortality.shapes());
        var severance = rites.stream().filter(r -> r.id().equals(NS + ":rite_of_severance")).findFirst().orElse(null);
        helper.assertTrue(severance != null && severance.fullMoon(), "Severance is a full-moon rite and its file says so");
        var imprisoned = rites.stream().filter(r -> r.id().equals(NS + ":" + IMPRISONED_ID)).findFirst().orElse(null);
        // the Imprisoned rite also needs the artifacts part (its mountain ash), so it is only there when that is on
        helper.assertTrue(imprisoned == null || imprisoned.circles().get(0).shape().equals("large_circle"), "the Imprisoned rite is drawn in the large circle");
        helper.assertTrue(rites.stream().filter(r -> r.id().endsWith("hybrid_blood") || r.id().endsWith("original_blood")).noneMatch(r -> r.fullMoon()), "a rite with no moon says none");
        helper.succeed();
    }

    private static final String IMPRISONED_ID = "rite_of_the_imprisoned_original";

    private static com.google.gson.JsonObject json(String s) {
        return com.google.gson.JsonParser.parseString(s).getAsJsonObject();
    }

    /** Severance and the Alpha Trial are full-moon rites unless their file says otherwise: an old file or a datapack override without the key keeps the rule. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void fullMoonRitesWithoutTheKeyStillNeedTheMoon(GameTestHelper helper) {
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        var severance = net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory.CODEC.codec();
        var trial = net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory.CODEC.codec();
        helper.assertTrue(severance.parse(ops, json("{}")).getOrThrow().requiresFullMoon(), "Severance without the key");
        helper.assertTrue(trial.parse(ops, json("{\"faction\":\"werewolf\"}")).getOrThrow().requiresFullMoon(), "the Alpha Trial without the key");
        helper.assertTrue(!severance.parse(ops, json("{\"requires_full_moon\":false}")).getOrThrow().requiresFullMoon(), "Severance with the key off");
        helper.assertTrue(!trial.parse(ops, json("{\"requires_full_moon\":false}")).getOrThrow().requiresFullMoon(), "the Alpha Trial with the key off");
        // the hub learns of the moon from the encoded factory: a true that equals the default must be written
        helper.assertTrue(severance.encodeStart(ops, new net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory(true)).getOrThrow().getAsJsonObject().get("requires_full_moon").getAsBoolean(), "Severance is written back");
        helper.assertTrue(trial.encodeStart(ops, new net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrialRiteFactory("werewolf", true)).getOrThrow().getAsJsonObject().get("requires_full_moon").getAsBoolean(), "the Alpha Trial is written back");
        helper.succeed();
    }

    /** The circle, a witch at its edge, a player Original and a hybrid of their line, all "online". */
    static final class Rig {
        final GameTestHelper helper;
        final LineTestPlayer caster;
        final LineTestPlayer original;
        final LineTestPlayer target;

        Rig(GameTestHelper helper, Species casterSpecies) {
            this.helper = helper;
            caster = LineTestPlayer.create(helper);
            species(caster, casterSpecies);
            caster.setPos(Vec3.atBottomCenterOf(centre(helper)).add(3, 1, 0));
            helper.getLevel().addNewPlayer(caster);
            original = LineTestPlayer.create(helper);
            species(original, Species.ORIGINAL);
            target = LineTestPlayer.create(helper);
            species(target, Species.HYBRID);
            SireRecorder.record(original, target);
            online(original, target);
        }

        LineTestPlayer caster() {
            return caster;
        }

        MinecraftServer server() {
            return helper.getLevel().getServer();
        }

        LineIndex lines() {
            return LineIndex.get(server());
        }

        ItemStack bloodOf(ServerPlayer p) {
            return SiresBloodItem.of(p.getUUID(), p.getGameProfile().getName());
        }

        List<ItemStack> brokenLineItems() {
            return List.of(bloodOf(original), bloodOf(target), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)));
        }

        List<ItemStack> imprisonedItems() {
            return List.of(bloodOf(target), new ItemStack(item(ASH), 3), new ItemStack(item(OTHERWHERE), 2));
        }

        void close(ServerPlayer... others) {
            remove(helper, caster);
            for (ServerPlayer p : new ServerPlayer[] {caster, original, target}) {
                lines().remove(p.getUUID());
            }
            for (ServerPlayer p : others) {
                lines().remove(p.getUUID());
            }
            cleanUp(caster, original, target);
            cleanUp(others);
        }
    }

    /** An eclipse that is surely running: at night of the current day, restored afterwards. */
    static void eclipse(GameTestHelper helper, Runnable body) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        long before = overworld.getDayTime();
        overworld.setDayTime(before - Math.floorMod(before, 24000L) + 14000L);
        try {
            LunarEclipse.start(server);
            helper.assertTrue(LunarEclipse.active(server), "control: the eclipse did not start");
            body.run();
        } finally {
            LunarEclipse.stop(server);
            overworld.setDayTime(before);
        }
    }

    private static boolean skipWithoutArtifacts(GameTestHelper helper) {
        if (type(helper, BROKEN) == null) {
            helper.succeed(); // the rites need the white oak ash of the artifacts part
            return true;
        }
        return false;
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineDataLoads(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        var rite = type(helper, BROKEN);
        helper.assertTrue(rite.getPower() == 12000, "power is " + rite.getPower());
        helper.assertTrue(rite.getItems().stream().filter(s -> s.is(SpeciesRegistries.SIRES_BLOOD.get())).mapToInt(ItemStack::getCount).sum() == 2
                && rite.getItems().stream().anyMatch(s -> s.is(item(ASH)) && s.getCount() == 1)
                && rite.getItems().stream().anyMatch(s -> s.is(item(OTHERWHERE)) && s.getCount() == 1), "ingredients are " + rite.getItems());
        helper.assertTrue(rite.getShapes().size() == 1, "it should have one circle");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedDataLoads(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        var rite = type(helper, IMPRISONED);
        helper.assertTrue(rite != null, "the rite did not load");
        helper.assertTrue(rite.getPower() == 36000, "power is " + rite.getPower());
        helper.assertTrue(rite.getItems().stream().anyMatch(s -> s.is(SpeciesRegistries.SIRES_BLOOD.get()) && s.getCount() == 1)
                && rite.getItems().stream().anyMatch(s -> s.is(item(ASH)) && s.getCount() == 3)
                && rite.getItems().stream().anyMatch(s -> s.is(item(OTHERWHERE)) && s.getCount() == 2), "ingredients are " + rite.getItems());
        helper.assertTrue(rite.getShapes().size() == 1 && rite.getShapes().keySet().stream().anyMatch(k -> k.toString().contains("large_circle")),
                "the Imprisoned rite needs the large circle: " + rite.getShapes());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedCostsMoreThanBrokenLine(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        helper.assertTrue(type(helper, IMPRISONED).getPower() > type(helper, BROKEN).getPower(), "the harder rite is not the costlier");
        helper.succeed();
    }

    // ---- the effect ----

    private static void perform(GameTestHelper helper, Rig rig) {
        start(helper, BROKEN, rig.caster(), rig.brokenLineItems());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineFreesTheTargetAndKeepsTheSire(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        try {
            eclipse(helper, () -> perform(helper, rig));
            UUID t = rig.target.getUUID();
            helper.assertTrue(rig.lines().rootOf(t).filter(t::equals).isPresent(), "the target is not their own head");
            helper.assertTrue(rig.lines().freedOf(t).filter(f -> f.from().equals(rig.original.getUUID()) && f.kind() == LineIndex.Kind.BROKEN_LINE).isPresent(),
                    "the freed record is " + rig.lines().freedOf(t));
            helper.assertTrue(rig.original.getUUID().equals(BloodlineAccess.get(rig.target).sire()), "the sire bond was lost");
            helper.assertTrue(!BloodlineAccess.get(rig.target).bondBroken(), "the sire bond was broken, not just the line");
            helper.assertTrue(rig.original.getUUID().equals(BloodlineAccess.get(rig.target).knownLine()), "the target did not learn which Original it was");
            helper.assertTrue(dropped(helper, OTHERWHERE) == 0, "the ingredients came back from a good rite");
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineDoesNotMakeAnOriginal(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        try {
            eclipse(helper, () -> perform(helper, rig));
            helper.assertTrue(BloodlineAccess.get(rig.target).species() == Species.HYBRID, "the species changed to " + BloodlineAccess.get(rig.target).species());
            helper.assertTrue(rig.lines().membersOf(rig.target.getUUID()).isEmpty(), "the head has members of their own line from nowhere");
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isPresent(), "control: the rite did not run");
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineLeavesTheTargetsSiredInTheLine(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        LineTestPlayer u = LineTestPlayer.create(helper);
        species(u, Species.HYBRID);
        try {
            SireRecorder.record(rig.target, u);
            helper.assertTrue(rig.lines().rootOf(u.getUUID()).filter(rig.original.getUUID()::equals).isPresent(), "control: their sired is not in the line");
            eclipse(helper, () -> perform(helper, rig));
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isPresent(), "control: the rite did not run");
            helper.assertTrue(rig.lines().rootOf(u.getUUID()).filter(rig.original.getUUID()::equals).isPresent(), "the target's sired left the Original's line");
        } finally {
            rig.close(u);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineWorksAgainstAPlayerOriginal(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        try {
            // the Original's blood is taken from them with a silver blade, like a player would
            ServerPlayer taker = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createDuelist(helper);
            taker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SpeciesRegistries.SILVER_DAGGER.get()));
            taker.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
            helper.assertTrue(SiresBlood.take(taker, rig.original), "the blood was not taken");
            ItemStack blood = taker.getInventory().items.stream().filter(s -> s.is(SpeciesRegistries.SIRES_BLOOD.get())).findFirst().orElseThrow();
            eclipse(helper, () -> start(helper, BROKEN, rig.caster(),
                    List.of(blood, rig.bloodOf(rig.target), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)))));
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isPresent(), "a player Original's blood did not work");
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineWorksWithAnNpcOriginalsBlood(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        UUID klaus = NpcOriginal.KLAUS.uuid();
        try {
            rig.lines().setRoot(rig.target.getUUID(), klaus, true);
            eclipse(helper, () -> start(helper, BROKEN, rig.caster(), List.of(SiresBloodItem.of(klaus, "Klaus"), rig.bloodOf(rig.target),
                    new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)))));
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).filter(f -> f.from().equals(klaus)).isPresent(), "an NPC Original's blood did not work");
            helper.assertTrue(klaus.equals(BloodlineAccess.get(rig.target).knownLine()), "the target did not learn Klaus");
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    // ---- refusals ----

    private static void refused(GameTestHelper helper, Species performer, boolean withEclipse, Function<Rig, List<ItemStack>> items, String reason) {
        Rig rig = new Rig(helper, performer);
        Runnable run = () -> start(helper, BROKEN, rig.caster(), items.apply(rig));
        try {
            if (withEclipse) {
                eclipse(helper, run);
            } else {
                run.run();
            }
            String key = LineRules.REFUSAL_PREFIX + reason;
            helper.assertTrue(rig.caster().received(key), "the caster was not told " + key + ": " + rig.caster().heard());
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isEmpty(), "a refused rite freed the target");
            helper.assertTrue(dropped(helper, OTHERWHERE) == 1, "the rare ingredient was not given back");
        } finally {
            rig.close();
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineRefusals(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        refused(helper, Species.HYBRID, true, Rig::brokenLineItems, "not_witch");
        refused(helper, Species.WITCH, false, Rig::brokenLineItems, "no_eclipse");
        refused(helper, Species.WITCH, true, r -> List.of(r.bloodOf(r.target), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE))), "no_blood");
        refused(helper, Species.WITCH, true, r -> List.of(r.bloodOf(r.target), SiresBloodItem.of(UUID.randomUUID(), "Stranger"),
                new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE))), "fails");
        // nobody whose blood is bound is here
        refused(helper, Species.WITCH, true, r -> {
            online();
            return r.brokenLineItems();
        }, "target_away");
        refused(helper, Species.WITCH, true, r -> {
            species(r.target, Species.ORIGINAL);
            r.lines().setRoot(r.target.getUUID(), r.original.getUUID(), true); // in the Original's line on paper
            return r.brokenLineItems();
        }, "target_original");
        SpeciesConfig.overrideLineRites(false);
        try {
            refused(helper, Species.WITCH, true, Rig::brokenLineItems, "rites_off");
        } finally {
            SpeciesConfig.overrideLineRites(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFreedHeadCannotBeFreedAgainFromTheSameOriginal(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        try {
            eclipse(helper, () -> {
                perform(helper, rig);
                helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isPresent(), "control: the first rite did not run");
                perform(helper, rig);
            });
            helper.assertTrue(rig.caster().received(LineRules.FAILS), "the second rite was not refused: " + rig.caster().heard());
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineDeathDoesNotReachTheFreed(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        LineTestPlayer u = LineTestPlayer.create(helper);
        species(u, Species.HYBRID);
        try {
            SireRecorder.record(rig.target, u);
            online(rig.original, rig.target, u);
            eclipse(helper, () -> perform(helper, rig));
            LineDeath.run(rig.server(), rig.original.getUUID());
            helper.assertTrue(!rig.target.isDeadOrDying(), "the freed target died with the Original");
            helper.assertTrue(u.isDeadOrDying(), "the target's sired, still in the line, survived");
        } finally {
            rig.close(u);
        }
        helper.succeed();
    }

    /** A wrong blood and an unrelated pair say the same thing, and the performer's and target's own checks come first (no oracle for who is in a line). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineGivesOnePlainRefusalForEveryWrongBlood(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        // two unrelated players, one of them with a different Original: not a pair
        refused(helper, Species.WITCH, true, r -> {
            LineTestPlayer other = LineTestPlayer.create(helper);
            species(other, Species.HYBRID);
            r.lines().setRoot(other.getUUID(), NpcOriginal.ELIJAH.uuid(), true);
            online(r.original, r.target, other);
            return List.of(r.bloodOf(other), r.bloodOf(r.target), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)));
        }, "fails");
        // a freed head is a root but not an Original: their blood with a member's is not a pair
        refused(helper, Species.WITCH, true, r -> {
            LineTestPlayer member = LineTestPlayer.create(helper);
            species(member, Species.HYBRID);
            r.lines().setRoot(member.getUUID(), r.target.getUUID(), true); // the "head"'s own line
            online(r.original, r.target, member);
            return List.of(r.bloodOf(r.target), r.bloodOf(member), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)));
        }, "fails");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void doneMessageGoesToChat(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        Rig rig = new Rig(helper, Species.WITCH);
        try {
            eclipse(helper, () -> perform(helper, rig));
            helper.assertTrue(rig.target.received(net.ofatech.supernaturalbloodlines.parts.species.rite.BrokenLineRiteFactory.DONE), "the target was not told: " + rig.target.heard());
            helper.assertTrue(rig.target.chat() >= 1, "the message did not go to chat");
        } finally {
            rig.close();
        }
        helper.succeed();
    }

    /** The Original is here and the other blood is not: whether that other blood is in the Original's line must not show in the answer. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void brokenLineAnswersTheSameForAnAbsentTargetInTheLineOrNot(GameTestHelper helper) {
        if (skipWithoutArtifacts(helper)) {
            return;
        }
        // B is in A's line and away
        refused(helper, Species.WITCH, true, r -> {
            online(r.original);
            return r.brokenLineItems();
        }, "fails");
        // B is not in A's line, and away
        refused(helper, Species.WITCH, true, r -> {
            LineTestPlayer stranger = LineTestPlayer.create(helper);
            species(stranger, Species.HYBRID);
            online(r.original);
            return List.of(r.bloodOf(r.original), r.bloodOf(stranger), new ItemStack(item(ASH)), new ItemStack(item(OTHERWHERE)));
        }, "fails");
        helper.succeed();
    }
}
