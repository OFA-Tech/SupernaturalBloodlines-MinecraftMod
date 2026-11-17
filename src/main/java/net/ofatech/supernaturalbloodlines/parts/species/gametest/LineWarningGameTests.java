package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;

/** R124: the line is warned once each time an Original becomes open to white oak, and a hidden Original is never named. */
@PrefixGameTestTemplate(false)
public final class LineWarningGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final UUID KLAUS = NpcOriginal.KLAUS.uuid();
    private static final String NAMED = "supernaturalbloodlines.line.warning_desiccated_named";
    private static final String HIDDEN = "supernaturalbloodlines.line.warning_desiccated_hidden";
    private static final String OPEN_NAMED = "supernaturalbloodlines.line.warning_open_named";
    private static final String OPEN_HIDDEN = "supernaturalbloodlines.line.warning_open_hidden";

    private LineWarningGameTests() {
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static LineTestPlayer memberOfKlaus(GameTestHelper helper, boolean discovered) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        species(p, Species.HYBRID);
        LineIndex.get(server(helper)).setRoot(p.getUUID(), KLAUS, true);
        if (discovered) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().knownLine(KLAUS).build());
        }
        return p;
    }

    private static void done(GameTestHelper helper, ServerPlayer... ps) {
        LineWarning.reset();
        LineWarning.overridePlayers(null);
        SpeciesConfig.overrideOriginalLineDeath(null);
        for (ServerPlayer p : ps) {
            LineIndex.get(server(helper)).remove(p.getUUID());
            p.removeData(SpeciesRegistries.DOWNED);
        }
        cleanUp(ps);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void desiccatedOriginalWarnsOnlineMembers(GameTestHelper helper) {
        LineTestPlayer a = memberOfKlaus(helper, true);
        LineTestPlayer b = memberOfKlaus(helper, false);
        try {
            online(a, b);
            LineWarning.opened(server(helper), KLAUS, true);
            helper.assertTrue(a.received(NAMED) && a.heard().contains("[supernaturalbloodlines.original.klaus]"), "the one who knows was not told by name: " + a.heard());
            helper.assertTrue(b.received(HIDDEN), "the one who does not know got no warning: " + b.heard());
        } finally {
            done(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void warningHidesTheNameUntilDiscovered(GameTestHelper helper) {
        LineTestPlayer b = memberOfKlaus(helper, false);
        try {
            online(b);
            LineWarning.opened(server(helper), KLAUS, true);
            helper.assertTrue(!b.received(NAMED) && !b.heard().toLowerCase().contains("klaus"), "the warning named an undiscovered Original: " + b.heard());
            helper.assertTrue(b.heard().equals("[" + HIDDEN + "]"), "unexpected warning: " + b.heard());
        } finally {
            done(helper, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void warnsOncePerOpening(GameTestHelper helper) {
        LineTestPlayer a = memberOfKlaus(helper, true);
        try {
            online(a);
            LineWarning.opened(server(helper), KLAUS, true);
            LineWarning.opened(server(helper), KLAUS, true);
            helper.assertTrue(a.count(NAMED) == 1, "warned " + a.count(NAMED) + " times for one opening");
            LineWarning.closed(KLAUS);
            LineWarning.opened(server(helper), KLAUS, true);
            helper.assertTrue(a.count(NAMED) == 2, "a new opening did not warn: " + a.count(NAMED));
        } finally {
            done(helper, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void playerOriginalDownedWarnsMembers(GameTestHelper helper) {
        LineTestPlayer o = LineTestPlayer.create(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer a = LineTestPlayer.create(helper);
        species(a, Species.HYBRID);
        try {
            SireRecorder.record(o, a);
            online(o, a);
            LineWarning.overridePlayers(s -> List.of(o));
            LineWarning.tick(server(helper));
            helper.assertTrue(a.received() == 0, "a healthy Original warned their line");

            o.setData(SpeciesRegistries.DOWNED, 200);
            LineWarning.tick(server(helper));
            helper.assertTrue(a.count(OPEN_HIDDEN) + a.count(OPEN_NAMED) == 1, "the member was not warned once, in the words of an Original who lies open: " + a.heard());
            helper.assertTrue(a.count(HIDDEN) + a.count(NAMED) == 0, "a downed Original was called desiccated: " + a.heard());
            LineWarning.tick(server(helper));
            helper.assertTrue(a.received() == 1, "the second tick warned again");

            o.setData(SpeciesRegistries.DOWNED, 0);
            LineWarning.tick(server(helper));
            o.setData(SpeciesRegistries.DOWNED, 200);
            LineWarning.tick(server(helper));
            helper.assertTrue(a.received() == 2, "going down again did not warn again: " + a.received());
            helper.assertTrue(o.received() == 0, "the Original was warned about themselves");
        } finally {
            done(helper, o, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void downedOriginalIsNotCalledDesiccatedAndAHiddenOneIsNotNamed(GameTestHelper helper) {
        LineTestPlayer b = memberOfKlaus(helper, false);
        LineTestPlayer a = memberOfKlaus(helper, true);
        try {
            online(a, b);
            LineWarning.opened(server(helper), KLAUS, false);
            helper.assertTrue(a.heard().equals("[" + OPEN_NAMED + "][supernaturalbloodlines.original.klaus]"), "the one who knows heard: " + a.heard());
            helper.assertTrue(b.heard().equals("[" + OPEN_HIDDEN + "]"), "the one who does not know heard: " + b.heard());
        } finally {
            done(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginWhileOpenWarns(GameTestHelper helper) {
        LineTestPlayer c = memberOfKlaus(helper, true);
        try {
            LineWarning.opened(server(helper), KLAUS, true); // nobody online yet
            helper.assertTrue(c.received() == 0, "control: someone was online");
            online(c);
            LineWarning.onLogin(c);
            helper.assertTrue(c.count(NAMED) == 1, "the login did not warn once: " + c.heard());
            LineWarning.closed(KLAUS);
            LineWarning.onLogin(c);
            helper.assertTrue(c.count(NAMED) == 1, "a login after the Original recovered warned");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noWarningWhenLineDeathIsOff(GameTestHelper helper) {
        LineTestPlayer a = memberOfKlaus(helper, true);
        SpeciesConfig.overrideOriginalLineDeath(false);
        try {
            online(a);
            LineWarning.opened(server(helper), KLAUS, true);
            LineWarning.onLogin(a);
            helper.assertTrue(a.received() == 0, "a warning came with line death off: " + a.heard());
        } finally {
            done(helper, a);
        }
        helper.succeed();
    }
}
