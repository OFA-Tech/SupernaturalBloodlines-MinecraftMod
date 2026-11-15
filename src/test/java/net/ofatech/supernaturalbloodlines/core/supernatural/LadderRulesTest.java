package net.ofatech.supernaturalbloodlines.core.supernatural;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules.ClaimResult;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules.Refusal;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector;
import org.junit.jupiter.api.Test;

class LadderRulesTest {
    private static final LadderTable T = ShippedLadders.all();
    private static final SupernaturalState FRESH = SupernaturalState.NONE;
    private static final String REVEAL = "supernaturalbloodlines:rite_of_lineage_reveal";

    private static SupernaturalState done(SupernaturalState s, String... leafIds) {
        for (String id : leafIds) {
            s = LadderRules.markDone(s, id);
        }
        return s;
    }

    private static ClaimResult claim(SupernaturalState s, SupernaturalRace creature, SupernaturalRace magic, String task) {
        return LadderRules.claim(T, s, creature, Optional.ofNullable(magic), task);
    }

    private static LadderTable custom(java.util.function.Predicate<String> mods, String... keyAndJson) {
        Map<String, JsonElement> files = new java.util.HashMap<>();
        for (int i = 0; i < keyAndJson.length; i += 2) {
            files.put(keyAndJson[i], JsonParser.parseString(keyAndJson[i + 1]));
        }
        LadderTable.Parsed p = LadderTable.parse(files, mods);
        assertEquals(List.of(), p.rejected());
        return p.table();
    }

    @Test
    void claimSplitsTheRungAndTheLastClaimReachesIt() {
        SupernaturalState s = done(FRESH, "L-ORI-1a", "L-ORI-1b");
        ClaimResult first = claim(s, ORIGINAL, null, "L-ORI-1a");
        assertTrue(first.ok());
        assertEquals(1, first.sp());
        assertEquals(2500, first.ep());
        assertEquals(-1, first.rungReached());
        assertEquals(0, first.state().rung(ORIGINAL));

        ClaimResult last = claim(first.state(), ORIGINAL, null, "L-ORI-1b");
        assertEquals(2, last.sp());
        assertEquals(2500, last.ep());
        assertEquals(1, last.rungReached());
        assertEquals(ORIGINAL, last.race());
        assertEquals(1, last.state().rung(ORIGINAL));
        assertEquals(3, last.state().sp());
        assertEquals(3, last.state().spEarned());
        assertEquals(Optional.of("L-ORI-2"), LadderRules.activeRung(T, last.state(), ORIGINAL).map(Rung::id));
    }

    @Test
    void theRemainderGoesToTheLastTaskWhateverTheClaimOrder() {
        SupernaturalState s = done(FRESH, "L-ORI-1a", "L-ORI-1b");
        ClaimResult b = claim(s, ORIGINAL, null, "L-ORI-1b");
        assertEquals(2, b.sp());
        assertEquals(-1, b.rungReached());
        ClaimResult a = claim(b.state(), ORIGINAL, null, "L-ORI-1a");
        assertEquals(1, a.sp());
        assertEquals(1, a.rungReached());
    }

    @Test
    void shareMath() {
        assertEquals(1, LadderRules.share(3, 2, 0));
        assertEquals(2, LadderRules.share(3, 2, 1));
        assertEquals(5, LadderRules.share(5, 1, 0));
        assertEquals(100_000 - 2 * 33_333L, LadderRules.share(100_000L, 3, 2));
        assertEquals(0, LadderRules.share(0, 2, 1));
    }

    @Test
    void anAnyOfGroupIsOneTaskAndPaysOnce() {
        // L-HYB-1: 2 SP over a) an any-of group and b) a level step
        SupernaturalState s = done(FRESH, "L-HYB-1a.1", "L-HYB-1a.2", "L-HYB-1b");
        ClaimResult a = claim(s, HYBRID, null, "L-HYB-1a");
        assertEquals(1, a.sp());
        ClaimResult b = claim(a.state(), HYBRID, null, "L-HYB-1b");
        assertEquals(1, b.sp());
        assertEquals(1, b.state().rung(HYBRID));
        assertEquals(2, b.state().sp());
    }

