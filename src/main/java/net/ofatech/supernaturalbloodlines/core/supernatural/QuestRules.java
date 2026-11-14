package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The skill quests, pure: which are open, what an event does to them, and when one is finished. A quest is open while a tree of one of
 * the player's races lists a skill with it, that tree's rung is reached and the quest is not finished. Its tasks count like a rung's,
 * in the same state ({@code done}, {@code counters}); finishing it adds it to {@code quests} and drops its task records. A Ghoul
 * records nothing. No game classes.
 */
public final class QuestRules {
    private QuestRules() {
    }

    public static List<QuestSpec> open(SkillTable table, SupernaturalState s, List<SupernaturalRace> races) {
        Map<String, QuestSpec> out = new LinkedHashMap<>();
        for (SupernaturalRace race : races) {
            for (SkillTree.Entry e : table.tree(race).entries()) {
                if (e.quest() != null && !s.quests().contains(e.quest()) && s.rung(race) >= e.rung()) {
                    table.quest(e.quest()).ifPresent(q -> out.put(q.id(), q));
                }
            }
        }
        return new ArrayList<>(out.values());
    }

    /** One event: every open quest's matching task counts it. Finishes the quests it completes. */
    public static SupernaturalState apply(SkillTable table, SupernaturalState s, List<SupernaturalRace> races, TaskEvent e) {
        if (races.contains(SupernaturalRace.GHOUL)) {
            return s;
        }
        SupernaturalState out = s;
        for (QuestSpec q : open(table, s, races)) {
            for (TaskSpec task : q.tasks()) {
                if (out.done().contains(task.id()) || !task.matches(e)) {
                    continue;
                }
                if (task.detector().counting()) {
                    long total = out.counters().getOrDefault(task.id(), 0L) + Math.max(0, e.value());
                    if (total >= task.target()) {
                        out = LadderRules.markDone(out, task.id());
                    } else if (e.value() > 0) {
                        Map<String, Long> counters = new HashMap<>(out.counters());
                        counters.put(task.id(), total);
                        out = out.toBuilder().counters(counters).build();
                    }
                } else if (e.value() >= task.target()) {
                    out = LadderRules.markDone(out, task.id());
                }
            }
        }
        return settle(table, out, races);
    }

    /** Finishes every open quest whose tasks are all done (also one with no tasks left because its mod or part is missing). */
    public static SupernaturalState settle(SkillTable table, SupernaturalState s, List<SupernaturalRace> races) {
        if (races.contains(SupernaturalRace.GHOUL)) {
            return s;
        }
        SupernaturalState out = s;
        for (QuestSpec q : open(table, s, races)) {
            SupernaturalState now = out;
            if (q.tasks().stream().allMatch(t -> now.done().contains(t.id()))) {
                Set<String> quests = new HashSet<>(out.quests());
                quests.add(q.id());
                Set<String> done = new HashSet<>(out.done());
                Map<String, Long> counters = new HashMap<>(out.counters());
                q.tasks().forEach(t -> {
                    done.remove(t.id());
                    counters.remove(t.id());
                });
                out = out.toBuilder().quests(quests).done(done).counters(counters).build();
            }
        }
        return out;
    }
}