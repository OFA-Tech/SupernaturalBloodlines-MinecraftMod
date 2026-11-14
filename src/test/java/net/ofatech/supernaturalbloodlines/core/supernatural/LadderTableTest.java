package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LadderTableTest {
    private static final String GOOD = """
            { "rungs": [
              { "id": "A-0", "sp": 0, "ep": 0 },
              { "id": "A-1", "name": "One", "sp": 3, "ep": 100, "tasks": [
                { "id": "A-1a", "detector": "feed", "n": 4 },
                { "id": "A-1b", "any_of": [
                  { "id": "A-1b.1", "detector": "rite", "rite": "any", "role": "target", "mod": "enchanted" },
                  { "id": "A-1b.2", "detector": "state", "predicate": "moon_unbound" } ] } ] } ] }
            """;

    private static Map<String, JsonElement> files(String... keyAndJson) {
        Map<String, JsonElement> m = new HashMap<>();
        for (int i = 0; i < keyAndJson.length; i += 2) {
            m.put(keyAndJson[i], JsonParser.parseString(keyAndJson[i + 1]));
        }
        return m;
    }

    private static LadderTable.Parsed parse(String... keyAndJson) {
        return LadderTable.parse(files(keyAndJson), mod -> true);
    }

    @Test
    void parsesRungsTasksGroupsAndTargets() {
        LadderTable.Parsed p = parse("vampire", GOOD);
        assertEquals(List.of(), p.rejected());
        Ladder l = p.table().get(SupernaturalRace.VAMPIRE);
        assertEquals(2, l.rungs().size());
        Rung one = l.rung(1).orElseThrow();
        assertEquals("One", one.name());
        assertEquals(3, one.sp());
        assertEquals(100, one.ep());
        TaskSpec feed = one.tasks().get(0);
        assertEquals(TaskSpec.Detector.FEED, feed.detector());
        assertEquals(4, feed.target());
        TaskSpec group = one.tasks().get(1);
        assertTrue(group.isGroup());
        assertEquals(List.of("A-1b.1", "A-1b.2"), group.leaves().stream().map(TaskSpec::id).toList());
        assertEquals("enchanted", group.leaves().get(0).mod());
        assertEquals("A-1b", p.table().find("A-1b").orElseThrow().task().id());
        assertEquals(1, p.table().find("A-1a").orElseThrow().rung());
    }

    @Test
    void dayUnitsAreTicksForThirstDays() {
        TaskSpec t = parse("hybrid", GOOD.replace("{ \"id\": \"A-1a\", \"detector\": \"feed\", \"n\": 4 }",
                "{ \"id\": \"A-1a\", \"detector\": \"thirst_days\", \"min\": \"15\", \"n\": 3 }"))
                .table().get(SupernaturalRace.HYBRID).rung(1).orElseThrow().tasks().get(0);
        assertEquals(72_000, t.target());
        assertEquals("15", t.args().get("min"));
    }

    @Test
    void aBadDetectorRejectsItsFileAndTheGoodOneSurvives() {
        LadderTable.Parsed p = parse("vampire", GOOD, "witch", GOOD.replace("A-", "B-").replace("\"feed\"", "\"juggle\""));
        assertEquals(1, p.rejected().size());
        assertTrue(p.rejected().get(0).startsWith("witch: "), p.rejected().get(0));
        assertTrue(p.rejected().get(0).contains("juggle"));
        assertEquals(2, p.table().get(SupernaturalRace.VAMPIRE).rungs().size());
        assertTrue(p.table().get(SupernaturalRace.WITCH).rungs().isEmpty());
    }

    @Test
    void anUnknownRaceIsRejected() {
        LadderTable.Parsed p = parse("dragon", GOOD, "vampire", GOOD);
        assertEquals(1, p.rejected().size());
        assertTrue(p.rejected().get(0).startsWith("dragon: unknown race"), p.rejected().get(0));
        assertEquals(2, p.table().get(SupernaturalRace.VAMPIRE).rungs().size());
    }

    @Test
    void aDuplicateIdInOneFileIsRejected() {
        LadderTable.Parsed p = parse("vampire", GOOD.replace("A-1b.1", "A-1a"));
        assertEquals(1, p.rejected().size());
        assertTrue(p.rejected().get(0).contains("duplicate id A-1a"), p.rejected().get(0));
        assertTrue(p.table().get(SupernaturalRace.VAMPIRE).rungs().isEmpty());
    }

    @Test
    void aDuplicateIdAcrossFilesRejectsTheLaterFileOnly() {
        LadderTable.Parsed p = parse("vampire", GOOD, "witch", GOOD);
        assertEquals(1, p.rejected().size());
        assertTrue(p.rejected().get(0).startsWith("witch: "), p.rejected().get(0));
        assertEquals(2, p.table().get(SupernaturalRace.VAMPIRE).rungs().size());
        assertEquals(SupernaturalRace.VAMPIRE, p.table().find("A-1a").orElseThrow().race());
    }

    @Test
    void otherBadShapesAreRejected() {
        String[] bad = {
                "{}",
                "{ \"rungs\": [] }",
                "{ \"rungs\": [ { \"id\": \"A-0\", \"sp\": 1 } ] }",
                "{ \"rungs\": [ { \"id\": \"A-0\", \"tasks\": [ { \"id\": \"A-0a\", \"detector\": \"feed\" } ] } ] }",
                GOOD.replace("\"n\": 4", "\"n\": 0"),
                GOOD.replace("\"sp\": 3", "\"sp\": -3"),
                GOOD.replace("\"role\": \"target\"", "\"role\": \"bystander\""),
                GOOD.replace("\"role\": \"target\", ", ""),
                GOOD.replace("\"predicate\": \"moon_unbound\"", "\"predicates\": \"moon_unbound\""),
                GOOD.replace("{ \"id\": \"A-1b.2\"", "{ \"id\": \"A-1b.2\", \"any_of\": [ { \"id\": \"x\", \"detector\": \"feed\" } ] }, { \"id\": \"A-1b.3\""),
                GOOD.replace("\"detector\": \"feed\"", "\"detector\": \"days_as\", \"race\": \"dragon\""),
                GOOD.replace("\"n\": 4", "\"n\": \"many\""),
                GOOD.replace("\"id\": \"A-1b\", \"any_of\"", "\"id\": \"A-1b\", \"detector\": \"feed\", \"any_of\""),
        };
        for (String json : bad) {
            LadderTable.Parsed p = parse("vampire", json);
            assertEquals(1, p.rejected().size(), json);
            assertTrue(p.table().get(SupernaturalRace.VAMPIRE).rungs().isEmpty(), json);
        }
    }

    @Test
    void aMissingModDropsTheTaskAndTheGroupMember() {
        LadderTable t = LadderTable.parse(files("vampire", GOOD), mod -> !mod.equals("enchanted")).table();
        TaskSpec group = t.get(SupernaturalRace.VAMPIRE).rung(1).orElseThrow().tasks().get(1);
        assertEquals(List.of("A-1b.2"), group.leaves().stream().map(TaskSpec::id).toList());

        String onlyEnchanted = GOOD.replace("{ \"id\": \"A-1b.2\", \"detector\": \"state\", \"predicate\": \"moon_unbound\" }",
                "{ \"id\": \"A-1b.2\", \"detector\": \"state\", \"predicate\": \"x\", \"mod\": \"enchanted\" }");
        Rung rung = LadderTable.parse(files("vampire", onlyEnchanted), mod -> !mod.equals("enchanted")).table()
                .get(SupernaturalRace.VAMPIRE).rung(1).orElseThrow();
        assertEquals(List.of("A-1a"), rung.tasks().stream().map(TaskSpec::id).toList());
    }

    @Test
    void aLadderEndsBeforeTheFirstRungWithAMissingMod() {
        String json = """
                { "rungs": [ { "id": "A-0" }, { "id": "A-1", "sp": 1 }, { "id": "A-2", "mods": ["tensura"] }, { "id": "A-3" } ] }
                """;
        assertEquals(4, LadderTable.parse(files("vampire", json), mod -> true).table().get(SupernaturalRace.VAMPIRE).rungs().size());
        assertEquals(2, LadderTable.parse(files("vampire", json), mod -> !mod.equals("tensura")).table()
                .get(SupernaturalRace.VAMPIRE).rungs().size());
    }

    @Test
    void currentIsReplaceable() {
        LadderTable t = parse("vampire", GOOD).table();
        LadderTable.setCurrent(t);
        try {
            assertTrue(LadderTable.current() == t);
        } finally {
            LadderTable.setCurrent(LadderTable.EMPTY);
        }
    }

    @Test
    void matchingUsesArgsAndAny() {
        TaskSpec any = parse("vampire", GOOD).table().find("A-1b").orElseThrow().task().leaves().get(0);
        assertTrue(any.matches(TaskEvent.of(TaskSpec.Detector.RITE, 1, "rite", "x:y", "role", "target")));
        assertTrue(!any.matches(TaskEvent.of(TaskSpec.Detector.RITE, 1, "rite", "x:y", "role", "performer")));
        assertTrue(!any.matches(TaskEvent.of(TaskSpec.Detector.FEED, 1, "rite", "x:y", "role", "target")));
        // "any" does not even need the attribute
        assertTrue(any.matches(TaskEvent.of(TaskSpec.Detector.RITE, 1, "role", "target")));
    }
}