    @Test
    void oneMemberOfTheGroupIsEnoughAndTheOtherStopsCounting() {
        SupernaturalState s = LadderRules.apply(T, FRESH, List.of(HYBRID), TaskEvent.of(Detector.STATE, 1, "predicate", "moon_unbound"));
        assertTrue(s.done().contains("L-HYB-1a.2"));
        assertFalse(s.done().contains("L-HYB-1b"));
        SupernaturalState after = LadderRules.apply(T, s, List.of(HYBRID),
                TaskEvent.of(Detector.RITE, 1, "rite", "supernaturalbloodlines:rite_of_the_moon", "role", "target"));
        assertEquals(s, after);
        assertTrue(claim(s, HYBRID, null, "L-HYB-1a").ok());
    }

    @Test
    void aRungWhoseTasksAllCameFromAMissingModIsReachedAtOnce() {
        String json = """
                { "rungs": [
                  { "id": "A-0" },
                  { "id": "A-1", "sp": 2, "ep": 10, "tasks": [ { "id": "A-1a", "detector": "feed" } ] },
                  { "id": "A-2", "sp": 3, "ep": 30, "tasks": [
                    { "id": "A-2a", "detector": "rite", "rite": "any", "role": "target", "mod": "enchanted" } ] },
                  { "id": "A-3", "sp": 4, "ep": 40, "tasks": [ { "id": "A-3a", "detector": "feed" } ] } ] }
                """;
        LadderTable t = custom(mod -> !mod.equals("enchanted"), "vampire", json);
        SupernaturalState s = LadderRules.markDone(FRESH, "A-1a");
        ClaimResult r = LadderRules.claim(t, s, VAMPIRE, Optional.empty(), "A-1a");
        assertEquals(5, r.sp());
        assertEquals(40, r.ep());
        assertEquals(2, r.rungReached());
        assertEquals(2, r.state().rung(VAMPIRE));
        assertEquals(Optional.of("A-3"), LadderRules.activeRung(t, r.state(), VAMPIRE).map(Rung::id));
    }

    @Test
    void settleReachesLeadingTasklessRungsAndStopsAtOneWithTasks() {
        String json = """
                { "rungs": [ { "id": "A-0" }, { "id": "A-1", "sp": 1, "ep": 5 }, { "id": "A-2", "sp": 2 },
                  { "id": "A-3", "sp": 4, "tasks": [ { "id": "A-3a", "detector": "feed" } ] } ] }
                """;
        LadderTable t = custom(mod -> true, "vampire", json);
        ClaimResult r = LadderRules.settle(t, FRESH, VAMPIRE);
        assertEquals(3, r.sp());
        assertEquals(5, r.ep());
        assertEquals(2, r.rungReached());
        assertEquals(3, r.state().sp());
        ClaimResult again = LadderRules.settle(t, r.state(), VAMPIRE);
        assertEquals(0, again.sp());
        assertEquals(-1, again.rungReached());
        assertEquals(r.state(), again.state());
    }

    @Test
    void aGhoulRecordsAndClaimsNothing() {
        SupernaturalState s = done(FRESH, "L-VAM-1a");
        TaskEvent feed = TaskEvent.of(Detector.FEED, 1);
        assertEquals(s, LadderRules.apply(T, s, List.of(GHOUL), feed));
        assertEquals(s, LadderRules.apply(T, s, List.of(GHOUL, WITCH), feed));
        assertEquals(s, LadderRules.record(T, s, GHOUL, "L-VAM-1b", 5));
        ClaimResult c = claim(s, GHOUL, null, "L-VAM-1a");
        assertEquals(Refusal.GHOUL_FROZEN, c.refusal());
        assertEquals(s, c.state());
        // frozen for the magic race as well
        assertEquals(Refusal.GHOUL_FROZEN, claim(done(FRESH, "L-WIT-1a"), GHOUL, WITCH, "L-WIT-1a").refusal());
    }

