package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillSpec;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTree;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import org.jetbrains.annotations.Nullable;

/**
 * What the Skills page shows of our trees: each skill of the player's race trees (creature, then magic; a skill both list shows once) as
 * owned, buyable, locked with the rules' own reason, or dormant, plus owned skills no current tree lists (dormant). The state comes from
 * the server's own pure rules ({@link SkillRules}), so the page offers a purchase exactly when the server would accept it. Pure.
 */
public final class HubSkills {
    /** COVERED: the player already has what the skill stands for (Vampirism's own skill), so buying it would pay for nothing. */
    public enum State { OWNED, BUYABLE, LOCKED, DORMANT, COVERED }

    /**
     * @param race  the tree that lists it, null for a dormant skill no current tree lists
     * @param entry its place in that tree (rung, skill before it, quest), null when not listed
     * @param why   the rules' refusal for a locked skill
     */
    public record Row(String id, String name, int cost, @Nullable SupernaturalRace race, @Nullable SkillTree.Entry entry, State state,
                      @Nullable SkillRules.Refusal why) {
    }

    private HubSkills() {
    }

    public static List<Row> rows(SkillTable table, SupernaturalState s, List<SupernaturalRace> races) {
        return rows(table, s, races, spec -> false);
    }

    public static List<Row> rows(SkillTable table, SupernaturalState s, List<SupernaturalRace> races, java.util.function.Predicate<SkillSpec> covered) {
        SupernaturalRace creature = races.get(0);
        Optional<SupernaturalRace> magic = races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty();
        Set<String> active = SkillRules.active(table, s, creature, magic);
        Set<String> seen = new HashSet<>();
        List<Row> out = new ArrayList<>();
        for (SupernaturalRace race : races) {
            for (SkillTree.Entry e : table.tree(race).entries()) {
                if (!seen.add(e.skill())) {
                    continue;
                }
                Optional<SkillSpec> spec = table.skill(e.skill());
                if (spec.isEmpty()) {
                    continue;
                }
                State state;
                SkillRules.Refusal why = null;
                if (s.skills().contains(e.skill())) {
                    state = active.contains(e.skill()) ? State.OWNED : State.DORMANT;
                } else {
                    why = SkillRules.buy(table, s, creature, magic, e.skill(), covered).refusal();
                    state = why == null ? State.BUYABLE : why == SkillRules.Refusal.COVERED ? State.COVERED : State.LOCKED;
                }
                out.add(new Row(e.skill(), spec.get().name(), spec.get().cost(), race, e, state, why));
            }
        }
        for (String id : s.skills().stream().sorted().toList()) {
            if (!seen.contains(id)) {
                // a granted-only skill is in no tree and counts while its rule holds: it is owned, not dormant
                table.skill(id).ifPresent(spec -> out.add(new Row(id, spec.name(), spec.cost(), null, null, active.contains(id) ? State.OWNED : State.DORMANT, null)));
            }
        }
        return out;
    }
}
