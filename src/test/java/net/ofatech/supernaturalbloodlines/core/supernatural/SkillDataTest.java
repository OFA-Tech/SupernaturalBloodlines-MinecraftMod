package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The shipped skill JSON against the design: every id, cost and tree row of the spec's catalogue and "Trees per race". */
class SkillDataTest {
    // id -> cost, in the order of the spec's catalogue table
    private static final Map<String, Integer> COSTS = Map.ofEntries(
            Map.entry("S-HUM-1", 1), Map.entry("S-HUM-2", 2), Map.entry("S-HUM-3", 2),
            Map.entry("S-VAM-1", 1), Map.entry("S-VAM-2", 2), Map.entry("S-VAM-3", 2), Map.entry("S-VAM-4", 3), Map.entry("S-VAM-5", 3),
            Map.entry("S-VAM-6", 4), Map.entry("S-VAM-7", 4),
            Map.entry("S-ORI-1", 2), Map.entry("S-ORI-2", 1), Map.entry("S-ORI-3", 3), Map.entry("S-ORI-4", 5),
            Map.entry("S-HYB-1", 2), Map.entry("S-HYB-2", 3),
            Map.entry("S-WOL-1", 1), Map.entry("S-WOL-2", 2), Map.entry("S-WOL-3", 2), Map.entry("S-WOL-4", 2), Map.entry("S-WOL-5", 3),
            Map.entry("S-WOL-6", 3), Map.entry("S-WOL-7", 4),
            Map.entry("S-WIT-1", 2), Map.entry("S-WIT-2", 2), Map.entry("S-WIT-3", 2), Map.entry("S-WIT-4", 3), Map.entry("S-WIT-5", 3),
            Map.entry("S-WIT-6", 4), Map.entry("S-WIT-7", 4),
            Map.entry("S-SIP-1", 2), Map.entry("S-SIP-2", 2), Map.entry("S-SIP-3", 3), Map.entry("S-SIP-4", 3), Map.entry("S-SIP-5", 4),
            Map.entry("S-HUN-1", 2), Map.entry("S-HUN-2", 2), Map.entry("S-HUN-3", 3), Map.entry("S-HUN-4", 3), Map.entry("S-HUN-5", 4));

    private static final Set<String> TENSURA = Set.of("S-VAM-5", "S-WOL-3", "S-WIT-5", "S-HUN-4");

    // race -> rows "skill@rung" or "skill@rung>after", "+Q" when the quest Q-VAM-6 is needed; the spec's tables, row by row
    private static final Map<SupernaturalRace, List<String>> TREES = Map.ofEntries(
            Map.entry(SupernaturalRace.HUMAN, List.of("S-HUM-1@1", "S-HUM-2@2", "S-HUM-3@2")),
            Map.entry(SupernaturalRace.VAMPIRE, List.of("S-VAM-1@1", "S-VAM-2@2", "S-VAM-3@3", "S-VAM-4@3>S-VAM-2", "S-VAM-5@4",
                    "S-VAM-6@4>S-VAM-1+Q", "S-VAM-7@5>S-VAM-4")),
            Map.entry(SupernaturalRace.ORIGINAL, List.of("S-VAM-1@1", "S-ORI-1@1", "S-ORI-2@2", "S-VAM-3@2", "S-VAM-4@3>S-ORI-1",
                    "S-ORI-3@4>S-VAM-3", "S-VAM-6@3>S-VAM-1+Q", "S-ORI-4@5>S-VAM-4")),
            Map.entry(SupernaturalRace.ORIGINAL_HYBRID, List.of("S-VAM-1@1", "S-ORI-1@1", "S-HYB-1@2", "S-HYB-2@2", "S-VAM-3@3",
                    "S-ORI-3@4>S-VAM-3", "S-VAM-6@3>S-VAM-1+Q", "S-WOL-7@5>S-HYB-2")),
            Map.entry(SupernaturalRace.HYBRID, List.of("S-VAM-1@1", "S-WOL-1@1", "S-HYB-1@2", "S-VAM-2@2", "S-HYB-2@3", "S-VAM-3@3",
                    "S-VAM-6@4>S-VAM-1+Q", "S-WOL-7@5>S-HYB-2")),
            Map.entry(SupernaturalRace.TRIBRID, List.of("S-WIT-1@1", "S-HYB-1@1", "S-WIT-2@2>S-WIT-1", "S-HYB-2@2", "S-WIT-4@3",
                    "S-VAM-6@3+Q", "S-WOL-7@5>S-HYB-2")),
            Map.entry(SupernaturalRace.WITCH, List.of("S-WIT-1@1", "S-WIT-2@1", "S-WIT-3@2", "S-WIT-4@3", "S-WIT-5@3", "S-WIT-6@4>S-WIT-1",
                    "S-WIT-7@5>S-WIT-3")),
            Map.entry(SupernaturalRace.SIPHONER, List.of("S-SIP-1@1", "S-SIP-2@1", "S-SIP-3@2>S-SIP-2", "S-WIT-5@3", "S-SIP-4@3>S-SIP-1",
                    "S-SIP-5@4>S-SIP-3")),
            Map.entry(SupernaturalRace.HERETIC, List.of("S-VAM-1@1", "S-SIP-1@1", "S-SIP-3@2", "S-VAM-3@3", "S-VAM-4@3",
                    "S-VAM-6@3>S-VAM-1+Q", "S-SIP-5@4>S-SIP-3")),
            Map.entry(SupernaturalRace.HUNTER, List.of("S-HUM-2@1", "S-HUN-1@1", "S-HUM-3@2", "S-HUN-2@3", "S-HUN-3@3", "S-HUN-4@4>S-HUN-2",
                    "S-HUN-5@5>S-HUN-1")),
            Map.entry(SupernaturalRace.WEREWOLF, List.of("S-WOL-1@1", "S-WOL-2@1", "S-WOL-3@2", "S-WOL-4@3", "S-WOL-5@4", "S-WOL-6@4>S-WOL-5",
                    "S-WOL-7@5>S-WOL-1")));

