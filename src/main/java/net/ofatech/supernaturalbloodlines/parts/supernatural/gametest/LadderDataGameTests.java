package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.Rung;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;

/** The ladders as the server loaded them from the datapacks. */
@PrefixGameTestTemplate(false)
public final class LadderDataGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private LadderDataGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theServerLoadedEveryLadder(GameTestHelper helper) {
        LadderTable t = LadderTable.current();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            helper.assertTrue(!t.get(race).rungs().isEmpty(), "no ladder loaded for " + race);
        }
        boolean tensura = ModList.get().isLoaded("tensura") && EnabledParts.isEnabled("supernaturalTensura");
        helper.assertTrue(t.get(SupernaturalRace.VAMPIRE).rungs().size() == (tensura ? 7 : 6),
                "the vampire ladder has the wrong number of rungs: " + t.get(SupernaturalRace.VAMPIRE).rungs().size());
        helper.succeed();
    }

    /**
     * Compelling is fired by the species part. With it off (run with {@code -PpartsOff=species}) L-VAM-3b is dropped, so a
     * vampire can still climb: the level step alone completes and pays the whole rung.
     */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireClimbsWhateverPartsAreOff(GameTestHelper helper) {
        boolean species = EnabledParts.isEnabled("species");
        LadderTable table = LadderTable.current();
        Rung rung = table.get(SupernaturalRace.VAMPIRE).rungs().get(3);
        helper.assertTrue(rung.tasks().size() == (species ? 2 : 1), "L-VAM-3 has " + rung.tasks().size() + " tasks with species " + species);
        List<SupernaturalRace> races = List.of(SupernaturalRace.VAMPIRE);
        SupernaturalState s = SupernaturalState.NONE.toBuilder().rung(SupernaturalRace.VAMPIRE, 2).build();
        s = LadderRules.apply(table, s, races, TaskEvent.of(TaskSpec.Detector.LEVEL, 10, "faction", "vampirism:vampire"));
        if (species) {
            s = LadderRules.apply(table, s, races, TaskEvent.of(TaskSpec.Detector.SIGNAL, 3, "signal", "compel"));
        }
        LadderRules.ClaimResult first = LadderRules.claim(table, s, SupernaturalRace.VAMPIRE, Optional.empty(), "L-VAM-3a");
        helper.assertTrue(first.ok(), "claim refused: " + first.refusal());
        SupernaturalState end = first.state();
        if (species) {
            end = LadderRules.claim(table, end, SupernaturalRace.VAMPIRE, Optional.empty(), "L-VAM-3b").state();
        }
        helper.assertTrue(end.rung(SupernaturalRace.VAMPIRE) == 3 && end.sp() == 4,
                "the rung did not pay in full: rung " + end.rung(SupernaturalRace.VAMPIRE) + ", " + end.sp() + " SP");
        helper.succeed();
    }

    /** The data writes "max level" as a number; this is what it is checked against. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void maxLevelStepsMatchTheFactions(GameTestHelper helper) {
        LadderTable t = LadderTable.current();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec task : r.tasks()) {
                    for (TaskSpec leaf : task.leaves()) {
                        if (leaf.detector() == TaskSpec.Detector.LEVEL) {
                            int max = maxOf(leaf.args().get("faction"));
                            helper.assertTrue(max < 0 || leaf.target() <= max, leaf.id() + " asks for level " + leaf.target() + ", the faction stops at " + max);
                        }
                    }
                }
            }
        }
        helper.assertTrue(highest(t, "vampirism:vampire") == maxOf("vampirism:vampire"), "no L-VAM step asks for the vampire max level");
        // the hunter ladder only reaches its max-level step (L-HUN-5b) with the species part and Tensura on
        if (t.get(SupernaturalRace.HUNTER).rungs().size() == 6) {
            helper.assertTrue(highest(t, "vampirism:hunter") == maxOf("vampirism:hunter"), "no L-HUN step asks for the hunter max level");
        }
        if (ModList.get().isLoaded("werewolves")) {
            helper.assertTrue(highest(t, "werewolves:werewolf") == maxOf("werewolves:werewolf"), "no werewolf step asks for the max level");
        }
        helper.succeed();
    }

    private static int maxOf(String faction) {
        return switch (faction) {
            case "vampirism:vampire" -> VReference.VAMPIRE_FACTION.getHighestReachableLevel();
            case "vampirism:hunter" -> VReference.HUNTER_FACTION.getHighestReachableLevel();
            case "werewolves:werewolf" -> ModList.get().isLoaded("werewolves") ? WerewolfMax.get() : -1;
            default -> -1;
        };
    }

    private static long highest(LadderTable t, String faction) {
        long best = 0;
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : t.get(race).rungs()) {
                for (TaskSpec task : r.tasks()) {
                    for (TaskSpec leaf : task.leaves()) {
                        if (leaf.detector() == TaskSpec.Detector.LEVEL && faction.equals(leaf.args().get("faction"))) {
                            best = Math.max(best, leaf.target());
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Keeps the Werewolves class out of this one's linking when the mod is missing. */
    private static final class WerewolfMax {
        static int get() {
            return de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION.getHighestReachableLevel();
        }
    }
}
