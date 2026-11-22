package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The ladder tables of the design (docs/superpowers/specs/2026-11-14-supernatural-system-design.md, "Ladders"), written out
 * row by row and compared with the shipped JSON: (sp, ep) of every rung, (detector, key arg, n) of every task.
 * Typed in again from the spec, not read from the JSON.
 */
class LadderSpecRowsTest {
    private static final String V = "vampirism:vampire";
    private static final String W = "werewolves:werewolf";
    private static final String H = "vampirism:hunter";
    private static final String S = "supernaturalbloodlines:";

    // R <rung> <sp> <ep>  /  T <task> <detector> <key arg> <n>
    private static final String ROWS = """
            R L-HUM-1 2 1000
            T L-HUM-1a item_use %S%vervain_tea 1
            T L-HUM-1b signal prey_hit_survived 1
            R L-HUM-2 3 3000
            T L-HUM-2a rite %S%rite_of_lineage_reveal/target 1
            T L-HUM-2b kill prey 5
            R L-VAM-1 2 2000
            T L-VAM-1a level %V% 2
            T L-VAM-1b feed - 10
            R L-VAM-2 3 5000
            T L-VAM-2a level %V% 4
            T L-VAM-2b signal sun_ring_bound 1
            R L-VAM-3 4 15000
            T L-VAM-3a level %V% 10
            T L-VAM-3b signal compel 3
            R L-VAM-4 4 30000
            T L-VAM-4a level %V% 12
            T L-VAM-4b kill hunter 10
            R L-VAM-5 5 60000
            T L-VAM-5a level %V% 14
            T L-VAM-5b days_as vampire 30
            R L-VAM-6 5 100000
            T L-VAM-6a tensura_tier - 1
            T L-VAM-6b days_as vampire 60
            R L-AUG-1 2 2000
            T L-AUG-1a level %V% 2
            T L-AUG-1b feed - 10
            R L-AUG-2 3 5000
            T L-AUG-2a level %V% 4
            T L-AUG-2b kill vampire 3
            R L-AUG-3 4 15000
            T L-AUG-3a level %V% 10
            T L-AUG-3b signal compel 3
            R L-AUG-4 4 30000
            T L-AUG-4a level %V% 12
            T L-AUG-4b kill hunter 10
            R L-AUG-5 5 60000
            T L-AUG-5a level %V% 14
            T L-AUG-5b days_as augustine 30
            R L-AUG-6 5 100000
            T L-AUG-6a tensura_tier - 1
            T L-AUG-6b days_as augustine 60
            R L-ORI-1 3 5000
            T L-ORI-1a level %V% 4
            T L-ORI-1b feed - 20
            R L-ORI-2 3 15000
            T L-ORI-2a rite %S%original_blood/target 1
            T L-ORI-2b level %V% 8
            R L-ORI-3 4 30000
            T L-ORI-3a signal dying_survived 1
            T L-ORI-3b signal compel 5
            R L-ORI-4 5 60000
            T L-ORI-4a sired - 3
            T L-ORI-4b level %V% 14
            R L-ORI-5 5 100000
            T L-ORI-5a tensura_tier - 1
            T L-ORI-5b days_as original 60
            R L-OHY-1 3 10000
            T L-OHY-1a.1 rite %S%rite_of_the_moon/target 1
            T L-OHY-1a.2 state moon_unbound 1
            T L-OHY-1b level %W% 4
            R L-OHY-2 4 20000
            T L-OHY-2a level %W% 8
            T L-OHY-2b signal venom_hit 3
            R L-OHY-3 4 30000
            T L-OHY-3a rite %S%hybrid_blood/target 1
            T L-OHY-3b rite %S%original_blood/target 1
            R L-OHY-4 5 60000
            T L-OHY-4a level %W% 14
            T L-OHY-4b sired - 3
            R L-OHY-5 5 100000
            T L-OHY-5a tensura_tier - 1
            T L-OHY-5b days_as original_hybrid 60
            R L-HYB-1 2 5000
            T L-HYB-1a.1 rite %S%rite_of_the_moon/target 1
            T L-HYB-1a.2 state moon_unbound 1
            T L-HYB-1b level %W% 3
            R L-HYB-2 3 10000
            T L-HYB-2a thirst_days 15 3
            T L-HYB-2b level %W% 6
            R L-HYB-3 4 20000
            T L-HYB-3a signal venom_hit 3
            T L-HYB-3b rite %S%hybrid_blood/target 1
            T L-HYB-3c level %W% 8
            R L-HYB-4 4 40000
            T L-HYB-4a pack_days - 7
            T L-HYB-4b level %W% 12
            R L-HYB-5 5 100000
            T L-HYB-5a tensura_tier - 1
            T L-HYB-5b level %W% 14
            R L-TRI-1 3 10000
            T L-TRI-1a rite any/performer 3
            T L-TRI-1b level %W% 4
            R L-TRI-2 3 20000
            T L-TRI-2a signal venom_hit 3
            T L-TRI-2b rite %S%hybrid_blood/target 1
            R L-TRI-3 4 40000
            T L-TRI-3a.1 state emissary 1
            T L-TRI-3a.2 state rank_alpha 1
            T L-TRI-3b level %W% 10
            R L-TRI-4 5 60000
            T L-TRI-4a level %W% 14
            T L-TRI-4b.1 rite %S%rite_of_lineage_awaken/performer 1
            T L-TRI-4b.2 rite %S%rite_of_severance/performer 1
            R L-TRI-5 5 100000
            T L-TRI-5a tensura_tier - 1
            T L-TRI-5b days_as tribrid 60
            R L-WIT-1 2 5000
            T L-WIT-1a rite any/performer 3
            T L-WIT-1b rite %S%rite_of_lineage_reveal/performer 1
            R L-WIT-2 3 10000
            T L-WIT-2a.1 rite %S%ward_of_daylight/performer 1
            T L-WIT-2a.2 rite %S%ward_of_the_moon/performer 1
            T L-WIT-2a.3 rite %S%ward_of_return/performer 1
            T L-WIT-2b signal mountain_ash_placed 16
            R L-WIT-3 4 20000
            T L-WIT-3a state emissary 1
            T L-WIT-3b rite any/performer 10
            R L-WIT-4 5 40000
            T L-WIT-4a.1 rite %S%rite_of_severance/performer 1
            T L-WIT-4a.2 rite %S%rite_of_the_cure/performer 1
            T L-WIT-4b rite any/performer 25
            R L-WIT-5 5 100000
            T L-WIT-5a tensura_tier - 1
            T L-WIT-5b rite %S%ancestral_calling/performer 1
            R L-SIP-1 2 5000
            T L-SIP-1a signal siphon_power 2000
            T L-SIP-1b rite any/performer 2
            R L-SIP-2 3 10000
            T L-SIP-2a signal siphon_entity 5
            T L-SIP-2b signal siphon_power 10000
            R L-SIP-3 4 20000
            T L-SIP-3a state siphoned_power 1000
            T L-SIP-3b rite any/performer 10
            R L-SIP-4 5 40000
            T L-SIP-4a signal siphon_power 50000
            T L-SIP-4b.1 rite %S%rite_of_severance/performer 1
            T L-SIP-4b.2 rite %S%purification/performer 1
            R L-SIP-5 5 100000
            T L-SIP-5a tensura_tier - 1
            T L-SIP-5b days_as siphoner 60
            R L-HER-1 3 5000
            T L-HER-1a level %V% 4
            T L-HER-1b signal siphon_power 2000
            R L-HER-2 3 15000
            T L-HER-2a signal siphon_entity 3
            T L-HER-2b feed - 15
            R L-HER-3 4 30000
            T L-HER-3a level %V% 10
            T L-HER-3b signal compel 3
            T L-HER-3c signal siphon_power 20000
            R L-HER-4 5 60000
            T L-HER-4a level %V% 14
            T L-HER-4b rite any/performer 10
            R L-HER-5 5 100000
            T L-HER-5a tensura_tier - 1
            T L-HER-5b days_as heretic 60
            R L-HUN-1 2 3000
            T L-HUN-1a level %H% 3
            T L-HUN-1b kill prey 10
            R L-HUN-2 3 8000
            T L-HUN-2a level %H% 6
            T L-HUN-2b kill prey 25
            R L-HUN-3 4 20000
            T L-HUN-3a state hunter_of_the_five 1
            R L-HUN-4 4 40000
            T L-HUN-4a state mark_kills 20
            T L-HUN-4b level %H% 10
            R L-HUN-5 5 100000
            T L-HUN-5a tensura_tier - 1
            T L-HUN-5b level %H% 14
            R L-WOL-1 2 3000
            T L-WOL-1a level %W% 3
            T L-WOL-1b signal forced_shift_survived 1
            R L-WOL-2 3 8000
            T L-WOL-2a state rank_beta 1
            T L-WOL-2b level %W% 5
            R L-WOL-3 3 15000
            T L-WOL-3a state moon_control 1
            T L-WOL-3b level %W% 8
            R L-WOL-4 4 40000
            T L-WOL-4a.1 state rank_alpha 1
            T L-WOL-4a.2 state beta_days 15
            T L-WOL-4b state defender_kills 100
            R L-WOL-5 5 100000
            T L-WOL-5a state rank_alpha 1
            R L-WOL-6 5 100000
            T L-WOL-6a tensura_tier - 1
            T L-WOL-6b days_as werewolf 60
            """.replace("%V%", V).replace("%W%", W).replace("%H%", H).replace("%S%", S);

