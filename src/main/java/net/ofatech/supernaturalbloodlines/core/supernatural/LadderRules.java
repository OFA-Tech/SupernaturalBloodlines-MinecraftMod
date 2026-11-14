package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The ladder rules, pure: which rung is active, what an event completes, what a claim pays. Every function returns a
 * new state. A Ghoul (creature race {@link SupernaturalRace#GHOUL}) records and claims nothing. No game classes.
 */
public final class LadderRules {
    /** Why a claim was refused; {@link #langKey()} is the message. */
    public enum Refusal {
        GHOUL_FROZEN, UNKNOWN_TASK, NOT_ACTIVE, NOT_DONE, ALREADY_CLAIMED;

        public String langKey() {
            return "supernaturalbloodlines.supernatural.claim." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * @param race         the ladder that was climbed, null for a refusal
     * @param rungReached  the highest rung reached by this call, -1 for none
     */
    public record ClaimResult(SupernaturalState state, @Nullable SupernaturalRace race, int sp, long ep, int rungReached,
                              @Nullable Refusal refusal) {
        public boolean ok() {
            return refusal == null;
        }
    }

    private LadderRules() {
    }

    /** The rung after the highest reached one; empty at the top of the ladder. */
    public static Optional<Rung> activeRung(LadderTable table, SupernaturalState s, SupernaturalRace race) {
        return table.get(race).rung(s.rung(race) + 1);
    }

    /** The top-level tasks of the active rung of each race (claimed ones included). */
    public static List<TaskSpec> activeTasks(LadderTable table, SupernaturalState s, List<SupernaturalRace> races) {
        List<TaskSpec> out = new ArrayList<>();
        for (SupernaturalRace race : races) {
            activeRung(table, s, race).ifPresent(r -> out.addAll(r.tasks()));
        }
        return out;
    }

    /** A leaf is done when recorded; a group when any member is. */
    public static boolean isDone(TaskSpec task, SupernaturalState s) {
        return task.leaves().stream().anyMatch(l -> s.done().contains(l.id()));
    }

    /** Marks a leaf done and drops its counter. */
    public static SupernaturalState markDone(SupernaturalState s, String leafId) {
        Set<String> done = new HashSet<>(s.done());
        done.add(leafId);
        Map<String, Long> counters = new HashMap<>(s.counters());
        counters.remove(leafId);
        return s.toBuilder().done(done).counters(counters).build();
    }

    /**
     * Adds {@code amount} to the counter of a leaf of the active rung of {@code race}; done at its target.
     * Anything else (a Ghoul, a task that is not active, one already done) changes nothing.
     */
    public static SupernaturalState record(LadderTable table, SupernaturalState s, SupernaturalRace race, String leafId, long amount) {
        TaskSpec leaf = trackable(table, s, race, leafId);
        if (leaf == null || amount <= 0) {
            return s;
        }
        long total = s.counters().getOrDefault(leafId, 0L) + amount;
        if (total >= leaf.target()) {
            return markDone(s, leafId);
        }
        Map<String, Long> counters = new HashMap<>(s.counters());
        counters.put(leafId, total);
        return s.toBuilder().counters(counters).build();
    }

    /** Marks a leaf of the active rung of {@code race} done when {@code value} reaches its target. */
    public static SupernaturalState reach(LadderTable table, SupernaturalState s, SupernaturalRace race, String leafId, long value) {
        TaskSpec leaf = trackable(table, s, race, leafId);
        return leaf != null && value >= leaf.target() ? markDone(s, leafId) : s;
    }

    /**
     * Applies an event to the active rungs of the player's races (creature first, then magic): every leaf the event
     * matches records it. One event can move both ladders.
     */
    public static SupernaturalState apply(LadderTable table, SupernaturalState s, List<SupernaturalRace> races, TaskEvent e) {
        if (races.contains(SupernaturalRace.GHOUL)) {
            return s;
        }
        SupernaturalState out = s;
        for (SupernaturalRace race : races) {
            for (TaskSpec task : activeTasks(table, out, List.of(race))) {
                for (TaskSpec leaf : task.leaves()) {
                    if (leaf.matches(e)) {
                        out = e.detector().counting()
                                ? record(table, out, race, leaf.id(), e.value())
                                : reach(table, out, race, leaf.id(), e.value());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Pays the task's share of its rung and, when that was the last unclaimed task, reaches the rung (and any
     * following rung left with no tasks). The share is the rung total divided evenly, the remainder on the last task.
     */
    public static ClaimResult claim(LadderTable table, SupernaturalState s, SupernaturalRace creature,
                                    Optional<SupernaturalRace> magic, String taskId) {
        if (creature == SupernaturalRace.GHOUL) {
            return refused(s, Refusal.GHOUL_FROZEN);
        }
        Optional<LadderTable.TaskRef> ref = table.find(taskId);
        if (ref.isEmpty()) {
            return refused(s, Refusal.UNKNOWN_TASK);
        }
        if (s.claimed().contains(taskId)) {
            return refused(s, Refusal.ALREADY_CLAIMED);
        }
        SupernaturalRace race = ref.get().race();
        boolean mine = race == creature || magic.filter(m -> m == race).isPresent();
        if (!mine || s.rung(race) + 1 != ref.get().rung()) {
            return refused(s, Refusal.NOT_ACTIVE);
        }
        if (!isDone(ref.get().task(), s)) {
            return refused(s, Refusal.NOT_DONE);
        }
        Rung rung = table.get(race).rungs().get(ref.get().rung());
        int index = rung.tasks().indexOf(ref.get().task());
        int sp = share(rung.sp(), rung.tasks().size(), index);
        long ep = share(rung.ep(), rung.tasks().size(), index);
        Set<String> claimed = new HashSet<>(s.claimed());
        claimed.add(taskId);
        SupernaturalState paid = pay(s.toBuilder().claimed(claimed).build(), sp);
        int reached = -1;
        if (rung.tasks().stream().allMatch(t -> claimed.contains(t.id()))) {
            paid = paid.toBuilder().rung(race, ref.get().rung()).build();
            reached = ref.get().rung();
        }
        ClaimResult chained = settle(table, paid, race);
        return new ClaimResult(chained.state(), race, sp + chained.sp(), ep + chained.ep(),
                Math.max(reached, chained.rungReached()), null);
    }

    /**
     * Reaches every rung after the current one that has no tasks (all dropped with their mod, or none to begin with),
     * paying its whole reward. Call it when a race is first entered and after every rung.
     */
    public static ClaimResult settle(LadderTable table, SupernaturalState s, SupernaturalRace race) {
        SupernaturalState out = s;
        int sp = 0;
        long ep = 0;
        int reached = -1;
        Optional<Rung> next;
        while ((next = activeRung(table, out, race)).isPresent() && next.get().tasks().isEmpty()) {
            Rung r = next.get();
            reached = out.rung(race) + 1;
            out = pay(out.toBuilder().rung(race, reached).build(), r.sp());
            sp += r.sp();
            ep += r.ep();
        }
        return new ClaimResult(out, race, sp, ep, reached, null);
    }

    /** The part of {@code total} for task {@code index} of {@code count}: even, the last one takes the remainder. */
    static int share(int total, int count, int index) {
        return (int) share((long) total, count, index);
    }

    static long share(long total, int count, int index) {
        long each = total / count;
        return index == count - 1 ? total - each * (count - 1) : each;
    }

    private static SupernaturalState pay(SupernaturalState s, int sp) {
        return s.toBuilder().sp(s.sp() + sp).spEarned(s.spEarned() + sp).build();
    }

    private static ClaimResult refused(SupernaturalState s, Refusal why) {
        return new ClaimResult(s, null, 0, 0, -1, why);
    }

    /** The leaf when it may be recorded now: on the active rung, not done, its task not done through another member. */
    @Nullable
    private static TaskSpec trackable(LadderTable table, SupernaturalState s, SupernaturalRace race, String leafId) {
        if (race == SupernaturalRace.GHOUL) {
            return null;
        }
        for (TaskSpec task : activeTasks(table, s, List.of(race))) {
            if (isDone(task, s)) {
                continue;
            }
            for (TaskSpec leaf : task.leaves()) {
                if (leaf.id().equals(leafId)) {
                    return leaf;
                }
            }
        }
        return null;
    }
}
