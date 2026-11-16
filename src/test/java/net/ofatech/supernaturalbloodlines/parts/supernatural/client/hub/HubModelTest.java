package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.LadderView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.TaskRow;
import org.junit.jupiter.api.Test;

class HubModelTest {
    private static final LadderTable T = table();

    private static LadderTable table() {
        String wolf = """
                { "rungs": [
                  { "id": "W-0", "name": "Wolf", "sp": 0, "ep": 0 },
                  { "id": "W-1", "name": "Bitten", "sp": 3, "ep": 300, "tasks": [
                    { "id": "W-1a", "detector": "kill", "category": "prey", "n": 3 },
                    { "id": "W-1b", "detector": "level", "faction": "werewolves:werewolf", "n": 4 },
                    { "id": "W-1c", "any_of": [
                      { "id": "W-1c.1", "detector": "rite", "rite": "supernaturalbloodlines:rite_of_the_moon", "role": "target" },
                      { "id": "W-1c.2", "detector": "state", "predicate": "moon_unbound" } ] },
                    { "id": "W-1d", "detector": "pack_days", "n": 2 } ] },
                  { "id": "W-2", "name": "Beta", "sp": 4, "ep": 0, "tasks": [
                    { "id": "W-2a", "detector": "tensura_tier" } ] } ] }
                """;
        String witch = """
                { "rungs": [
                  { "id": "C-0", "name": "Called", "sp": 0, "ep": 0 },
                  { "id": "C-1", "name": "Apprentice", "sp": 2, "ep": 0, "tasks": [
                    { "id": "C-1a", "detector": "rite", "rite": "any", "role": "performer", "n": 3 } ] } ] }
                """;
        LadderTable.Parsed p = LadderTable.parse(Map.of("werewolf", JsonParser.parseString(wolf), "witch", JsonParser.parseString(witch)), m -> true);
        assertEquals(List.of(), p.rejected());
        return p.table();
    }

    private static SupernaturalState state(int wolfRung, Set<String> done, Set<String> claimed, Map<String, Long> counters) {
        return SupernaturalState.NONE.toBuilder().rung(WEREWOLF, wolfRung).done(done).claimed(claimed).counters(counters).build();
    }

    @Test
    void ladderMarksReachedCurrentAndLockedRungs() {
        LadderView v = HubModel.ladders(T, state(0, Set.of(), Set.of("W-1b"), Map.of()), List.of(WEREWOLF), Map.of()).get(0);
        assertEquals(List.of(RungStatus.REACHED, RungStatus.CURRENT, RungStatus.LOCKED), v.rungs().stream().map(HubModel.RungRow::status).toList());
        assertEquals("W-1", v.active().orElseThrow().id());
        assertEquals(1, v.active().orElseThrow().claimed());
        assertEquals(4, v.active().orElseThrow().tasks());
    }

    @Test
    void aRiteTaskIsUnavailableWhileTheMixinHookCannotSeeRites() {
        try {
            HubModel.ritesCountWhen(() -> false);
            // W-1c holds the rite as one of two options (a group: unchanged); a leaf rite task is the witch ladder's C-1a
            List<TaskRow> witch = HubModel.ladders(T, SupernaturalState.NONE.toBuilder().rung(WITCH, 0).build(), List.of(WITCH), Map.of()).get(0).tasks();
            assertEquals(List.of(TaskState.UNAVAILABLE), witch.stream().map(TaskRow::state).toList());
            List<TaskRow> wolf = HubModel.ladders(T, state(0, Set.of(), Set.of(), Map.of()), List.of(WEREWOLF), Map.of()).get(0).tasks();
            assertTrue(wolf.stream().noneMatch(r -> r.task().id().equals("W-1a") && r.state() == TaskState.UNAVAILABLE), "a kill task is not a rite task");
            HubModel.ritesCountWhen(() -> true);
            assertEquals(List.of(TaskState.OPEN), HubModel.ladders(T, SupernaturalState.NONE.toBuilder().rung(WITCH, 0).build(), List.of(WITCH), Map.of()).get(0).tasks()
                    .stream().map(TaskRow::state).toList());
        } finally {
            HubModel.ritesCountWhen(() -> true);
        }
    }