    // race -> SP total of its tree, as the spec states it
    private static final Map<SupernaturalRace, Integer> SP = Map.ofEntries(
            Map.entry(SupernaturalRace.HUMAN, 5), Map.entry(SupernaturalRace.VAMPIRE, 19), Map.entry(SupernaturalRace.ORIGINAL, 21),
            Map.entry(SupernaturalRace.ORIGINAL_HYBRID, 21), Map.entry(SupernaturalRace.HYBRID, 19), Map.entry(SupernaturalRace.TRIBRID, 20),
            Map.entry(SupernaturalRace.WITCH, 20), Map.entry(SupernaturalRace.SIPHONER, 17), Map.entry(SupernaturalRace.HERETIC, 19),
            Map.entry(SupernaturalRace.HUNTER, 18), Map.entry(SupernaturalRace.WEREWOLF, 17));

    private static Path dir() throws IOException {
        try {
            // the processed resources are a directory on the test classpath, so a file nobody listed in this test is still found
            return Path.of(SkillDataTest.class.getResource("/data/supernaturalbloodlines/supernatural/ladder/human.json").toURI()).getParent().getParent();
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static Map<String, JsonElement> read(String dir) {
        Map<String, JsonElement> out = new HashMap<>();
        try (Stream<Path> files = Files.list(dir().resolve(dir))) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString();
                out.put(name.substring(0, name.length() - ".json".length()), JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    static SkillTable load(java.util.function.Predicate<String> modLoaded) {
        return load(modLoaded, part -> true);
    }

    static SkillTable load(java.util.function.Predicate<String> modLoaded, java.util.function.Predicate<String> partEnabled) {
        SkillTable.Parsed parsed = SkillTable.parse(read("skill"), read("tree"), read("quest"), modLoaded, partEnabled);
        assertEquals(List.of(), parsed.rejected());
        return parsed.table();
    }

    @Test
    void theCatalogueIsTheSpecs() {
        SkillTable t = load(mod -> true);
        assertEquals(41, t.skills().size());
        for (Map.Entry<String, Integer> e : COSTS.entrySet()) {
            SkillSpec s = t.skill(e.getKey()).orElseThrow(() -> new AssertionError("missing " + e.getKey()));
            assertEquals(e.getValue(), s.cost(), e.getKey());
            assertFalse(s.grantedOnly(), e.getKey());
            assertEquals(TENSURA.contains(e.getKey()), s.kinds().contains(SkillSpec.Kind.TENSURA), e.getKey());
            assertTrue(!s.kinds().contains(SkillSpec.Kind.ATTR) || s.attr() != null, e.getKey());
        }
        assertEquals(40, COSTS.size());
        SkillSpec primeval = t.skill("S-WOL-8").orElseThrow();
        assertTrue(primeval.grantedOnly());
        assertEquals(0, primeval.cost());
    }

    @Test
    void everyTreeIsTheSpecsTableAndItsSpTotalMatches() {
        SkillTable t = load(mod -> true);
        for (SupernaturalRace race : SupernaturalRace.values()) {
            if (race == SupernaturalRace.GHOUL) {
                assertTrue(t.tree(race).entries().isEmpty(), "a Ghoul has no tree");
                continue;
            }
            List<String> rows = new ArrayList<>();
            int sp = 0;
            for (SkillTree.Entry e : t.tree(race).entries()) {
                rows.add(e.skill() + "@" + e.rung() + (e.after() == null ? "" : ">" + e.after()) + (e.quest() == null ? "" : "+Q"));
                assertTrue(e.quest() == null || e.quest().equals("Q-VAM-6"), e.toString());
                sp += t.skill(e.skill()).orElseThrow().cost();
            }
            assertEquals(TREES.get(race), rows, race.id());
            assertEquals(SP.get(race), sp, race.id() + " SP");
        }
    }

    @Test
    void everyIdIsListedAndEveryRungExistsOnItsLadder() {
        SkillTable t = load(mod -> true);
        LadderTable ladders = ShippedLadders.all();
        Set<String> listed = new HashSet<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            int top = ladders.get(race).rungs().size() - 1;
            for (SkillTree.Entry e : t.tree(race).entries()) {
                listed.add(e.skill());
                assertTrue(e.rung() <= top, race.id() + " lists " + e.skill() + " at rung " + e.rung() + " but its ladder ends at " + top);
            }
        }
        assertEquals(COSTS.keySet(), listed, "a catalogue skill no tree lists, or a tree skill outside the catalogue");
    }

    @Test
    void theQuestIsTheSpecsAndEveryTreeQuestExists() {
        SkillTable t = load(mod -> true);
        QuestSpec q = t.quest("Q-VAM-6").orElseThrow();
        assertEquals("Hunter of Hunters", q.name());
        assertEquals(List.of("Q-VAM-6a:kill:10", "Q-VAM-6b:signal:3", "Q-VAM-6c:feed:25"),
                q.tasks().stream().map(x -> x.id() + ":" + x.detector().id() + ":" + x.target()).toList());
        assertEquals("vampire", q.tasks().get(0).args().get("category"));
        assertEquals("vampire_bite_refused", q.tasks().get(1).args().get("signal"));
        assertEquals(1, t.quests().size());
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (SkillTree.Entry e : t.tree(race).entries()) {
                assertTrue(e.quest() == null || t.quest(e.quest()).isPresent(), race.id() + " lists a quest that does not exist: " + e.quest());
            }
        }
        // the quest opens at the rung the tree names for S-VAM-6
        assertEquals(4, t.tree(SupernaturalRace.VAMPIRE).entry("S-VAM-6").orElseThrow().rung());
        assertEquals(3, t.tree(SupernaturalRace.ORIGINAL).entry("S-VAM-6").orElseThrow().rung());
        // without the species part the bite task goes (its signal is fired there)
        assertEquals(2, load(mod -> true, part -> !part.equals("species")).quest("Q-VAM-6").orElseThrow().tasks().size());
    }

    @Test
    void withoutTensuraOnlyTheTensuraSkillsGo() {
        SkillTable t = load(mod -> !mod.equals("tensura"));
        assertEquals(41 - TENSURA.size(), t.skills().size());
        for (String id : TENSURA) {
            assertFalse(t.skill(id).isPresent(), id);
            for (SupernaturalRace race : SupernaturalRace.values()) {
                assertFalse(t.tree(race).lists(id), id);
            }
        }
        assertEquals(6, t.tree(SupernaturalRace.VAMPIRE).entries().size());
        assertEquals(6, t.tree(SupernaturalRace.WEREWOLF).entries().size());
    }
}
