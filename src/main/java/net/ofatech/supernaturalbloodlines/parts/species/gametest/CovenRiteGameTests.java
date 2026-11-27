package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circleAt;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.tidy;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteManager;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.coven.GrandRites;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.LinkWindow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.rite.NightWatchRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;

/** Plan 20b Task 9 (R254, R255, R264 to R268): the three grand rites and what gates them. A cast runs the cost hook around {@code Rite.start()} as the chalk does. */
@PrefixGameTestTemplate(false)
public final class CovenRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<String> KEYS = List.of("grand_rites_enabled", "grand_rite_cooldown_hours", "grand_rite_radius", "fete_minutes", "harvest_xp_levels", "harvest_max_blocks");
    private static final String CIRCLE = "supernaturalbloodlines.rite.grand.circle";
    private static final String COOLDOWN = "supernaturalbloodlines.rite.grand.cooldown";

    private CovenRiteGameTests() {
    }

    private static List<ItemStack> feteItems() {
        return List.of(new ItemStack(item("minecraft:gold_ingot"), 4), new ItemStack(item("minecraft:emerald"), 4), new ItemStack(item("minecraft:honey_bottle"), 2));
    }

    private static List<ItemStack> nightItems() {
        return List.of(new ItemStack(item("minecraft:lantern"), 4), new ItemStack(item("minecraft:iron_ingot"), 4), new ItemStack(item("minecraft:bell"), 1));
    }

    private static List<ItemStack> harvestItems() {
        return List.of(new ItemStack(item("minecraft:wheat"), 16), new ItemStack(item("minecraft:carrot"), 16), new ItemStack(item("minecraft:potato"), 16),
                new ItemStack(item("minecraft:beetroot"), 16));
    }

    /** The cost hook, the rite and its start, then the commit: what a chalk's tick does for a rite that has paid. */
    public static Rite cast(GameTestHelper helper, Circle c, String id, List<ItemStack> items) {
        ServerLevel level = helper.getLevel();
        RiteType type = type(helper, id);
        RiteCost.begin(level, c.anchor().getUUID(), c.chalk(), c.chalkEntity().getPosHolder(), type.getPower(), true, type);
        Rite rite = type.create(level, c.chalk(), c.anchor().getUUID(), new ArrayList<>(items));
        RiteManager.addRite(level, rite);
        try {
            rite.start();
        } finally {
            RiteCost.commit();
        }
        return rite;
    }

    private static void done(GameTestHelper helper, Circle c) {
        LinkWindow.forget(c.anchor().getUUID());
        KEYS.forEach(k -> GroupsConfig.override(k, null));
        Rite left = RiteManager.getRiteAt(helper.getLevel(), c.chalk());
        if (left != null) {
            left.stop();
            RiteManager.removeRite(helper.getLevel(), left);
        }
        clearDropped(helper);
        PackIndex.overrideClock(null);
        tidy(helper, c.array());
    }

    private static void atNight(GameTestHelper helper, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        level.setDayTime(before - Math.floorMod(before, 24000L) + 14000);
        try {
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    private static void refill(GameTestHelper helper, Circle c) {
        for (TalkingDuelist p : c.all()) {
            GroupIndex.get(helper.getLevel()).setChannel(p.getUUID(), 99999, 0, 99999);
        }
    }

    private static boolean told(TalkingDuelist p, String key) {
        return p.receivedOnActionBar(key);
    }

    private static ServerPlayer outsider(GameTestHelper helper, Circle c, double dx) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.setPos(c.chalk().getX() + 0.5 + dx, c.chalk().getY() + 1, c.chalk().getZ() + 0.5);
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    private static void drop(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSoloWitchCannotCastAGrandRite(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 2);
        try {
            net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.dropMember(helper.getLevel().getServer(), c.others().get(0).getUUID(), false);
            for (var rite : List.of("fete_des_benedictions", "night_watch", "great_harvest")) {
                atNight(helper, () -> cast(helper, c, rite, feteItems()));
                helper.assertTrue(told(c.anchor(), CIRCLE), rite + ": no refusal for a solo witch: " + c.anchor().countOnActionBar(CIRCLE));
                helper.assertTrue(dropped(helper, "minecraft:gold_ingot") == 4, rite + ": the items did not come back");
                clearDropped(helper);
            }
            helper.assertTrue(GroupIndex.get(helper.getLevel()).channelOf(c.anchor().getUUID()) == null, "a solo cast charged the meter");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfThreeCastsTheFete(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        ServerPlayer near = outsider(helper, c, 10);
        ServerPlayer edge = outsider(helper, c, 40);
        ServerPlayer far = outsider(helper, c, 80);
        try {
            cast(helper, c, "fete_des_benedictions", feteItems());
            for (TalkingDuelist m : c.all()) {
                helper.assertTrue(m.hasEffect(MobEffects.LUCK) && m.hasEffect(MobEffects.REGENERATION), "a member is not blessed");
            }
            helper.assertTrue(near.hasEffect(MobEffects.LUCK) && near.hasEffect(MobEffects.REGENERATION), "an outsider within the reach is not blessed");
            helper.assertTrue(!edge.hasEffect(MobEffects.LUCK), "an outsider at 40 blocks is blessed: the reach is 33 for a circle of three");
            helper.assertTrue(!far.hasEffect(MobEffects.LUCK), "an outsider at 80 blocks is blessed");
            helper.assertTrue(RiteManager.getRiteAt(helper.getLevel(), c.chalk()) == null, "the instant rite stayed");
        } finally {
            drop(helper, near, edge, far);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFeteLengthFollowsPotency(GameTestHelper helper) {
        Circle three = circleAt(helper, new BlockPos(8, 1, 8), 3);
        int lengthThree;
        try {
            cast(helper, three, "fete_des_benedictions", feteItems());
            lengthThree = three.anchor().getEffect(MobEffects.LUCK).getDuration();
        } finally {
            done(helper, three);
        }
        helper.assertTrue(lengthThree == 16320, "ten minutes at the potency of three (1.36) is 16320 ticks, not " + lengthThree);
        GroupsConfig.override("fete_minutes", 20);
        Circle again = circleAt(helper, new BlockPos(8, 1, 8), 3);
        try {
            cast(helper, again, "fete_des_benedictions", feteItems());
            helper.assertTrue(again.anchor().getEffect(MobEffects.LUCK).getDuration() == 32640, "the config minutes do not scale the length: " + again.anchor().getEffect(MobEffects.LUCK).getDuration());
        } finally {
            done(helper, again);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfSixCannotCastNightWatchButSevenCan(GameTestHelper helper) {
        Circle six = circleAt(helper, new BlockPos(8, 1, 8), 6);
        try {
            atNight(helper, () -> cast(helper, six, "night_watch", nightItems()));
            helper.assertTrue(told(six.anchor(), CIRCLE) && RiteManager.getRiteAt(helper.getLevel(), six.chalk()) == null, "six cast Night Watch");
            helper.assertTrue(dropped(helper, "minecraft:bell") == 1, "the items did not come back");
        } finally {
            done(helper, six);
        }
        Circle seven = circleAt(helper, new BlockPos(8, 1, 8), 7);
        try {
            atNight(helper, () -> cast(helper, seven, "night_watch", nightItems()));
            helper.assertTrue(!told(seven.anchor(), CIRCLE) && RiteManager.getRiteAt(helper.getLevel(), seven.chalk()) != null, "seven could not cast Night Watch");
        } finally {
            done(helper, seven);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfSevenThatLosesAMemberAtTheStartIsTooSmall(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        try {
            TalkingDuelist hurt = c.others().get(2);
            hurt.setHealth(hurt.getMaxHealth() * 0.4f);
            atNight(helper, () -> cast(helper, c, "night_watch", nightItems()));
            helper.assertTrue(told(c.anchor(), CIRCLE) && RiteManager.getRiteAt(helper.getLevel(), c.chalk()) == null, "a circle of seven with one member too hurt to pay cast Night Watch");
            helper.assertTrue(dropped(helper, "minecraft:bell") == 1, "the items did not come back");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nightWatchSlowsAndGlowsMonstersUntilDawn(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        Zombie far = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        far.setPos(far.getX() + 200, far.getY(), far.getZ());
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 5));
        long before = level.getDayTime();
        try {
            atNight(helper, () -> {
                cast(helper, c, "night_watch", nightItems());
                helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) != null, "the rite does not run");
                RiteManager.tick(level);
                helper.assertTrue(near.hasEffect(MobEffects.GLOWING) && near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the monster is not outlined and slowed");
                helper.assertTrue(!far.hasEffect(MobEffects.GLOWING), "a monster 200 blocks away was touched");
                helper.assertTrue(!cow.hasEffect(MobEffects.GLOWING), "a cow was touched");
                helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) != null, "the rite ended at night");
            });
            level.setDayTime(before - Math.floorMod(before, 24000L) + 23500);
            RiteManager.tick(level);
            helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) == null, "the rite did not end at dawn");
        } finally {
            level.setDayTime(before);
            near.discard();
            far.discard();
            cow.discard();
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSavedNightWatchExpiresEvenWhenItsChalkIsStillUnloaded(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        try {
            atNight(helper, () -> {
                var saved = cast(helper, c, "night_watch", nightItems()).save();
                BlockPos unloaded = c.chalk().offset(4096, 0, 0);
                helper.assertTrue(!level.hasChunkAt(unloaded), "setup: the chalk chunk is loaded");
                var loaded = type(helper, "night_watch").create(level, unloaded);
                loaded.load(saved);
                RiteManager.addRite(level, loaded);
                helper.assertTrue(loaded.tick(), "the watch did not survive a reload on the same night");
                level.setDayTime(level.getDayTime() + 24_000);
                RiteManager.tick(level);
                helper.assertTrue(RiteManager.getRiteAt(level, unloaded) == null && !level.hasChunkAt(unloaded), "the watch missed dawn or loaded the chalk to expire");
                level.getChunkAt(unloaded);
                RiteManager.tick(level);
                helper.assertTrue(RiteManager.getRiteAt(level, unloaded) == null, "an expired watch came back when its chunk loaded");
            });
        } finally {
            level.setDayTime(before);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNightWatchSavedBeforeItKeptItsDawnEndsAtTheNextDawn(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        try {
            atNight(helper, () -> {
                var saved = cast(helper, c, "night_watch", nightItems()).save();
                saved.remove("dawn");
                BlockPos unloaded = c.chalk().offset(4096, 0, 0);
                var loaded = type(helper, "night_watch").create(level, unloaded);
                loaded.load(saved);
                helper.assertTrue(loaded.tick(), "an older save with no dawn ended at once when loaded at night");
                level.setDayTime(level.getDayTime() - Math.floorMod(level.getDayTime(), 24_000L) + net.ofatech.supernaturalbloodlines.core.moon.MoonRule.NIGHT_END);
                helper.assertTrue(!loaded.tick(), "an older save with no dawn did not end at the next dawn");
            });
        } finally {
            level.setDayTime(before);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nightWatchByDayIsRefusedBeforeAnythingIsPaidOrStamped(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        level.setDayTime(before - Math.floorMod(before, 24000L) + 6000);
        try {
            cast(helper, c, "night_watch", nightItems());
            helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) == null, "Night Watch was raised by day");
            helper.assertTrue(told(c.anchor(), net.ofatech.supernaturalbloodlines.parts.species.rite.NightWatchRiteFactory.DAY), "no word that it needs the night: " + c.anchor().countOnActionBar(net.ofatech.supernaturalbloodlines.parts.species.rite.NightWatchRiteFactory.DAY));
            helper.assertTrue(dropped(helper, "minecraft:bell") == 1, "the items did not come back");
            for (TalkingDuelist m : c.all()) {
                helper.assertTrue(GroupIndex.get(level).channelOf(m.getUUID()) == null, "a member paid for a rite that never started");
            }
            UUID group = GroupIndex.get(level).existingGroupOf(net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.covenOf(c.anchor())).orElseThrow();
            helper.assertTrue(GroupIndex.get(level).cooldownUntil(group, "night_watch") == 0, "the cooldown was stamped for a rite that never started");
        } finally {
            level.setDayTime(before);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withCovensOffAnExistingCircleLinksNothingAndCastsNoGrandRite(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        ServerLevel level = helper.getLevel();
        try {
            int solo = CovenCostGameTests.soloBill(helper, c, c.anchor().getUUID(), 6000);
            int staged = CovenCostGameTests.begin(helper, c, c.anchor().getUUID(), 6000, true);
            helper.assertTrue(CircleStage.current() != null && CircleStage.size() == 3 && staged < solo, "the control: a circle with covens on should split the bill");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.rite.GrandGate.check(level, c.anchor().getUUID(), "fete_des_benedictions", 0).isEmpty(), "the control: the gate should open for a circle of three");
            GroupsConfig.override("covens_enabled", false);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.rite.GrandGate.check(level, c.anchor().getUUID(), "fete_des_benedictions", 0).orElse("").equals(CIRCLE), "the gate opened for a coven while covens are off");
            RiteCost.discard();
            int off = CovenCostGameTests.begin(helper, c, c.anchor().getUUID(), 6000, true);
            helper.assertTrue(CircleStage.current() == null && off == solo, "a circle was planned while covens are off: altar " + off + ", solo " + solo);
        } finally {
            GroupsConfig.override("covens_enabled", null);
            done(helper, c);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void nightWatchSavesItsRadius(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        try {
            AtomicRadius radius = new AtomicRadius();
            atNight(helper, () -> {
                Rite rite = cast(helper, c, "night_watch", nightItems());
                radius.value = ((NightWatchRiteFactory.NightWatchRite) rite).radius();
                var tag = rite.save();
                Rite loaded = type(helper, "night_watch").create(level, c.chalk());
                loaded.load(tag);
                helper.assertTrue(radius.value == 42 && ((NightWatchRiteFactory.NightWatchRite) loaded).radius() == radius.value,
                        "24 blocks at the potency of seven (1.738) is 42; saved " + radius.value + ", loaded " + ((NightWatchRiteFactory.NightWatchRite) loaded).radius());
            });
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    private static final class AtomicRadius {
        int value;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfTwelveCannotCastGreatHarvest(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 12);
        try {
            for (TalkingDuelist p : c.all()) {
                p.giveExperienceLevels(50);
            }
            plant(helper, 5);
            cast(helper, c, "great_harvest", harvestItems());
            helper.assertTrue(told(c.anchor(), CIRCLE), "twelve cast the Great Harvest");
            helper.assertTrue(aged(helper) == 0, "crops grew");
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(p.experienceLevel == 50, "levels were taken");
            }
            helper.assertTrue(dropped(helper, "minecraft:wheat") == 16, "the items did not come back");
        } finally {
            unplant(helper);
            done(helper, c);
        }
        helper.succeed();
    }

    /** Wheat on farmland in a {@code size} x {@code size} square at the corner of the cell (the chalk is at 8, 1, 8: that spot stays free). */
    private static void plant(GameTestHelper helper, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (x == 8 && z == 8) {
                    continue;
                }
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 1, z)), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void unplant(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (x < 10 && z < 10 && !(x == 8 && z == 8)) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 1, z)), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static int aged(GameTestHelper helper) {
        int n = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 2, z)));
                if (state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0) {
                    n++;
                }
            }
        }
        return n;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatHarvestGrowsCropsAndChargesTheLevels(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 13);
        try {
            for (TalkingDuelist p : c.all()) {
                p.giveExperienceLevels(50);
            }
            plant(helper, 8);
            cast(helper, c, "great_harvest", harvestItems());
            helper.assertTrue(!told(c.anchor(), CIRCLE), "thirteen were refused");
            int paid = c.all().stream().mapToInt(p -> 50 - p.experienceLevel).sum();
            helper.assertTrue(paid == 30, "the levels add up to " + paid + ", not harvest_xp_levels (30)");
            helper.assertTrue(c.all().stream().allMatch(p -> p.experienceLevel == 48 || p.experienceLevel == 47), "the split is not even: " + c.all().stream().map(p -> p.experienceLevel).toList());
            helper.assertTrue(aged(helper) == 64, "every crop in the reach should have grown: " + aged(helper) + " of 64; the crop at the corner is " + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(0, 2, 0))) + ", the chalk at " + c.chalk() + " vs " + helper.absolutePos(new BlockPos(8, 1, 8)));
        } finally {
            unplant(helper);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWithTooFewLevelsRefusesTheHarvestBeforeAnyCharge(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 13);
        try {
            for (TalkingDuelist p : c.all()) {
                p.giveExperienceLevels(50);
            }
            TalkingDuelist poor = c.others().get(5);
            poor.giveExperienceLevels(-50);
            plant(helper, 5);
            cast(helper, c, "great_harvest", harvestItems());
            helper.assertTrue(told(c.anchor(), "supernaturalbloodlines.rite.grand.levels"), "the performer was not told the circle cannot pay");
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(p == poor ? p.experienceLevel == 0 : p.experienceLevel == 50, "somebody was charged: " + p.experienceLevel);
            }
            helper.assertTrue(aged(helper) == 0, "crops grew");
            helper.assertTrue(dropped(helper, "minecraft:carrot") == 16, "the items did not come back");
            helper.assertTrue(GroupIndex.get(helper.getLevel()).channelOf(c.anchor().getUUID()) == null, "a meter was charged");
        } finally {
            unplant(helper);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatHarvestStopsAtTheBlockCap(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 13);
        try {
            GroupsConfig.override("harvest_max_blocks", 64);
            for (TalkingDuelist p : c.all()) {
                p.giveExperienceLevels(50);
            }
            plant(helper, 10);
            cast(helper, c, "great_harvest", harvestItems());
            int grown = aged(helper);
            helper.assertTrue(grown > 0 && grown <= 64, "100 crops and a cap of 64: " + grown + " grew");
        } finally {
            unplant(helper);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatHarvestNeverKills(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 13);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 1, 4));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 6));
        try {
            for (TalkingDuelist p : c.all()) {
                p.giveExperienceLevels(50);
            }
            float cowHealth = cow.getHealth();
            float zombieHealth = zombie.getHealth();
            plant(helper, 6);
            cast(helper, c, "great_harvest", harvestItems());
            helper.assertTrue(cow.isAlive() && cow.getHealth() == cowHealth && zombie.isAlive() && zombie.getHealth() == zombieHealth, "a creature was hurt or taken");
            helper.assertTrue(!cow.isRemoved() && !zombie.isRemoved(), "a creature was removed");
        } finally {
            cow.discard();
            zombie.discard();
            unplant(helper);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownIsPerCovenAndPerRite(GameTestHelper helper) {
        long[] clock = {80_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        Circle a = circleAt(helper, new BlockPos(8, 1, 8), 3);
        try {
            cast(helper, a, "fete_des_benedictions", feteItems());
            helper.assertTrue(a.anchor().hasEffect(MobEffects.LUCK) && !told(a.anchor(), COOLDOWN) && !told(a.anchor(), CIRCLE), "the first cast was refused");
            refill(helper, a);
            cast(helper, a, "fete_des_benedictions", feteItems());
            helper.assertTrue(told(a.anchor(), COOLDOWN), "the same coven cast the same rite twice in a row");
            GroupIndex groups = GroupIndex.get(helper.getLevel());
            UUID group = groups.existingGroupOf(a.anchor().getUUID()).orElseThrow();
            helper.assertTrue(groups.cooldownUntil(group, GrandRites.NIGHT_WATCH) == 0 && groups.cooldownUntil(group, GrandRites.GREAT_HARVEST) == 0, "the cooldown reached another rite");
            clock[0] += GroupsConfig.grandRiteCooldownHours() * 3_600_000L + 1;
            refill(helper, a);
            cast(helper, a, "fete_des_benedictions", feteItems());
            helper.assertTrue(a.anchor().countOnActionBar(COOLDOWN) == 1, "the cooldown did not end");
        } finally {
            done(helper, a);
        }
        PackIndex.overrideClock(() -> clock[0]);
        Circle b = circleAt(helper, new BlockPos(8, 1, 8), 3);
        try {
            cast(helper, b, "fete_des_benedictions", feteItems());
            helper.assertTrue(b.anchor().hasEffect(MobEffects.LUCK) && !told(b.anchor(), COOLDOWN), "another coven was held by the first one's cooldown");
        } finally {
            done(helper, b);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFailedCastDoesNotStartTheCooldown(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 2);
        try {
            cast(helper, c, "fete_des_benedictions", feteItems());
            helper.assertTrue(told(c.anchor(), CIRCLE), "two cast the Fete");
            UUID group = GroupIndex.get(helper.getLevel()).existingGroupOf(c.anchor().getUUID()).orElseThrow();
            helper.assertTrue(GroupIndex.get(helper.getLevel()).cooldownUntil(group, GrandRites.FETE) == 0, "a refused cast started the cooldown");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGrandRitesSwitchClosesAllThree(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 13);
        try {
            GroupsConfig.override("grand_rites_enabled", false);
            atNight(helper, () -> {
                cast(helper, c, "fete_des_benedictions", feteItems());
                cast(helper, c, "night_watch", nightItems());
                cast(helper, c, "great_harvest", harvestItems());
            });
            helper.assertTrue(c.anchor().countOnActionBar("supernaturalbloodlines.rite.grand.off") == 3, "the switch did not close all three");
            helper.assertTrue(c.all().stream().noneMatch(p -> p.hasEffect(MobEffects.LUCK)), "the Fete was cast");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anExistingRiteIsNotGrand(GameTestHelper helper) {
        helper.assertTrue(GrandRites.tierOf("ward_of_daylight") == 0 && GrandRites.tierOf("rite_of_severance") == 0 && GrandRites.tierOf("evolve_great_sage") == 0, "a rite of before is a grand rite");
        var caster = net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer.create(helper);
        net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place(helper, caster);
        try {
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start(helper, "ancestral_calling", caster, List.of());
            helper.assertTrue(net.ofatech.supernaturalbloodlines.core.BloodlineAccess.get(caster).species() != net.ofatech.supernaturalbloodlines.core.bloodline.Species.NONE,
                    "an ordinary rite did not start for a solo caster: " + caster.heard());
        } finally {
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(caster, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE);
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAnchorsDeathStopsNightWatch(GameTestHelper helper) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 7);
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        level.setDayTime(before - Math.floorMod(before, 24000L) + 14000);
        cast(helper, c, "night_watch", nightItems());
        helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) != null, "the rite does not run");
        c.anchor().die(c.anchor().damageSources().genericKill());
        helper.assertTrue(c.others().stream().allMatch(m -> m.getHealth() == m.getMaxHealth() - 12), "the members were not hurt while the watch ran");
        helper.assertTrue(LinkWindow.queuedStops() >= 1 && RiteManager.getRiteAt(level, c.chalk()) != null, "the stop was not queued");
        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(RiteManager.getRiteAt(level, c.chalk()) == null, "Night Watch kept running after its anchor fell");
            } finally {
                level.setDayTime(before);
                done(helper, c);
            }
            helper.succeed();
        });
    }
}