    @Test
    void tasksSortClaimableFirstThenOpenThenClaimed() {
        SupernaturalState s = state(0, Set.of("W-1b", "W-1c.2"), Set.of("W-1b"), Map.of("W-1a", 2L));
        List<TaskRow> rows = HubModel.ladders(T, s, List.of(WEREWOLF), Map.of()).get(0).tasks();
        assertEquals(List.of("W-1c", "W-1a", "W-1d", "W-1b"), rows.stream().map(r -> r.task().id()).toList());
        assertEquals(List.of(TaskState.CLAIMABLE, TaskState.OPEN, TaskState.OPEN, TaskState.CLAIMED), rows.stream().map(TaskRow::state).toList());
    }

    @Test
    void countsAreShownInDataUnits() {
        SupernaturalState s = state(0, Set.of(), Set.of(), Map.of("W-1a", 2L, "W-1d", 30_000L));
        Map<String, TaskRow> rows = byId(HubModel.ladders(T, s, List.of(WEREWOLF), Map.of()).get(0).tasks());
        assertEquals(2, rows.get("W-1a").progress());
        assertEquals(3, rows.get("W-1a").target());
        assertEquals(1, rows.get("W-1d").progress()); // 30,000 ticks is one whole day
        assertEquals(2, rows.get("W-1d").target());
        assertEquals(0, rows.get("W-1b").progress()); // not in the werewolf faction: level 0 of 4
        assertEquals(2, rows.get("W-1c").members().size());
    }

    @Test
    void aGhoulSeesEveryTaskFrozen() {
        SupernaturalState s = state(0, Set.of("W-1b"), Set.of(), Map.of());
        List<LadderView> v = HubModel.ladders(T, s, List.of(GHOUL), Map.of());
        assertEquals(GHOUL, v.get(0).race());
        assertTrue(v.get(0).tasks().isEmpty()); // no ghoul ladder in this table: nothing to show, nothing to claim
        TaskRow row = HubModel.row(WEREWOLF, T.find("W-1b").orElseThrow().task(), s, true, Map.of());
        assertEquals(TaskState.FROZEN, row.state());
    }

    @Test
    void aWitchWerewolfGetsBothLaddersCreatureFirst() {
        List<LadderView> v = HubModel.ladders(T, SupernaturalState.NONE, List.of(WEREWOLF, WITCH), Map.of());
        assertEquals(List.of(WEREWOLF, WITCH), v.stream().map(LadderView::race).toList());
        assertEquals("C-1", v.get(1).active().orElseThrow().id());
    }

    @Test
    void theTopOfALadderHasNoActiveRung() {
        LadderView v = HubModel.ladders(T, state(2, Set.of(), Set.of(), Map.of()), List.of(WEREWOLF), Map.of()).get(0);
        assertTrue(v.atTop());
        assertTrue(v.tasks().isEmpty());
    }

    @Test
    void sourcesFollowTheModTheTaskIsAbout() {
        assertEquals("species", HubModel.source(T.find("W-1a").orElseThrow().task()));
        assertEquals("werewolves", HubModel.source(T.find("W-1b").orElseThrow().task()));
        assertEquals("species", HubModel.source(T.find("W-1c").orElseThrow().task())); // a rite or our state: mixed, ours
        assertEquals("enchanted", HubModel.source(T.find("C-1a").orElseThrow().task()));
        assertEquals("tensura", HubModel.source(T.find("W-2a").orElseThrow().task()));
    }

    @Test
    void textNamesTheRiteByEnchantedsKeyAndTheLevel() {
        HubModel.TaskText rite = HubModel.text(T.find("W-1c").orElseThrow().task().anyOf().get(0));
        assertEquals("supernaturalbloodlines.hub.task.rite.target", rite.key());
        assertEquals(new HubModel.Key("rite.supernaturalbloodlines.rite_of_the_moon", "Rite of the moon"), rite.args().get(0));
        HubModel.TaskText level = HubModel.text(T.find("W-1b").orElseThrow().task());
        assertEquals(List.of(new HubModel.Key("supernaturalbloodlines.hub.faction.werewolves.werewolf", "Werewolf")), level.args()); // the number is the progress, once
        assertEquals("supernaturalbloodlines.hub.task.id.W-1b", level.override());
    }

