package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.EclipseSignal;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsPart;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;

/** The lunar mood and the eclipse mirror, with a fake Enhanced Celestials (the test runtime has none). Needs no linked mod. */
@PrefixGameTestTemplate(false)
public final class CelestialsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CelestialsGameTests() {
    }

    /** Runs {@code body} with {@code ec} as the source of the lunar mood, then goes back to "no Enhanced Celestials". */
    static void with(FakeEc ec, Runnable body) {
        CelestialsPart.install(ec);
        try {
            body.run();
        } finally {
            CelestialsPart.install(null);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutEcNothingChanges(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CelestialsPart.install(null);
        helper.assertTrue(LunarMoods.now(helper.getLevel()).isNone(), "a mood without Enhanced Celestials");
        helper.assertTrue(LunarMoods.riteCost(helper.getLevel(), true) == 1 && LunarMoods.riteCost(helper.getLevel(), false) == 1, "a rite cost factor without a mood");
        helper.assertTrue(LunarMoods.biteFactor(helper.getLevel()) == 1 && LunarMoods.packChanceFactor(helper.getLevel()) == 1 && LunarMoods.bossWeight(helper.getLevel()) == 1, "a factor without a mood");
        helper.assertFalse(LunarMoods.sparesForcedShift(helper.getLevel()), "the forced shift was spared without a mood");
        helper.assertTrue(LunarMoods.ahead(helper.getLevel(), 100).isEmpty(), "events without Enhanced Celestials");
        helper.assertFalse(LunarMoods.ecDrawsEclipse(helper.getLevel()), "the client would leave the moon to Enhanced Celestials");
        helper.assertFalse(EclipseSignal.fire(server, true) || EclipseSignal.fire(server, false), "the eclipse part was told to skip its own notice");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrokenSourceMeansNoMoodNotAnException(GameTestHelper helper) {
        var level = helper.getLevel();
        LunarMoods.use(new net.ofatech.supernaturalbloodlines.core.moon.LunarSource() {
            @Override
            public LunarMood now(net.minecraft.world.level.Level l) {
                throw new IllegalStateException("EC changed its API");
            }

            @Override
            public List<Upcoming> ahead(net.minecraft.world.level.Level l, int days) {
                throw new NoSuchMethodError("EC changed its API");
            }
        }, net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsConfig::tuning);
        try {
            helper.assertTrue(LunarMoods.now(level).isNone() && LunarMoods.riteCost(level, true) == 1 && LunarMoods.biteFactor(level) == 1, "a broken source gave a mood");
            helper.assertTrue(LunarMoods.ahead(level, 6).isEmpty(), "a broken source gave events");
        } finally {
            CelestialsPart.install(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMoodFollowsTheRunningEvent(GameTestHelper helper) {
        var level = helper.getLevel();
        with(new FakeEc().running("blood_moon"), () -> {
            helper.assertTrue(LunarMoods.now(level).kind() == LunarMood.Kind.BLOOD && !LunarMoods.now(level).superMoon(), "a Blood Moon was " + LunarMoods.now(level));
            helper.assertTrue(LunarMoods.biteFactor(level) == 1.25, "the bite bonus is " + LunarMoods.biteFactor(level));
            helper.assertTrue(LunarMoods.riteCost(level, true) == 1, "a Blood Moon changed a rite cost");
            helper.assertFalse(LunarMoods.sparesForcedShift(level), "a Blood Moon spared the forced shift");
        });
        with(new FakeEc().running("blue_moon"), () -> {
            helper.assertTrue(LunarMoods.riteCost(level, true) == 0.8 && LunarMoods.riteCost(level, false) == 0.8, "a Blue Moon rite cost");
            helper.assertFalse(LunarMoods.sparesForcedShift(level), "a plain Blue Moon spared the forced shift");
            helper.assertTrue(LunarMoods.biteFactor(level) == 1, "a Blue Moon changed the bite");
        });
        with(new FakeEc().running("super_blue_moon"), () -> {
            helper.assertTrue(LunarMoods.riteCost(level, false) == 0.8, "a super moon discounted a rite that does not need the full moon: " + LunarMoods.riteCost(level, false));
            helper.assertTrue(Math.abs(LunarMoods.riteCost(level, true) - 0.6) < 1e-9, "the Super Blue Moon full-moon rite cost is " + LunarMoods.riteCost(level, true));
            helper.assertTrue(LunarMoods.sparesForcedShift(level), "a Super Blue Moon did not spare the forced shift");
            helper.assertTrue(LunarMoods.bossWeight(level) == 2.0, "the boss weight");
        });
        with(new FakeEc().running("super_moon"), () -> {
            helper.assertTrue(LunarMoods.riteCost(level, true) == 0.75 && LunarMoods.riteCost(level, false) == 1, "a super moon rite cost");
            helper.assertFalse(LunarMoods.sparesForcedShift(level), "a plain super moon spared the forced shift");
        });
        with(new FakeEc().running("harvest_moon"), () -> helper.assertTrue(LunarMoods.biteFactor(level) == 1 && LunarMoods.riteCost(level, true) == 1, "a Harvest Moon changed a factor"));
        FakeEc other = new FakeEc();
        other.running = java.util.Optional.of(new net.ofatech.supernaturalbloodlines.parts.celestials.EcAdapter.Event(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("someaddon", "blood_moon"), -1));
        with(other, () -> helper.assertTrue(LunarMoods.now(level).isNone(), "an event of another add-on counted as a Blood Moon"));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theScheduleReachesTheHub(GameTestHelper helper) {
        FakeEc ec = new FakeEc();
        ec.schedule.put(4, FakeEc.event("super_moon"));
        ec.schedule.put(2, FakeEc.event("blood_moon"));
        ec.schedule.put(40, FakeEc.event("blue_moon"));
        with(ec, () -> {
            var level = helper.getLevel();
            var near = LunarMoods.ahead(level, 6);
            helper.assertTrue(near.size() == 2 && near.get(0).days() == 2 && near.get(1).days() == 4, "the next six days: " + near);
            helper.assertTrue(HubAlmanac.next(LunarMoods.ahead(level, 100)).orElseThrow().days() == 2, "the next event is not the nearest");
            helper.assertTrue(HubAlmanac.eventOn(near, 4).orElseThrow().mood().superMoon(), "the day 4 event is not a super moon");
            helper.assertTrue(HubAlmanac.eventOn(near, 3).isEmpty(), "an event on a day with none");
            helper.assertTrue(near.get(0).nameKey().equals("lunar.event.enhancedcelestials2defaultlunarevents.blood_moon"), "the name key is " + near.get(0).nameKey());
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ourEclipseIsMirroredAndOnlyOneNoticeIsSent(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        LunarEclipse.overridePlayers(s -> List.of());
        FakeEc free = new FakeEc();
        try {
            with(free, () -> {
                helper.assertTrue(LunarEclipse.start(server), "the eclipse did not start");
                helper.assertTrue(free.calls.equals(List.of("start supernaturalbloodlines:lunar_eclipse")), "the mirror asked for " + free.calls);
                helper.assertTrue(LunarMoods.now(helper.getLevel()).kind() == LunarMood.Kind.ECLIPSE, "Enhanced Celestials is not showing the eclipse");
                helper.assertTrue(LunarMoods.ecDrawsEclipse(helper.getLevel()), "the client would not leave the moon to Enhanced Celestials");
                helper.assertTrue(LunarEclipse.active(server), "the eclipse gameplay did not start");
                helper.assertTrue(LunarEclipse.stop(server), "the eclipse did not stop");
                helper.assertTrue(free.running.isEmpty() && free.calls.size() == 2, "the mirror stayed: " + free.calls);
                // the notice: Enhanced Celestials sends it, so the eclipse part skips its own
                helper.assertTrue(EclipseSignal.fire(server, true), "no notice was left to Enhanced Celestials");
                free.running = java.util.Optional.empty();
            });
            // another event is running: nothing is mirrored (the eclipse part keeps its own chat line), and the gameplay still runs
            FakeEc busy = new FakeEc().running("blood_moon");
            with(busy, () -> {
                helper.assertFalse(EclipseSignal.fire(server, true), "a start was announced although a Blood Moon is running");
                helper.assertTrue(LunarEclipse.start(server), "the eclipse did not start in a Blood Moon");
                helper.assertTrue(busy.running.orElseThrow().id().getPath().equals("blood_moon"), "the Blood Moon was replaced");
                helper.assertTrue(LunarEclipse.active(server), "the eclipse gameplay did not run in a Blood Moon");
                helper.assertTrue(LunarEclipse.stop(server), "the eclipse did not stop");
                helper.assertTrue(busy.running.isPresent(), "the end of our eclipse ended the Blood Moon");
            });
        } finally {
            LunarEclipse.stop(server);
            LunarEclipse.overridePlayers(null);
        }
        helper.succeed();
    }
}
