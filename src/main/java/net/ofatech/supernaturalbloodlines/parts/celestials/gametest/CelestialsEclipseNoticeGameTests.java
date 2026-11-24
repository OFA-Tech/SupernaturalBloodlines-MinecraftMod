package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsPart;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;

/**
 * The eclipse part's own chat line, on a real eclipse start and end, with and without Enhanced Celestials mirroring it: the line is always sent
 * (that EC announces a mirrored event is not verified in a game). Registered only when the species part is on, like the other tests that reach into it.
 */
@PrefixGameTestTemplate(false)
public final class CelestialsEclipseNoticeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CelestialsEclipseNoticeGameTests() {
    }

    private static List<String> keys(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component c : lines) {
            out.add(c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString());
        }
        return out;
    }

    private static void run(GameTestHelper helper, FakeEc ec, boolean mirrored) {
        MinecraftServer server = helper.getLevel().getServer();
        List<Component> sent = new ArrayList<>();
        LunarEclipse.overridePlayers(s -> List.of());
        LunarEclipse.overrideNotice((s, line) -> sent.add(line));
        try {
            CelestialsPart.install(ec);
            helper.assertTrue(LunarEclipse.start(server), "the eclipse did not start");
            helper.assertTrue(LunarEclipse.stop(server), "the eclipse did not stop");
            helper.assertTrue(ec == null || ec.calls.isEmpty() == !mirrored, "the mirror: " + (ec == null ? "none" : ec.calls));
            helper.assertTrue(keys(sent).equals(List.of(LunarEclipse.START_MESSAGE, LunarEclipse.END_MESSAGE)),
                    "our chat lines (mirrored " + mirrored + "): " + keys(sent));
        } finally {
            LunarEclipse.stop(server);
            CelestialsPart.install(null);
            LunarEclipse.overrideNotice(null);
            LunarEclipse.overridePlayers(null);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutEcARealEclipseSendsOurChatLine(GameTestHelper helper) {
        run(helper, null, false);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withEcTheMirroredEclipseStillSendsOurChatLine(GameTestHelper helper) {
        run(helper, new FakeEc(), true);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withAnotherEventRunningTheChatLineAndTheGameplayStillRun(GameTestHelper helper) {
        FakeEc busy = new FakeEc().running("blood_moon");
        run(helper, busy, true);
        helper.assertTrue(busy.running.orElseThrow().id().getPath().equals("blood_moon"), "the Blood Moon was replaced");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRestartInsideAnEclipseMirrorsItAgainAndAStaleMirrorIsEnded(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        FakeEc ec = new FakeEc();
        LunarEclipse.overridePlayers(s -> List.of());
        LunarEclipse.overrideNotice((s, line) -> {
        });
        // an eclipse is active only inside its night (the time of day is before the dawn): the world's time is whatever the tests before this one left it (some set it just past the dawn), so this test picks its own
        long dayTime = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(dayTime - dayTime % 24000L + 15000L);
        try {
            CelestialsPart.install(ec);
            helper.assertTrue(LunarEclipse.start(server) && ec.running.isPresent(), "setup: the eclipse did not start mirrored");
            CelestialsPart.stopMirror(ec, server); // the shutdown
            helper.assertTrue(ec.running.isEmpty() && LunarEclipse.active(server), "setup: the shutdown did not end the mirror of an eclipse that is still on");
            LunarEclipse.onServerStarted(server); // the start
            helper.assertTrue(ec.running.isPresent(), "the restart did not mirror the eclipse that is still on");
            LunarEclipse.onServerStarted(server);
            helper.assertTrue(ec.running.isPresent() && ec.calls.stream().filter(c -> c.startsWith("start")).count() <= 3, "a second start looped: " + ec.calls);
            // the eclipse ended while the server was off, but the saved event of Enhanced Celestials still shows ours
            LunarEclipse.stop(server);
            helper.assertTrue(ec.running.isEmpty() && !LunarEclipse.active(server), "setup: the eclipse did not end");
            ec.start(server.overworld(), CelestialsPart.ECLIPSE_EVENT);
            helper.assertTrue(ec.running.isPresent(), "setup: the stale mirror was not made");
            LunarEclipse.onServerStarted(server);
            helper.assertTrue(ec.running.isEmpty(), "a stale mirror of an eclipse that is over stayed on");
        } finally {
            LunarEclipse.stop(server);
            helper.getLevel().setDayTime(dayTime);
            CelestialsPart.install(null);
            LunarEclipse.overrideNotice(null);
            LunarEclipse.overridePlayers(null);
        }
        helper.succeed();
    }
}
