package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;

/**
 * One step of a ladder. Rewards are for the whole rung; they are split across its tasks when those are claimed.
 *
 * @param mods  mods the rung needs; the ladder ends before the first rung with a mod missing
 */
public record Rung(String id, String name, int sp, long ep, List<String> mods, List<TaskSpec> tasks) {
    public Rung {
        mods = List.copyOf(mods);
        tasks = List.copyOf(tasks);
    }
}
