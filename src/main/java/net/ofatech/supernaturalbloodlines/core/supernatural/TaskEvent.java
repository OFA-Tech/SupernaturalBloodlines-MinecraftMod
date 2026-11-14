package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.HashMap;
import java.util.Map;

/**
 * Something that happened to a player, as seen by the ladders. For a counting detector {@code value} is the amount to add;
 * for the others it is the value now (a level, a day count, 1 for a state that holds) and it counts when it reaches the target.
 * {@code attrs} is matched against the args of the tasks, see {@link TaskSpec#matches}.
 */
public record TaskEvent(TaskSpec.Detector detector, Map<String, String> attrs, long value) {
    public TaskEvent {
        attrs = Map.copyOf(attrs);
    }

    /** {@code kv} is key, value, key, value... */
    public static TaskEvent of(TaskSpec.Detector detector, long value, String... kv) {
        Map<String, String> attrs = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            attrs.put(kv[i], kv[i + 1]);
        }
        return new TaskEvent(detector, attrs, value);
    }
}