    @Test
    void countingOnlyWhileTheTaskIsActive() {
        SupernaturalState s = FRESH;
        // L-VAM-4b (kill hunters) is four rungs away
        s = LadderRules.apply(T, s, List.of(VAMPIRE), TaskEvent.of(Detector.KILL, 1, "category", "hunter"));
        assertEquals(FRESH, s);
        for (int i = 0; i < 9; i++) {
            s = LadderRules.apply(T, s, List.of(VAMPIRE), TaskEvent.of(Detector.FEED, 1));
        }
        assertEquals(9L, s.counters().get("L-VAM-1b"));
        assertFalse(s.done().contains("L-VAM-1b"));
        s = LadderRules.apply(T, s, List.of(VAMPIRE), TaskEvent.of(Detector.FEED, 1));
        assertTrue(s.done().contains("L-VAM-1b"));
        assertTrue(s.counters().isEmpty());
        // done tasks stop counting
        assertEquals(s, LadderRules.apply(T, s, List.of(VAMPIRE), TaskEvent.of(Detector.FEED, 5)));
    }

    @Test
    void aValueTaskCountsWhenTheValueReachesTheTarget() {
        TaskEvent lvl1 = TaskEvent.of(Detector.LEVEL, 1, "faction", "vampirism:vampire");
        TaskEvent lvl5 = TaskEvent.of(Detector.LEVEL, 5, "faction", "vampirism:vampire");
        TaskEvent hunter5 = TaskEvent.of(Detector.LEVEL, 5, "faction", "vampirism:hunter");
        assertEquals(FRESH, LadderRules.apply(T, FRESH, List.of(VAMPIRE), lvl1));
        assertEquals(FRESH, LadderRules.apply(T, FRESH, List.of(VAMPIRE), hunter5));
        SupernaturalState s = LadderRules.apply(T, FRESH, List.of(VAMPIRE), lvl5);
        assertTrue(s.done().contains("L-VAM-1a"));
        // level 5 does not reach the level 4 step of the rung after the active one
        assertFalse(s.done().contains("L-VAM-2a"));
        assertTrue(s.counters().isEmpty());
    }

    @Test
    void anEventLeavesTheOtherRungsOfAFreshStateAlone() {
        assertEquals(FRESH, LadderRules.apply(T, FRESH, List.of(HUMAN), TaskEvent.of(Detector.FEED, 1)));
        assertEquals(FRESH, LadderRules.apply(T, FRESH, List.of(), TaskEvent.of(Detector.FEED, 1)));
    }

    @Test
    void witchWerewolfClimbsBothLadders() {
        List<SupernaturalRace> races = List.of(WEREWOLF, WITCH);
        // a rite event: the witch ladder moves, the werewolf ladder does not
        SupernaturalState s = LadderRules.apply(T, FRESH, races,
                TaskEvent.of(Detector.RITE, 1, "rite", REVEAL, "role", "performer"));
        assertTrue(s.done().contains("L-WIT-1b"));
        assertEquals(1L, s.counters().get("L-WIT-1a"));
        assertFalse(s.done().stream().anyMatch(id -> id.startsWith("L-WOL")));
        assertFalse(s.counters().keySet().stream().anyMatch(id -> id.startsWith("L-WOL")));
        // a level event: the werewolf ladder moves only
        SupernaturalState lvl = LadderRules.apply(T, s, races, TaskEvent.of(Detector.LEVEL, 3, "faction", "werewolves:werewolf"));
        assertTrue(lvl.done().contains("L-WOL-1a"));
        assertEquals(s.counters(), lvl.counters());
        assertEquals(s.rung(WITCH), lvl.rung(WITCH));
        // claims go to the right ladder
        assertTrue(claim(lvl, WEREWOLF, WITCH, "L-WOL-1a").ok());
        assertTrue(claim(lvl, WEREWOLF, WITCH, "L-WIT-1b").ok());
    }

