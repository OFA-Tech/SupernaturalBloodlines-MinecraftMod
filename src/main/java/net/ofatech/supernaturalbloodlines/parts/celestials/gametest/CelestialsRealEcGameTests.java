package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsPart;
import net.ofatech.supernaturalbloodlines.parts.celestials.EcAdapter;

/**
 * Against the REAL Enhanced Celestials 2 classes and datapack loading. Registered only when EC is loaded, so only a run with
 * {@code -PecJars=<folder>} has them (CI has no EC jars): that our datapack files decoded, that the reflection finds every member, and that a
 * mirrored event can be started and ended on a real forecast (EC's forecast takes the change on its next server tick).
 */
@PrefixGameTestTemplate(false)
public final class CelestialsRealEcGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final int TICKS = 5;

    private CelestialsRealEcGameTests() {
    }

    private static void restore(ServerLevel level, EcAdapter ec, long dayTime) {
        ec.stop(level, CelestialsPart.ECLIPSE_EVENT);
        level.setDayTime(dayTime);
        CelestialsPart.install(null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRealStartUpLinkedEcAndItKnowsOurEclipseEvent(GameTestHelper helper) throws Exception {
        helper.assertTrue(CelestialsPart.linkedAtInit(), "the start-up did not link Enhanced Celestials");
        EcAdapter ec = CelestialsPart.createReal();
        helper.assertTrue(ec.knows(helper.getLevel(), CelestialsPart.ECLIPSE_EVENT), "Enhanced Celestials did not load our lunar_eclipse event (datapack decode)");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMirroredEventStartsAndEndsOnARealForecast(GameTestHelper helper) throws Exception {
        EcAdapter ec = CelestialsPart.createReal();
        ServerLevel level = helper.getLevel();
        CelestialsPart.install(ec);
        long dayTime = level.getDayTime();
        level.setDayTime(14000L); // our eclipse starts at dusk: EC's event only counts as running inside its night window
        try {
            ec.upcoming(level, 100);
            helper.assertTrue(ec.active(level).isEmpty(), "an event was running before ours: " + ec.active(level));
            helper.assertTrue(ec.start(level, CelestialsPart.ECLIPSE_EVENT), "start was refused with no other event running");
        } catch (RuntimeException e) {
            restore(level, ec, dayTime);
            throw e;
        }
        helper.runAfterDelay(TICKS, () -> {
            try {
                helper.assertTrue(ec.active(level).map(e -> e.id().equals(CelestialsPart.ECLIPSE_EVENT)).orElse(false), "our event is not the running one: " + ec.active(level));
                LunarMoods.refresh();
                helper.assertTrue(LunarMoods.now(level).kind() == LunarMood.Kind.ECLIPSE, "the mood is not the eclipse: " + LunarMoods.now(level));
                helper.assertTrue(ec.stop(level, CelestialsPart.ECLIPSE_EVENT), "stop did not end our event");
            } catch (RuntimeException e) {
                restore(level, ec, dayTime);
                throw e;
            }
            helper.runAfterDelay(TICKS, () -> {
                try {
                    helper.assertTrue(ec.active(level).isEmpty(), "our event is still running after stop: " + ec.active(level));
                } finally {
                    restore(level, ec, dayTime);
                }
                helper.succeed();
            });
        });
    }
}
