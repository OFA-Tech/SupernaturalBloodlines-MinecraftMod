package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import com.google.gson.JsonParser;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.LadderService;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;

/** Claims on real players: SP, refusals that explain themselves, the Ghoul freeze and the rungs that pay on their own. */
@PrefixGameTestTemplate(false)
public final class ClaimGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ClaimGameTests() {}

    private static ServerPlayer vampire(GameTestHelper helper, int level) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level), "could not make a vampire");
        return p;
    }

    private static void done(ServerPlayer p, String... leaves) {
        SupernaturalState s = SupernaturalAccess.get(p);
        for (String id : leaves) {
            s = LadderRules.markDone(s, id);
        }
        SupernaturalAccess.set(p, s);
    }

    private static SupernaturalState state(ServerPlayer p) {
        return SupernaturalAccess.get(p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void claimPaysSp(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        done(p, "L-VAM-1a", "L-VAM-1b");
        LadderRules.ClaimResult first = LadderService.claim(p, "L-VAM-1a");
        helper.assertTrue(first.ok() && state(p).sp() == 1 && state(p).rung(SupernaturalRace.VAMPIRE) == 0, "first claim: " + state(p));
        LadderRules.ClaimResult last = LadderService.claim(p, "L-VAM-1b");
        helper.assertTrue(last.ok() && last.rungReached() == 1 && state(p).sp() == 2 && state(p).spEarned() == 2
                && state(p).rung(SupernaturalRace.VAMPIRE) == 1, "second claim: " + state(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void claimTwiceRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        done(p, "L-VAM-1a", "L-VAM-1b");
        LadderService.claim(p, "L-VAM-1a");
        SupernaturalState before = state(p);
        LadderRules.ClaimResult again = LadderService.claim(p, "L-VAM-1a");
        helper.assertTrue(again.refusal() == LadderRules.Refusal.ALREADY_CLAIMED && state(p).equals(before), "a second claim: " + again.refusal());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void claimOtherRaceRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        done(p, "L-HUN-1a", "L-VAM-2a");
        SupernaturalState before = state(p);
        helper.assertTrue(LadderService.claim(p, "L-HUN-1a").refusal() == LadderRules.Refusal.NOT_ACTIVE, "a hunter task was claimed by a vampire");
        helper.assertTrue(LadderService.claim(p, "L-VAM-2a").refusal() == LadderRules.Refusal.NOT_ACTIVE, "a later rung was claimed");
        helper.assertTrue(state(p).equals(before), "a refusal changed the state");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anInventedTaskIdIsRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        SupernaturalState before = state(p);
        for (String id : new String[] {"", "x", "../../etc", "L-VAM-99z", "L-VAM-1a.1", "\u0000"}) {
            helper.assertTrue(!LadderService.claim(p, id).ok(), "'" + id + "' was accepted");
        }
        helper.assertTrue(state(p).equals(before), "a refused claim changed the state");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulFrozenThenVampire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        done(p, "L-VAM-1a");
        LadderRules.ClaimResult c = LadderService.claim(p, "L-VAM-1a");
        helper.assertTrue(c.refusal() == LadderRules.Refusal.GHOUL_FROZEN, "a Ghoul's claim: " + c.refusal());
        helper.assertTrue(state(p).sp() == 0 && state(p).claimed().isEmpty() && state(p).rung(SupernaturalRace.VAMPIRE) == 0, "a Ghoul was paid");
        TaskTracker.emit(p, net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent.of(
                net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector.FEED, 1));
        helper.assertTrue(state(p).counters().isEmpty(), "a Ghoul's feed counted");
        // the turning ends: the player is a vampire at L-VAM-0 and the ladder moves again
        BloodlineAccess.set(p, BloodlineState.NONE);
        helper.assertTrue(state(p).rung(SupernaturalRace.VAMPIRE) == 0, "the turned vampire is not at L-VAM-0");
        TaskTracker.emit(p, net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent.of(
                net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector.FEED, 1));
        helper.assertTrue(state(p).counters().getOrDefault("L-VAM-1b", 0L) == 1, "feeding does not count after the turning");
        helper.assertTrue(LadderService.claim(p, "L-VAM-1a").ok(), "a turned vampire cannot claim");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRungWithNoTasksIsReachedOnItsOwn(GameTestHelper helper) {
        // a ladder whose first rung lost every task (its mod or part is off) pays at once; a table of our own, put back afterwards
        LadderTable before = LadderTable.current();
        ServerPlayer p = vampire(helper, 1);
        try {
            LadderTable.Parsed parsed = LadderTable.parse(Map.of("vampire", JsonParser.parseString("""
                    { "rungs": [ { "id": "T-0" },
                      { "id": "T-1", "sp": 2, "ep": 500, "tasks": [ { "id": "T-1a", "detector": "signal", "signal": "nothing", "part": "no_such_part" } ] },
                      { "id": "T-2", "sp": 3, "tasks": [ { "id": "T-2a", "detector": "feed", "n": 99 } ] } ] }
                    """)), mod -> true, part -> !part.equals("no_such_part"));
            helper.assertTrue(parsed.rejected().isEmpty(), "setup: " + parsed.rejected());
            LadderTable.setCurrent(parsed.table());
            TaskTracker.recheck(p);
            helper.assertTrue(state(p).rung(SupernaturalRace.VAMPIRE) == 1 && state(p).sp() == 2, "the empty rung was not reached: " + state(p));
            TaskTracker.recheck(p);
            helper.assertTrue(state(p).sp() == 2, "the empty rung paid twice: " + state(p));
        } finally {
            LadderTable.setCurrent(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulIsNotPaidForAnEmptyRung(GameTestHelper helper) {
        LadderTable before = LadderTable.current();
        ServerPlayer p = vampire(helper, 1);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        try {
            LadderTable.setCurrent(LadderTable.parse(Map.of("vampire", JsonParser.parseString(
                    "{ \"rungs\": [ { \"id\": \"G-0\" }, { \"id\": \"G-1\", \"sp\": 2 } ] }")), mod -> true).table());
            TaskTracker.recheck(p);
            helper.assertTrue(state(p).sp() == 0 && state(p).rung(SupernaturalRace.VAMPIRE) == 0, "a Ghoul was paid: " + state(p));
        } finally {
            LadderTable.setCurrent(before);
        }
        helper.succeed();
    }
}