    @Test
    void oneEventCanMoveAndPayBothLadders() {
        String ladder = """
                { "rungs": [ { "id": "X-0" }, { "id": "X-1", "sp": 1, "tasks": [ { "id": "X-1a", "detector": "signal", "signal": "s" } ] } ] }
                """;
        LadderTable t = custom(mod -> true, "werewolf", ladder.replace("X-", "W-"), "witch", ladder.replace("X-", "T-"));
        SupernaturalState s = LadderRules.apply(t, FRESH, List.of(WEREWOLF, WITCH), TaskEvent.of(Detector.SIGNAL, 1, "signal", "s"));
        assertEquals(java.util.Set.of("W-1a", "T-1a"), s.done());
        ClaimResult w = LadderRules.claim(t, s, WEREWOLF, Optional.of(WITCH), "W-1a");
        ClaimResult m = LadderRules.claim(t, w.state(), WEREWOLF, Optional.of(WITCH), "T-1a");
        assertEquals(2, m.state().sp());
        assertEquals(1, m.state().rung(WEREWOLF));
        assertEquals(1, m.state().rung(WITCH));
    }

    @Test
    void claimRefusalsExplainThemselves() {
        SupernaturalState s = done(FRESH, "L-VAM-1a", "L-VAM-2a", "L-WOL-1a");
        assertEquals(Refusal.NOT_DONE, claim(FRESH, VAMPIRE, null, "L-VAM-1a").refusal());
        assertEquals(Refusal.UNKNOWN_TASK, claim(s, VAMPIRE, null, "L-VAM-9z").refusal());
        // another race's task
        assertEquals(Refusal.NOT_ACTIVE, claim(s, VAMPIRE, null, "L-WOL-1a").refusal());
        // a later rung of the right race
        assertEquals(Refusal.NOT_ACTIVE, claim(s, VAMPIRE, null, "L-VAM-2a").refusal());
        ClaimResult paid = claim(s, VAMPIRE, null, "L-VAM-1a");
        assertTrue(paid.ok());
        assertEquals(Refusal.ALREADY_CLAIMED, claim(paid.state(), VAMPIRE, null, "L-VAM-1a").refusal());
        assertEquals(paid.state(), claim(paid.state(), VAMPIRE, null, "L-VAM-1a").state());
        assertEquals("supernaturalbloodlines.supernatural.claim.not_done", Refusal.NOT_DONE.langKey());
        assertEquals("supernaturalbloodlines.supernatural.claim.ghoul_frozen", Refusal.GHOUL_FROZEN.langKey());
    }

    @Test
    void aReachedRungIsAHighWaterMarkAndItsTasksStayClaimed() {
        SupernaturalState s = done(FRESH, "L-VAM-1a", "L-VAM-1b");
        s = claim(claim(s, VAMPIRE, null, "L-VAM-1a").state(), VAMPIRE, null, "L-VAM-1b").state();
        assertEquals(1, s.rung(VAMPIRE));
        // becoming something else and back changes nothing
        assertEquals(0, s.rung(WEREWOLF));
        assertEquals(1, s.rung(VAMPIRE));
        assertEquals(Refusal.ALREADY_CLAIMED, claim(s, VAMPIRE, null, "L-VAM-1b").refusal());
    }

    @Test
    void activeTasksFollowTheRungsOfEachRace() {
        assertEquals(List.of("L-VAM-1a", "L-VAM-1b"), LadderRules.activeTasks(T, FRESH, List.of(VAMPIRE)).stream().map(TaskSpec::id).toList());
        assertEquals(4, LadderRules.activeTasks(T, FRESH, List.of(WEREWOLF, WITCH)).size());
        SupernaturalState top = FRESH.toBuilder().rung(VAMPIRE, 6).build();
        assertTrue(LadderRules.activeTasks(T, top, List.of(VAMPIRE)).isEmpty());
        assertTrue(LadderRules.activeRung(T, top, VAMPIRE).isEmpty());
        assertTrue(LadderRules.activeTasks(T, FRESH, List.of(GHOUL)).isEmpty());
    }

