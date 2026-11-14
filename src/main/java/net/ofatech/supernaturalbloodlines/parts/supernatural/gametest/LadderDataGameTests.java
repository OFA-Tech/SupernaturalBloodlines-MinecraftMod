package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.Rung;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
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
        boolean tensura = ModList.get().isLoaded("tensura");
        helper.assertTrue(t.get(SupernaturalRace.VAMPIRE).rungs().size() == (tensura ? 7 : 6),
                "the vampire ladder has the wrong number of rungs: " + t.get(SupernaturalRace.VAMPIRE).rungs().size());
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
        helper.assertTrue(highest(t, "vampirism:hunter") == maxOf("vampirism:hunter"), "no L-HUN step asks for the hunter max level");
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
