package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;

/**
 * One step of a ladder. Rewards are for the whole rung; they are split across its tasks when those are claimed.
 *
 * @param mods   mods the rung needs; the ladder ends before the first rung with a mod missing
 * @param parts  parts the rung needs (config keys); the ladder ends before the first rung with one of them off
 */
public record Rung(String id, String name, int sp, long ep, List<String> mods, List<String> parts, List<TaskSpec> tasks) {
    public Rung {
        mods = List.copyOf(mods);
        parts = List.copyOf(parts);
        tasks = List.copyOf(tasks);
    }
}