    @Test
    void markDoneDropsTheCounter() {
        SupernaturalState s = LadderRules.record(T, FRESH, VAMPIRE, "L-VAM-1b", 3);
        assertEquals(3L, s.counters().get("L-VAM-1b"));
        s = LadderRules.markDone(s, "L-VAM-1b");
        assertNull(s.counters().get("L-VAM-1b"));
        assertTrue(s.done().contains("L-VAM-1b"));
    }

    @Test
    void inTotalTasksCountFromTheStartWhateverTheRung() {
        // L-SIP-4a (50,000) sits three rungs ahead; siphoning from the first day already counts
        TaskEvent siphon = TaskEvent.of(Detector.SIGNAL, 1000, "signal", "siphon_power");
        SupernaturalState s = FRESH;
        for (int i = 0; i < 12; i++) {
            s = LadderRules.apply(T, s, List.of(HUMAN, SIPHONER), siphon);
        }
        assertEquals(12_000L, s.lifetime().get("siphoner/signal?signal=siphon_power"));
        // the active tasks (rung 1: 2,000; rung 2 would be 10,000) are done from the count, not from a counter of their own
        assertTrue(s.done().contains("L-SIP-1a"));
        assertTrue(s.counters().isEmpty());
        // the rung after: 2b (10,000) is already enough the moment it becomes active
        SupernaturalState rung2 = s.toBuilder().rung(SIPHONER, 1).build();
        assertFalse(rung2.done().contains("L-SIP-2b"));
        SupernaturalState refreshed = LadderRules.refreshTotals(T, rung2, List.of(HUMAN, SIPHONER));
        assertTrue(refreshed.done().contains("L-SIP-2b"));
        assertFalse(refreshed.done().contains("L-SIP-2a"));
    }

    @Test
    void theHereticKeepsItsOwnTotal() {
        TaskEvent siphon = TaskEvent.of(Detector.SIGNAL, 2000, "signal", "siphon_power");
        SupernaturalState s = LadderRules.apply(T, FRESH, List.of(HERETIC), siphon);
        assertTrue(s.done().contains("L-HER-1b"));
        assertEquals(2000L, s.lifetime().get("heretic/signal?signal=siphon_power"));
        assertFalse(s.lifetime().containsKey("siphoner/signal?signal=siphon_power"));
    }

    @Test
    void aRiteTotalDoesNotCountOtherRoles() {
        TaskEvent target = TaskEvent.of(Detector.RITE, 1, "rite", "x:y", "role", "target");
        assertTrue(LadderRules.apply(T, FRESH, List.of(HUMAN, WITCH), target).lifetime().isEmpty());
        TaskEvent performer = TaskEvent.of(Detector.RITE, 1, "rite", "x:y", "role", "performer");
        assertEquals(1L, LadderRules.apply(T, FRESH, List.of(HUMAN, WITCH), performer).lifetime().get("witch/rite?rite=any&role=performer"));
    }

    @Test
    void aGhoulFreezesTheMagicLadderAndTheTotalsToo() {
        TaskEvent siphon = TaskEvent.of(Detector.SIGNAL, 5000, "signal", "siphon_power");
        assertEquals(FRESH, LadderRules.apply(T, FRESH, List.of(GHOUL, SIPHONER), siphon));
        SupernaturalState rich = FRESH.toBuilder().lifetime(Map.of("siphoner/signal?signal=siphon_power", 99_999L)).build();
        assertEquals(rich, LadderRules.refreshTotals(T, rich, List.of(GHOUL, SIPHONER)));
        assertTrue(LadderRules.refreshTotals(T, rich, List.of(SIPHONER)).done().contains("L-SIP-1a"));
    }

