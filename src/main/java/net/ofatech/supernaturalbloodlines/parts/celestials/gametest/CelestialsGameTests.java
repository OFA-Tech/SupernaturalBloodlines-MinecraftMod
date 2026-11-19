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
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsPart;

/**
 * The lunar mood and the eclipse mirror, with a fake Enhanced Celestials (the test runtime has none). Needs no linked mod and reaches other
 * parts only through {@code core/moon}; what needs the eclipse part is in {@link CelestialsEclipseNoticeGameTests}.
 */
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

    /** The real start-up, with no Enhanced Celestials in the runtime: no adapter was made and the source is the empty one. Nothing here calls {@code install(null)} first. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRealStartUpWithoutEcLinksNothing(GameTestHelper helper) {
        if (net.neoforged.fml.ModList.get().isLoaded(CelestialsPart.EC_MOD)) {
            // a run with -PecJars: the start-up linked the real one, and CelestialsRealEcGameTests checks what it does
            helper.assertTrue(CelestialsPart.linkedAtInit(), "Enhanced Celestials is loaded but the start-up did not link it");
            helper.assertTrue(CelestialsPart.adapter() != null && LunarMoods.active(), "Enhanced Celestials is loaded but no adapter or no lunar source is in use");
            helper.succeed();
            return;
        }
        helper.assertFalse(CelestialsPart.linkedAtInit(), "the start-up linked an Enhanced Celestials that is not there");
        helper.assertTrue(CelestialsPart.adapter() == null, "an adapter is installed without Enhanced Celestials");
        helper.assertTrue(LunarMoods.source() == LunarSource.NONE, "a lunar source is in use without Enhanced Celestials");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutEcNothingChanges(GameTestHelper helper) {
        CelestialsPart.install(null);
        helper.assertTrue(LunarMoods.now(helper.getLevel()).isNone(), "a mood without Enhanced Celestials");
        helper.assertTrue(LunarMoods.riteCost(helper.getLevel(), true) == 1 && LunarMoods.riteCost(helper.getLevel(), false) == 1, "a rite cost factor without a mood");
        helper.assertTrue(LunarMoods.biteFactor(helper.getLevel()) == 1, "a factor without a mood");
        helper.assertFalse(LunarMoods.sparesForcedShift(helper.getLevel()), "the forced shift was spared without a mood");
        helper.assertTrue(LunarMoods.ahead(helper.getLevel(), 100).isEmpty(), "events without Enhanced Celestials");
        helper.assertFalse(LunarMoods.ecDrawsEclipse(helper.getLevel()), "the client would leave the moon to Enhanced Celestials");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrokenSourceMeansNoMoodNotAnExceptionAndIsDroppedForGood(GameTestHelper helper) {
        var level = helper.getLevel();
        LunarSource broken = new LunarSource() {
            @Override
            public LunarMood now(net.minecraft.world.level.Level l) {
                throw new IllegalStateException("EC changed its API");
            }

            @Override
            public List<Upcoming> ahead(net.minecraft.world.level.Level l, int days) {
                throw new NoSuchMethodError("EC changed its API");
            }
        };
        LunarMoods.use(broken, net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsConfig::tuning);
        try {
            // the mood is cached for a tick, so each call stands for a tick of its own
            helper.assertTrue(LunarMoods.now(level).isNone(), "a broken source gave a mood");
            helper.assertTrue(LunarMoods.ahead(level, 6).isEmpty(), "a broken source gave events");
            LunarMoods.refresh();
            helper.assertTrue(LunarMoods.now(level).isNone(), "a broken source gave a mood");
            helper.assertTrue(LunarMoods.source() == broken, "the source was dropped after two failures of each kind");
            LunarMoods.refresh();
            helper.assertTrue(LunarMoods.now(level).isNone(), "a broken source gave a mood");
            helper.assertTrue(LunarMoods.source() == LunarSource.NONE, "the source is still in use after repeated failures");
            helper.assertTrue(LunarMoods.riteCost(level, true) == 1 && LunarMoods.biteFactor(level) == 1, "a factor after the source was dropped");
        } finally {
            CelestialsPart.install(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrokenMirrorStopsBeingAskedAfterThreeTries(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakeEc ec = new FakeEc();
        ec.broken = true;
        with(ec, () -> {
            for (int i = 0; i < 6; i++) {
                EclipseSignal.fire(server, i % 2 == 0);
            }
            helper.assertTrue(ec.calls.size() == 3, "a broken Enhanced Celestials was asked " + ec.calls.size() + " times, not 3: " + ec.calls);
            helper.assertFalse(LunarMoods.active(), "the source is still in use");
        });
        helper.succeed();
    }

    /** Synchronous on purpose: other tests swap the shared source between ticks, so a wait across ticks would be flaky. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMoodIsAskedOncePerLevelAndTick(GameTestHelper helper) {
        var level = helper.getLevel();
        var other = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        int[] asked = {0};
        LunarMoods.use(new LunarSource() {
            @Override
            public LunarMood now(net.minecraft.world.level.Level l) {
                asked[0]++;
                return LunarMood.NONE;
            }

            @Override
            public List<Upcoming> ahead(net.minecraft.world.level.Level l, int days) {
                return List.of();
            }
        }, net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsConfig::tuning);
        try {
            LunarMoods.now(level);
            LunarMoods.now(level);
            LunarMoods.biteFactor(level);
            helper.assertTrue(asked[0] == 1, "the source was asked " + asked[0] + " times in one tick");
            LunarMoods.now(other);
            helper.assertTrue(asked[0] == 2, "another level shared the cache: " + asked[0]);
            LunarMoods.refresh();
            LunarMoods.now(other);
            helper.assertTrue(asked[0] == 3, "refresh did not drop the cached mood: " + asked[0]);
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
            var near = LunarMoods.ahead(helper.getLevel(), 6);
            helper.assertTrue(near.size() == 2 && near.get(0).days() == 2 && near.get(1).days() == 4, "the next six days: " + near);
            helper.assertTrue(near.get(1).mood().superMoon(), "the day 4 event is not a super moon");
            helper.assertTrue(near.get(0).nameKey().equals("lunar.event.enhancedcelestials2defaultlunarevents.blood_moon"), "the name key is " + near.get(0).nameKey());
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ourEclipseIsMirroredIntoEnhancedCelestials(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakeEc free = new FakeEc();
        with(free, () -> {
            EclipseSignal.fire(server, true);
            helper.assertTrue(free.calls.equals(List.of("start supernaturalbloodlines:lunar_eclipse")), "the mirror asked for " + free.calls);
            helper.assertTrue(LunarMoods.now(helper.getLevel()).kind() == LunarMood.Kind.ECLIPSE, "Enhanced Celestials is not showing the eclipse");
            helper.assertTrue(LunarMoods.ecDrawsEclipse(helper.getLevel()), "the client would not leave the moon to Enhanced Celestials");
            EclipseSignal.fire(server, false);
            helper.assertTrue(free.running.isEmpty() && free.calls.size() == 2, "the mirror stayed: " + free.calls);
        });
        // another event is running: it is not replaced, and its own end is not taken for ours
        FakeEc busy = new FakeEc().running("blood_moon");
        with(busy, () -> {
            EclipseSignal.fire(server, true);
            helper.assertTrue(busy.running.orElseThrow().id().getPath().equals("blood_moon"), "the Blood Moon was replaced");
            EclipseSignal.fire(server, false);
            helper.assertTrue(busy.running.isPresent(), "the end of our eclipse ended the Blood Moon");
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ourMirroredEclipseEndsWhenTheLinkEndsOrTheServerStops(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakeEc a = new FakeEc();
        CelestialsPart.install(a);
        try {
            EclipseSignal.fire(server, true);
            helper.assertTrue(a.running.isPresent(), "setup: the mirror did not start");
            CelestialsPart.install(null); // the part goes off, or the link is dropped
            helper.assertTrue(a.running.isEmpty(), "our eclipse stayed on in Enhanced Celestials after the link ended");
        } finally {
            CelestialsPart.install(null);
        }
        FakeEc b = new FakeEc();
        with(b, () -> {
            EclipseSignal.fire(server, true);
            helper.assertTrue(b.running.isPresent(), "setup: the second mirror did not start");
            CelestialsPart.stopMirror(b, server); // the server is stopping
            helper.assertTrue(b.running.isEmpty(), "our eclipse stayed on in Enhanced Celestials after the server stopped");
        });
        FakeEc busy = new FakeEc().running("blood_moon");
        with(busy, () -> {
            CelestialsPart.stopMirror(busy, server);
            helper.assertTrue(busy.running.isPresent(), "stopping our mirror ended the Blood Moon");
        });
        helper.succeed();
    }
}