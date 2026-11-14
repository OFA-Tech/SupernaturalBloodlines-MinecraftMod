package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * One task of a rung. A leaf has a {@link Detector}; a group ({@code anyOf}) is done when one of its members is.
 * Members are leaves and have ids of their own. {@code target} is in the detector's own unit
 * (ticks for {@link Detector#THIRST_DAYS}, whose data is written in days). No game classes.
 *
 * @param args  what the event must carry for this task to count; the value {@code any} matches every event
 * @param mod   the mod the task needs; the task is dropped when it is missing
 */
public record TaskSpec(String id, @Nullable Detector detector, Map<String, String> args, long target, @Nullable String mod,
                       List<TaskSpec> anyOf) {

    /** The fixed set of detectors. Counting ones add up the events of an active task; the others compare a value with the target. */
    public enum Detector {
        LEVEL(false, 1, "faction"),
        STATE(false, 1, "predicate"),
        FEED(true, 1),
        KILL(true, 1, "category"),
        RITE(true, 1, "rite", "role"),
        SIGNAL(true, 1, "signal"),
        ITEM_USE(true, 1, "item"),
        SIRED(false, 1),
        DAYS_AS(false, 1, "race"),
        THIRST_DAYS(true, 24_000, "min"),
        TENSURA_TIER(false, 1);

        private final boolean counting;
        private final long unit;
        private final List<String> required;

        Detector(boolean counting, long unit, String... required) {
            this.counting = counting;
            this.unit = unit;
            this.required = List.of(required);
        }

        public boolean counting() {
            return counting;
        }

        /** Ticks per data unit: the {@code n} of the data is multiplied by it. */
        public long unit() {
            return unit;
        }

        /** The args a task of this detector must have. */
        public List<String> required() {
            return required;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Optional<Detector> byId(String id) {
            for (Detector d : values()) {
                if (d.id().equals(id)) {
                    return Optional.of(d);
                }
            }
            return Optional.empty();
        }
    }

    public TaskSpec {
        args = Map.copyOf(args);
        anyOf = List.copyOf(anyOf);
    }

    public boolean isGroup() {
        return !anyOf.isEmpty();
    }

    /** The leaves that can be recorded: this task, or the members of the group. */
    public List<TaskSpec> leaves() {
        return isGroup() ? anyOf : List.of(this);
    }

    /** True when this leaf is the kind of task the event is about. */
    public boolean matches(TaskEvent e) {
        if (detector != e.detector()) {
            return false;
        }
        for (Map.Entry<String, String> arg : args.entrySet()) {
            if (!arg.getValue().equals("any") && !arg.getValue().equals(e.attrs().get(arg.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