    @Test
    void thirstDaysCountInTicks() {
        // L-HYB-2a: 3 days at thirst 15+, fed 200 ticks at a time
        SupernaturalState s = FRESH.toBuilder().rung(HYBRID, 1).build();
        TaskEvent tick = TaskEvent.of(Detector.THIRST_DAYS, 200, "min", "15");
        for (int i = 0; i < 359; i++) {
            s = LadderRules.apply(T, s, List.of(HYBRID), tick);
        }
        assertFalse(s.done().contains("L-HYB-2a"));
        s = LadderRules.apply(T, s, List.of(HYBRID), tick);
        assertTrue(s.done().contains("L-HYB-2a"));
    }

    @Test
    void theApexOfferNeedsTheTopRungOfThatRacesLadder() {
        // every shipped ladder ends with its apex rung (L-VAM-6, L-ORI-5, ..., L-WOL-6)
        for (SupernaturalRace r : SupernaturalRace.values()) {
            if (r == HUMAN || r == GHOUL) {
                assertTrue(LadderRules.apexRungReached(T, FRESH, r), r + " has no apex rung to ask for");
                continue;
            }
            int top = T.get(r).rungs().size() - 1;
            assertFalse(LadderRules.apexRungReached(T, FRESH.toBuilder().rung(r, top - 1).build(), r), r + " one below the top");
            assertTrue(LadderRules.apexRungReached(T, FRESH.toBuilder().rung(r, top).build(), r), r + " at the top");
        }
        // a witch werewolf has two ladders, each its own rung
        SupernaturalState s = FRESH.toBuilder().rung(WEREWOLF, 6).rung(WITCH, 4).build();
        assertTrue(LadderRules.apexRungReached(T, s, WEREWOLF));
        assertFalse(LadderRules.apexRungReached(T, s, WITCH));
    }

    @Test
    void aLadderWithoutAnApexRungAsksForNothing() {
        String ladder = """
                { "rungs": [ { "id": "X-0" },
                    { "id": "X-1", "sp": 1, "tasks": [ { "id": "X-1a", "detector": "signal", "signal": "s" } ] },
                    { "id": "X-2", "mods": ["tensura"], "tasks": [ { "id": "X-2a", "detector": "tensura_tier" } ] } ] }
                """;
        LadderTable without = custom(mod -> !mod.equals("tensura"), "vampire", ladder);
        assertEquals(2, without.get(VAMPIRE).rungs().size());
        assertTrue(LadderRules.apexRungReached(without, FRESH, VAMPIRE));
        LadderTable with = custom(mod -> true, "vampire", ladder);
        assertFalse(LadderRules.apexRungReached(with, FRESH.toBuilder().rung(VAMPIRE, 1).build(), VAMPIRE));
        assertTrue(LadderRules.apexRungReached(with, FRESH.toBuilder().rung(VAMPIRE, 2).build(), VAMPIRE));
    }

    /** A werewolf witch may be at the apex tier of the human-like line for both, or of Beastfolk for the werewolf only. */
    @Test
    void tensuraTierMovesOnlyTheLadderOfTheRaceItNames() {
        SupernaturalState s = FRESH.toBuilder().rung(WEREWOLF, 5).rung(WITCH, 4).build();
        List<SupernaturalRace> races = List.of(WEREWOLF, WITCH);
        SupernaturalState wolf = LadderRules.apply(T, s, races, TaskEvent.of(Detector.TENSURA_TIER, 1, "race", "werewolf"));
        assertTrue(wolf.done().contains("L-WOL-6a"));
        assertFalse(wolf.done().contains("L-WIT-5a"));
        SupernaturalState both = LadderRules.apply(T, wolf, races, TaskEvent.of(Detector.TENSURA_TIER, 1, "race", "witch"));
        assertTrue(both.done().contains("L-WIT-5a"));
        // an event that names no race (an older sender) still moves every ladder, as every other detector does
        assertTrue(LadderRules.apply(T, s, races, TaskEvent.of(Detector.TENSURA_TIER, 1)).done().containsAll(List.of("L-WOL-6a", "L-WIT-5a")));
    }
}