    private static String key(TaskSpec t) {
        String first = t.detector().required().isEmpty() ? "-" : t.args().get(t.detector().required().get(0));
        return t.detector() == TaskSpec.Detector.RITE ? first + "/" + t.args().get("role") : first;
    }

    @Test
    void everyRungAndTaskMatchesTheSpecRow() {
        LadderTable table = ShippedLadders.all();
        Map<String, String> expectedRungs = new LinkedHashMap<>();
        Map<String, String> expectedTasks = new LinkedHashMap<>();
        for (String line : ROWS.strip().split("\n")) {
            String[] p = line.trim().split(" ");
            if (p[0].equals("R")) {
                expectedRungs.put(p[1], p[2] + "/" + p[3]);
            } else {
                expectedTasks.put(p[1], p[2] + " " + p[3] + " " + p[4]);
            }
        }
        Map<String, String> rungs = new LinkedHashMap<>();
        Map<String, String> tasks = new LinkedHashMap<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : table.get(race).rungs()) {
                if (r.sp() != 0 || r.ep() != 0 || !r.tasks().isEmpty()) {
                    rungs.put(r.id(), r.sp() + "/" + r.ep());
                }
                for (TaskSpec top : r.tasks()) {
                    for (TaskSpec leaf : top.leaves()) {
                        tasks.put(leaf.id(), leaf.detector().id() + " " + key(leaf) + " " + leaf.target() / leaf.detector().unit());
                    }
                }
            }
        }
        assertEquals(expectedRungs, rungs, "rewards of the rungs");
        assertEquals(expectedTasks, tasks, "tasks");
    }
}