    @Test
    void pickKeepsTheSelectionElseTheFirstClaimable() {
        SupernaturalState s = state(0, Set.of("W-1d"), Set.of(), Map.of());
        List<TaskRow> rows = HubModel.ladders(T, s, List.of(WEREWOLF), Map.of()).get(0).tasks();
        assertEquals("W-1a", HubModel.pick(rows, "W-1a").orElseThrow().task().id());
        assertEquals("W-1d", HubModel.pick(rows, "gone").orElseThrow().task().id());
        assertEquals(Optional.empty(), HubModel.pick(List.of(), null));
    }

    @Test
    void theModelOffersAClaimOnlyWhereTheRulesAllowOne() {
        SupernaturalState s = state(0, Set.of("W-1a", "W-1b"), Set.of("W-1b"), Map.of());
        for (TaskRow r : HubModel.ladders(T, s, List.of(WEREWOLF), Map.of()).get(0).tasks()) {
            boolean allowed = LadderRules.claim(T, s, WEREWOLF, Optional.empty(), r.task().id()).ok();
            assertEquals(allowed, r.state() == TaskState.CLAIMABLE, r.task().id());
        }
    }

    @Test
    void aLevelStepShowsTheLevelReachedSoFar() {
        Map<String, TaskRow> rows = byId(HubModel.ladders(T, SupernaturalState.NONE, List.of(WEREWOLF), Map.of("werewolves:werewolf", 1)).get(0).tasks());
        assertEquals(1, rows.get("W-1b").progress());
        assertEquals(4, rows.get("W-1b").target());
        assertTrue(rows.get("W-1b").counted());
        Map<String, TaskRow> above = byId(HubModel.ladders(T, SupernaturalState.NONE, List.of(WEREWOLF), Map.of("werewolves:werewolf", 9)).get(0).tasks());
        assertEquals(4, above.get("W-1b").progress()); // capped at the target
    }

    @Test
    void claimParityAboveRungZeroWithAMagicRaceAndAFrozenLadder() {
        // werewolf on rung 1 (its rung 2 task done), witch on rung 0 (its task done): both ladders offer exactly what the rules accept
        SupernaturalState s = SupernaturalState.NONE.toBuilder().rung(WEREWOLF, 1).done(Set.of("W-2a", "C-1a")).build();
        List<LadderView> views = HubModel.ladders(T, s, List.of(WEREWOLF, WITCH), Map.of());
        assertEquals("W-2", views.get(0).active().orElseThrow().id());
        int offered = 0;
        for (LadderView v : views) {
            for (TaskRow r : v.tasks()) {
                boolean allowed = LadderRules.claim(T, s, WEREWOLF, Optional.of(WITCH), r.task().id()).ok();
                assertEquals(allowed, r.state() == TaskState.CLAIMABLE, r.task().id());
                offered += allowed ? 1 : 0;
            }
        }
        assertEquals(2, offered);
        // a task of a rung that is not active is never offered, and the rules refuse it
        assertTrue(!LadderRules.claim(T, s, WEREWOLF, Optional.of(WITCH), "W-1a").ok());
        // a Ghoul: everything frozen, and the rules refuse every claim
        for (LadderView v : HubModel.ladders(T, s, List.of(GHOUL, WITCH), Map.of())) {
            for (TaskRow r : v.tasks()) {
                assertEquals(TaskState.FROZEN, r.state(), r.task().id());
                assertTrue(!LadderRules.claim(T, s, GHOUL, Optional.of(WITCH), r.task().id()).ok());
            }
        }
    }

    private static Map<String, TaskRow> byId(List<TaskRow> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(r -> r.task().id(), r -> r));
    }
}
