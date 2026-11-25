package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circleAt;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.tidy;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenRiteGameTests.cast;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;

/** Plan 20b Task 10 (R262, R263): the two ring rites of a circle. Run only when the artifacts part is on (the Moon Ring also needs Werewolves). */
@PrefixGameTestTemplate(false)
public final class CovenRingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String CIRCLE = "supernaturalbloodlines.rite.coven_forge.circle";
    private static final String OFF = "supernaturalbloodlines.rite.coven_forge.off";
    private static final String NOT_FOR_YOU = "supernaturalbloodlines.refusal.coven_forge";
    private static final String NO_EFFECT = "supernaturalbloodlines.rite.no_effect";

    private CovenRingGameTests() {
    }

    private static boolean skip(GameTestHelper helper, boolean wolves) {
        if (!EnabledParts.isEnabled("artifacts") || wolves && !ModList.get().isLoaded("werewolves")) {
            helper.succeed();
            return true;
        }
        return false;
    }

    /** Werewolves' own classes are named only here, so a run without them never loads them. */
    private static final class Wolves {
        static void make(ServerPlayer p) {
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 1);
        }
    }

    private static List<ItemStack> moonItems() {
        return List.of(new ItemStack(item("werewolves:silver_ingot"), 2), new ItemStack(item("minecraft:amethyst_shard"), 1));
    }

    private static List<ItemStack> sunItems() {
        return List.of(new ItemStack(item("minecraft:sunflower"), 2), new ItemStack(item("minecraft:glowstone_dust"), 4), new ItemStack(item("minecraft:gold_ingot"), 2));
    }

    /** The receiver stands on the chalk's centre block. */
    private static TalkingDuelist onCentre(GameTestHelper helper) {
        TalkingDuelist p = GroupTestKit.human(helper, 1);
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(0, 1, 0));
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    private static List<ItemEntity> rings(GameTestHelper helper, String id) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(2), e -> e.getItem().is(item(id)));
    }

    private static UUID boundTo(ItemEntity e) {
        return e.getItem().get(ArtifactItems.BOUND_TO.get());
    }

    private static void done(GameTestHelper helper, Circle c, ServerPlayer... more) {
        for (var k : List.of("moonlight_ring_enabled", "sun_ring_forge_enabled", "sun_ring_day_walker_only")) {
            GroupsConfig.override(k, null);
        }
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(4))) {
            e.discard();
        }
        for (ServerPlayer p : more) {
            GroupIndex.get(helper.getLevel()).clearRanks(p.getUUID());
            PackIndex.get(helper.getLevel()).leave(p.getUUID());
        }
        clearDropped(helper);
        ServerPlayer[] all = new ServerPlayer[c.array().length + more.length];
        System.arraycopy(c.array(), 0, all, 0, c.array().length);
        System.arraycopy(more, 0, all, c.array().length, more.length);
        tidy(helper, all);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfThreeForgesAMoonRingBoundToTheReceiver(GameTestHelper helper) {
        if (skip(helper, true)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist receiver = onCentre(helper);
        TalkingDuelist packLeader = GroupTestKit.human(helper, 3);
        try {
            Wolves.make(receiver);
            PackIndex.get(helper.getLevel()).join(packLeader.getUUID(), receiver.getUUID());
            cast(helper, c, "coven_moon_ring", moonItems());
            List<ItemEntity> made = rings(helper, "supernaturalbloodlines:moon_ring");
            helper.assertTrue(made.size() == 1 && made.get(0).getItem().getCount() == 1, "one ring expected, got " + made.size());
            helper.assertTrue(receiver.getUUID().equals(boundTo(made.get(0))), "the ring is bound to " + boundTo(made.get(0)) + ", not the receiver");
            helper.assertTrue(!receiver.getUUID().equals(c.anchor().getUUID()) && !c.anchor().getUUID().equals(boundTo(made.get(0))), "the ring is the anchor's");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 0, "the silver came back: the rite did not take it");
            helper.assertTrue(type(helper, "coven_moon_ring").getItems().stream().filter(s -> s.is(item("werewolves:silver_ingot"))).mapToInt(ItemStack::getCount).sum() == 2,
                    "the rite takes 2 silver, half of the 4 of the crafting");
        } finally {
            done(helper, c, receiver, packLeader);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfTwoCannotForgeIt(GameTestHelper helper) {
        if (skip(helper, true)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 2);
        TalkingDuelist receiver = onCentre(helper);
        TalkingDuelist packLeader = GroupTestKit.human(helper, 3);
        try {
            Wolves.make(receiver);
            PackIndex.get(helper.getLevel()).join(packLeader.getUUID(), receiver.getUUID());
            cast(helper, c, "coven_moon_ring", moonItems());
            helper.assertTrue(c.anchor().receivedOnActionBar(CIRCLE), "the performer was not told the circle is too small");
            helper.assertTrue(rings(helper, "supernaturalbloodlines:moon_ring").isEmpty(), "a ring was forged");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2 && dropped(helper, "minecraft:amethyst_shard") == 1, "the items did not come back");
        } finally {
            done(helper, c, receiver, packLeader);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfInACovenIsNotEligibleForTheMoonRing(GameTestHelper helper) {
        if (skip(helper, true)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist receiver = onCentre(helper);
        try {
            Wolves.make(receiver);
            PackIndex.get(helper.getLevel()).join(c.anchor().getUUID(), receiver.getUUID());
            cast(helper, c, "coven_moon_ring", moonItems());
            helper.assertTrue(rings(helper, "supernaturalbloodlines:moon_ring").isEmpty()
                    && c.anchor().receivedOnActionBar(NO_EFFECT) && dropped(helper, "werewolves:silver_ingot") == 2,
                    "a werewolf in a coven was treated as a pack member");
        } finally {
            done(helper, c, receiver);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aReceiverOutsideAPackIsRefusedTheSameAsAHuman(GameTestHelper helper) {
        if (skip(helper, true)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist wolf = onCentre(helper);
        try {
            Wolves.make(wolf);
            cast(helper, c, "coven_moon_ring", moonItems());
            helper.assertTrue(rings(helper, "supernaturalbloodlines:moon_ring").isEmpty(), "a ring for a werewolf with no pack");
            helper.assertTrue(c.anchor().receivedOnActionBar(NO_EFFECT) && !c.anchor().receivedOnActionBar(NOT_FOR_YOU), "the performer was told something about the receiver");
            helper.assertTrue(wolf.receivedOnActionBar(NOT_FOR_YOU), "the receiver was not told");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2, "the items did not come back");
        } finally {
            done(helper, c, wolf);
        }
        Circle again = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist human = onCentre(helper);
        try {
            cast(helper, again, "coven_moon_ring", moonItems());
            helper.assertTrue(again.anchor().receivedOnActionBar(NO_EFFECT) && !again.anchor().receivedOnActionBar(NOT_FOR_YOU),
                    "a human receiver was answered differently from a werewolf with no pack");
            helper.assertTrue(again.anchor().countOnActionBar(NO_EFFECT) == 1, "the performer's line is not the same one line");
        } finally {
            done(helper, again, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSunRingBindsAtHalfPower(GameTestHelper helper) {
        if (skip(helper, false)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist vampire = onCentre(helper);
        try {
            helper.assertTrue(type(helper, "coven_sun_ring").getPower() == 3000 && type(helper, "ward_of_daylight").getPower() == 6000, "the file powers are 3000 and 6000");
            int half = CovenCostGameTests.begin(helper, c, c.anchor().getUUID(), 3000, true);
            RiteCost.discard();
            int full = CovenCostGameTests.begin(helper, c, c.anchor().getUUID(), 6000, true);
            RiteCost.discard();
            helper.assertTrue(half > 0 && Math.abs(half * 2 - full) <= 2, "the altar bill at 3000 is " + half + " and at 6000 is " + full);
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            cast(helper, c, "coven_sun_ring", sunItems());
            List<ItemEntity> made = rings(helper, "supernaturalbloodlines:sun_ring");
            helper.assertTrue(made.size() == 1 && vampire.getUUID().equals(boundTo(made.get(0))), "no sun ring bound to the vampire: " + made.size());
        } finally {
            done(helper, c, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainSunRingBindStillCostsSixThousandAndWorksAlone(GameTestHelper helper) {
        if (skip(helper, false)) {
            return;
        }
        helper.assertTrue(type(helper, "ward_of_daylight").getPower() == 6000, "the plain bind costs 6000");
        helper.assertTrue(type(helper, "ward_of_daylight").getItems().stream().anyMatch(s -> s.is(item("supernaturalbloodlines:sun_ring"))), "the plain bind takes the ring");
        if (ModList.get().isLoaded("werewolves")) {
            helper.assertTrue(type(helper, "ward_of_the_moon").getPower() == 6000, "the plain Moon bind changed");
            helper.assertTrue(type(helper, "ward_of_the_moon").getItems().stream().filter(s -> s.is(item("werewolves:silver_ingot"))).mapToInt(ItemStack::getCount).sum() == 2,
                    "the plain Moon bind's items changed");
        }
        TalkingDuelist v = GroupTestKit.vampire(helper, 1);
        try {
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place(helper, v);
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start(helper, "ward_of_daylight", v, List.of(new ItemStack(ArtifactItems.SUN_RING.get()),
                    new ItemStack(item("minecraft:sunflower"), 2), new ItemStack(item("minecraft:glowstone_dust"), 4)));
            List<ItemEntity> made = rings(helper, "supernaturalbloodlines:sun_ring");
            helper.assertTrue(made.size() == 1 && v.getUUID().equals(boundTo(made.get(0))), "a solo bind did not work");
        } finally {
            for (ItemEntity e : rings(helper, "supernaturalbloodlines:sun_ring")) {
                e.discard();
            }
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove(helper, v);
            GroupTestKit.clean(helper, v);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRingSwitchesCloseThem(GameTestHelper helper) {
        if (skip(helper, false)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist vampire = onCentre(helper);
        try {
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            GroupsConfig.override("sun_ring_forge_enabled", false);
            cast(helper, c, "coven_sun_ring", sunItems());
            helper.assertTrue(c.anchor().receivedOnActionBar(OFF) && rings(helper, "supernaturalbloodlines:sun_ring").isEmpty(), "the Sun Ring switch did not close it");
            helper.assertTrue(dropped(helper, "minecraft:gold_ingot") == 2, "the items did not come back");
        } finally {
            done(helper, c, vampire);
        }
        if (ModList.get().isLoaded("werewolves")) {
            Circle again = circleAt(helper, new BlockPos(8, 1, 8), 3);
            TalkingDuelist wolf = onCentre(helper);
            TalkingDuelist leader = GroupTestKit.human(helper, 3);
            try {
                Wolves.make(wolf);
                PackIndex.get(helper.getLevel()).join(leader.getUUID(), wolf.getUUID());
                GroupsConfig.override("moonlight_ring_enabled", false);
                cast(helper, again, "coven_moon_ring", moonItems());
                helper.assertTrue(again.anchor().receivedOnActionBar(OFF) && rings(helper, "supernaturalbloodlines:moon_ring").isEmpty(), "the Moon Ring switch did not close it");
            } finally {
                done(helper, again, wolf, leader);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDayWalkerSwitchMakesItNeedARank(GameTestHelper helper) {
        if (skip(helper, false)) {
            return;
        }
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 3);
        TalkingDuelist vampire = onCentre(helper);
        try {
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            GroupsConfig.override("sun_ring_day_walker_only", true);
            cast(helper, c, "coven_sun_ring", sunItems());
            helper.assertTrue(rings(helper, "supernaturalbloodlines:sun_ring").isEmpty() && vampire.receivedOnActionBar(NOT_FOR_YOU), "a vampire with no rank got the ring");
            helper.assertTrue(c.anchor().receivedOnActionBar(NO_EFFECT), "the performer was not told the generic line");
            clearDropped(helper);
            GroupIndex.get(helper.getLevel()).setRank(vampire.getUUID(), GroupRank.DAY_WALKER);
            net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage.set(null);
            cast(helper, c, "coven_sun_ring", sunItems());
            helper.assertTrue(rings(helper, "supernaturalbloodlines:sun_ring").size() == 1, "a Day Walker did not get the ring");
        } finally {
            done(helper, c, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noNewItemIsRegistered(GameTestHelper helper) {
        for (var id : BuiltInRegistries.ITEM.keySet()) {
            helper.assertTrue(!id.getNamespace().equals(NS) || !id.getPath().contains("coven"), "a coven item exists: " + id);
        }
        helper.succeed();
    }
}
