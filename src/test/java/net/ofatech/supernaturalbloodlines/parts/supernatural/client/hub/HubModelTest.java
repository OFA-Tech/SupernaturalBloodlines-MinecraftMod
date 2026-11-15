package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

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
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.RungStatus;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.TaskRow;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel.TaskState;
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
        LadderView v = HubModel.ladders(T, state(0, Set.of(), Set.of("W-1b"), Map.of()), List.of(WEREWOLF)).get(0);
        assertEquals(List.of(RungStatus.REACHED, RungStatus.CURRENT, RungStatus.LOCKED), v.rungs().stream().map(HubModel.RungRow::status).toList());
        assertEquals("W-1", v.active().orElseThrow().id());
        assertEquals(1, v.active().orElseThrow().claimed());
        assertEquals(4, v.active().orElseThrow().tasks());
    }

    @Test
    void tasksSortClaimableFirstThenOpenThenClaimed() {
        SupernaturalState s = state(0, Set.of("W-1b", "W-1c.2"), Set.of("W-1b"), Map.of("W-1a", 2L));
        List<TaskRow> rows = HubModel.ladders(T, s, List.of(WEREWOLF)).get(0).tasks();
        assertEquals(List.of("W-1c", "W-1a", "W-1d", "W-1b"), rows.stream().map(r -> r.task().id()).toList());
        assertEquals(List.of(TaskState.CLAIMABLE, TaskState.OPEN, TaskState.OPEN, TaskState.CLAIMED), rows.stream().map(TaskRow::state).toList());
    }

    @Test
    void countsAreShownInDataUnits() {
        SupernaturalState s = state(0, Set.of(), Set.of(), Map.of("W-1a", 2L, "W-1d", 30_000L));
        Map<String, TaskRow> rows = byId(HubModel.ladders(T, s, List.of(WEREWOLF)).get(0).tasks());
        assertEquals(2, rows.get("W-1a").progress());
        assertEquals(3, rows.get("W-1a").target());
        assertEquals(1, rows.get("W-1d").progress()); // 30,000 ticks is one whole day
        assertEquals(2, rows.get("W-1d").target());
        assertEquals(false, rows.get("W-1b").counted()); // a level is done or not
        assertEquals(2, rows.get("W-1c").members().size());
    }

    @Test
    void aGhoulSeesEveryTaskFrozen() {
        SupernaturalState s = state(0, Set.of("W-1b"), Set.of(), Map.of());
        List<LadderView> v = HubModel.ladders(T, s, List.of(GHOUL));
        assertEquals(GHOUL, v.get(0).race());
        assertTrue(v.get(0).tasks().isEmpty()); // no ghoul ladder in this table: nothing to show, nothing to claim
        TaskRow row = HubModel.row(WEREWOLF, T.find("W-1b").orElseThrow().task(), s, true);
        assertEquals(TaskState.FROZEN, row.state());
    }

    @Test
    void aWitchWerewolfGetsBothLaddersCreatureFirst() {
        List<LadderView> v = HubModel.ladders(T, SupernaturalState.NONE, List.of(WEREWOLF, WITCH));
        assertEquals(List.of(WEREWOLF, WITCH), v.stream().map(LadderView::race).toList());
        assertEquals("C-1", v.get(1).active().orElseThrow().id());
    }

    @Test
    void theTopOfALadderHasNoActiveRung() {
        LadderView v = HubModel.ladders(T, state(2, Set.of(), Set.of(), Map.of()), List.of(WEREWOLF)).get(0);
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
        assertEquals(List.of(new HubModel.Key("supernaturalbloodlines.hub.faction.werewolves.werewolf", "Werewolf"), new HubModel.Num(4)), level.args());
        assertEquals("supernaturalbloodlines.hub.task.id.W-1b", level.override());
    }

    @Test
    void pickKeepsTheSelectionElseTheFirstClaimable() {
        SupernaturalState s = state(0, Set.of("W-1d"), Set.of(), Map.of());
        List<TaskRow> rows = HubModel.ladders(T, s, List.of(WEREWOLF)).get(0).tasks();
        assertEquals("W-1a", HubModel.pick(rows, "W-1a").orElseThrow().task().id());
        assertEquals("W-1d", HubModel.pick(rows, "gone").orElseThrow().task().id());
        assertEquals(Optional.empty(), HubModel.pick(List.of(), null));
    }

    @Test
    void theModelOffersAClaimOnlyWhereTheRulesAllowOne() {
        SupernaturalState s = state(0, Set.of("W-1a", "W-1b"), Set.of("W-1b"), Map.of());
        for (TaskRow r : HubModel.ladders(T, s, List.of(WEREWOLF)).get(0).tasks()) {
            boolean allowed = LadderRules.claim(T, s, WEREWOLF, Optional.empty(), r.task().id()).ok();
            assertEquals(allowed, r.state() == TaskState.CLAIMABLE, r.task().id());
        }
    }

    private static Map<String, TaskRow> byId(List<TaskRow> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(r -> r.task().id(), r -> r));
    }
}
