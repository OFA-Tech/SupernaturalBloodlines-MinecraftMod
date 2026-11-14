package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;

/**
 * A skill quest (a file {@code supernatural/quest/<id>.json}): tasks that must all be done, with the same detectors as a ladder task.
 * It opens when a tree that lists a skill with this quest has its rung reached, and finishing it pays nothing: it lets the player buy
 * that skill. No game classes.
 */
public record QuestSpec(String id, String name, List<TaskSpec> tasks) {
    public QuestSpec {
        tasks = List.copyOf(tasks);
    }
}