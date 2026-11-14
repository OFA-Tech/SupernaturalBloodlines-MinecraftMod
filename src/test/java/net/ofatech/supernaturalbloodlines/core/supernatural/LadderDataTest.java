package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The shipped JSON against the design: every rung and task id, and the totals of the "Rung counts" table. */
class LadderDataTest {
    // race -> "<rung number>:<task letters>" per rung
    private static final Map<SupernaturalRace, List<String>> SHAPE = Map.ofEntries(
            Map.entry(SupernaturalRace.HUMAN, List.of("0:", "1:ab", "2:ab")),
            Map.entry(SupernaturalRace.GHOUL, List.of("0:")),
            Map.entry(SupernaturalRace.VAMPIRE, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab", "6:ab")),
            Map.entry(SupernaturalRace.ORIGINAL, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.ORIGINAL_HYBRID, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.HYBRID, List.of("0:", "1:ab", "2:ab", "3:abc", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.TRIBRID, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.WITCH, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.SIPHONER, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.HERETIC, List.of("0:", "1:ab", "2:ab", "3:abc", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.HUNTER, List.of("0:", "1:ab", "2:ab", "3:a", "4:ab", "5:ab")),
            Map.entry(SupernaturalRace.WEREWOLF, List.of("0:", "1:ab", "2:ab", "3:ab", "4:ab", "5:a", "6:ab")));

    // race -> {rungs, SP total, EP total}
    private static final Map<SupernaturalRace, long[]> TOTALS = Map.ofEntries(
            Map.entry(SupernaturalRace.HUMAN, new long[] {3, 5, 4_000}),
            Map.entry(SupernaturalRace.GHOUL, new long[] {1, 0, 0}),
            Map.entry(SupernaturalRace.VAMPIRE, new long[] {7, 23, 212_000}),
            Map.entry(SupernaturalRace.ORIGINAL, new long[] {6, 20, 210_000}),
            Map.entry(SupernaturalRace.ORIGINAL_HYBRID, new long[] {6, 21, 220_000}),
            Map.entry(SupernaturalRace.HYBRID, new long[] {6, 18, 175_000}),
            Map.entry(SupernaturalRace.TRIBRID, new long[] {6, 20, 230_000}),
            Map.entry(SupernaturalRace.WITCH, new long[] {6, 19, 175_000}),
            Map.entry(SupernaturalRace.SIPHONER, new long[] {6, 19, 175_000}),
            Map.entry(SupernaturalRace.HERETIC, new long[] {6, 20, 210_000}),
            Map.entry(SupernaturalRace.HUNTER, new long[] {6, 18, 171_000}),
            Map.entry(SupernaturalRace.WEREWOLF, new long[] {7, 23, 266_000}));

    private static String prefix(SupernaturalRace race) {
        return switch (race) {
            case ORIGINAL_HYBRID -> "L-OHY-";
            case WEREWOLF -> "L-WOL-";
            default -> "L-" + race.id().substring(0, 3).toUpperCase() + "-";
        };
    }

    @Test
    void everyRaceHasAFileAndNoFileIsRejected() {
        LadderTable.Parsed parsed = LadderTable.parse(ShippedLadders.files(), mod -> true);
        assertEquals(List.of(), parsed.rejected());
        assertEquals(12, ShippedLadders.files().size());
    }

    @Test
    void everyRungAndTaskOfTheSpecExists() {
        LadderTable t = ShippedLadders.all();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            String p = prefix(race);
            List<String> actual = new ArrayList<>();
            List<Rung> rungs = t.get(race).rungs();
            for (int i = 0; i < rungs.size(); i++) {
                assertEquals(p + i, rungs.get(i).id(), "rung id of " + race);
                StringBuilder letters = new StringBuilder();
                for (TaskSpec task : rungs.get(i).tasks()) {
                    assertTrue(task.id().startsWith(p + i), task.id());
                    letters.append(task.id().substring((p + i).length()));
                }
                actual.add(i + ":" + letters);
            }
            assertEquals(SHAPE.get(race), actual, "rungs and tasks of " + race);
        }
    }

    @Test
    void totalsMatchTheRungCountsTable() {
        LadderTable t = ShippedLadders.all();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            List<Rung> rungs = t.get(race).rungs();
            long[] expected = TOTALS.get(race);
            assertEquals(expected[0], rungs.size(), "rungs of " + race);
            assertEquals(expected[1], rungs.stream().mapToLong(Rung::sp).sum(), "SP of " + race);
            assertEquals(expected[2], rungs.stream().mapToLong(Rung::ep).sum(), "EP of " + race);
        }
    }

    @Test
    void everyTopLevelTaskIsFoundById() {
        LadderTable t = ShippedLadders.all();
        int tasks = 0;
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec task : r.tasks()) {
                    assertEquals(race, t.find(task.id()).orElseThrow().race());
                    tasks++;
                }
            }
        }
        assertEquals(tasks, countTopLevel(ShippedLadders.files()));
    }

    private static int countTopLevel(Map<String, JsonElement> files) {
        int n = 0;
        for (JsonElement f : files.values()) {
            for (JsonElement r : f.getAsJsonObject().getAsJsonArray("rungs")) {
                if (r.getAsJsonObject().has("tasks")) {
                    n += r.getAsJsonObject().getAsJsonArray("tasks").size();
                }
            }
        }
        return n;
    }

    @Test
    void apexRungsNeedTensura() {
        LadderTable all = ShippedLadders.all();
        LadderTable noTensura = ShippedLadders.load(mod -> !mod.equals("tensura"));
        for (SupernaturalRace race : SupernaturalRace.values()) {
            int full = all.get(race).rungs().size();
            boolean hasApex = race != SupernaturalRace.HUMAN && race != SupernaturalRace.GHOUL;
            assertEquals(hasApex ? full - 1 : full, noTensura.get(race).rungs().size(), "ladder of " + race + " without Tensura");
        }
    }

    @Test
    void hunterLadderStopsAtSlayerWithoutTheSpeciesPart() {
        LadderTable t = LadderTable.parse(ShippedLadders.files(), mod -> true, part -> !part.equals("species")).table();
        assertEquals(3, t.get(SupernaturalRace.HUNTER).rungs().size());
    }

    @Test
    void theApexRungsAlsoNeedTheTensuraPart() {
        LadderTable t = LadderTable.parse(ShippedLadders.files(), mod -> true, part -> !part.equals("supernaturalTensura")).table();
        assertEquals(6, t.get(SupernaturalRace.VAMPIRE).rungs().size());
        assertEquals(6, t.get(SupernaturalRace.WEREWOLF).rungs().size());
    }

    @Test
    void aTaskWhoseSignalComesFromAPartThatIsOffIsDropped() {
        LadderTable t = LadderTable.parse(ShippedLadders.files(), mod -> true, part -> !part.equals("species")).table();
        // compelling is the species part's; the vampire's rung 3 is left with its level step and still pays in full
        Rung compeller = t.get(SupernaturalRace.VAMPIRE).rungs().get(3);
        assertEquals(List.of("L-VAM-3a"), compeller.tasks().stream().map(TaskSpec::id).toList());
        assertEquals(4, compeller.sp());
        // the sun ring is the artifacts part's
        LadderTable noArtifacts = LadderTable.parse(ShippedLadders.files(), mod -> true, part -> !part.equals("artifacts")).table();
        assertEquals(List.of("L-VAM-2a"), noArtifacts.get(SupernaturalRace.VAMPIRE).rungs().get(2).tasks().stream().map(TaskSpec::id).toList());
    }

    @Test
    void theInTotalTasksAreTheSiphonAndRiteTotals() {
        LadderTable t = ShippedLadders.all();
        java.util.Set<String> totals = new java.util.TreeSet<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec top : r.tasks()) {
                    top.leaves().stream().filter(TaskSpec::total).forEach(l -> totals.add(l.id()));
                }
            }
        }
        assertEquals(java.util.Set.of("L-SIP-1a", "L-SIP-2b", "L-SIP-4a", "L-HER-1b", "L-HER-3c", "L-WIT-3b", "L-WIT-4b"), totals);
    }

    @Test
    void everyPreyTaskCarriesTheVampirismEnchantedPart() {
        // "prey" is defined by vampirismEnchanted; with that part off these signals never come
        int seen = 0;
        LadderTable t = ShippedLadders.all();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec top : r.tasks()) {
                    for (TaskSpec leaf : top.leaves()) {
                        boolean prey = "prey".equals(leaf.args().get("category")) || "true".equals(leaf.args().get("prey"))
                                || "true".equals(leaf.args().get("vampiric")) || "prey_hit_survived".equals(leaf.args().get("signal"));
                        if (prey) {
                            seen++;
                            assertEquals("vampirismEnchanted", leaf.part(), leaf.id());
                        }
                    }
                }
            }
        }
        assertTrue(seen >= 5, "the test found only " + seen + " prey tasks");
    }

    @Test
    void theHereticsSiphonTaskIsVampireTypeOnly() {
        TaskSpec her2a = ShippedLadders.all().find("L-HER-2a").orElseThrow().task();
        assertEquals("true", her2a.args().get("vampiric"));
        assertTrue(!her2a.args().containsKey("prey"), "a werewolf would count");
    }

    @Test
    void theShippedFilesGiveNoWarnings() {
        assertEquals(List.of(), LadderTable.parse(ShippedLadders.files(), mod -> true).warnings());
    }

    @Test
    void theShippedFilesNeedNoPartEnabledCondition() {
        for (JsonElement f : ShippedLadders.files().values()) {
            assertTrue(!f.getAsJsonObject().has("neoforge:conditions"), "the listener only exists while the part is on");
        }
    }

    @Test
    void withoutWerewolvesTheWitchKeepsHerRitesOnly() {
        LadderTable t = ShippedLadders.load(mod -> !mod.equals("werewolves"));
        Rung emissary = t.get(SupernaturalRace.WITCH).rungs().get(3);
        assertEquals(List.of("L-WIT-3b"), emissary.tasks().stream().map(TaskSpec::id).toList());
    }

    @Test
    void werewolfOnlyTasksVanishWithoutWerewolves() {
        LadderTable t = ShippedLadders.load(mod -> !mod.equals("werewolves"));
        for (SupernaturalRace race : Set.of(SupernaturalRace.WEREWOLF, SupernaturalRace.HYBRID)) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec task : r.tasks()) {
                    for (TaskSpec leaf : task.leaves()) {
                        assertTrue(!"werewolves".equals(leaf.mod()), leaf.id());
                        assertTrue(!"werewolves:werewolf".equals(leaf.args().get("faction")), leaf.id());
                    }
                }
            }
        }
    }

    @Test
    void werewolfAlphaRungWantsAnAlphaOrATrueAlphaAndNothingElse() {
        Rung alpha = ShippedLadders.all().get(SupernaturalRace.WEREWOLF).rungs().get(5);
        assertEquals("Alpha", alpha.name());
        assertEquals(1, alpha.tasks().size());
        TaskSpec a = alpha.tasks().get(0);
        assertEquals(TaskSpec.Detector.STATE, a.detector());
        assertEquals("rank_alpha", a.args().get("predicate"));
        assertEquals(6, alpha.sp());
        assertEquals(100_000, alpha.ep());
    }
}
