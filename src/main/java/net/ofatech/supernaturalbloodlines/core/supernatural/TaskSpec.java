package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/**
 * One task of a rung. A leaf has a {@link Detector}; a group ({@code anyOf}) is done when one of its members is.
 * Members are leaves and have ids of their own. {@code target} is in the detector's own unit
 * (ticks for {@link Detector#THIRST_DAYS} and {@link Detector#PACK_DAYS}, whose data is written in days). No game classes.
 *
 * @param args   what the event must carry for this task to count; the value {@code any} matches every event
 * @param mod    the mod the task needs; the task is dropped when it is missing
 * @param part   the part of this mod whose code fires the event; the task is dropped when that part is off
 * @param total  a lifetime count: it counts from the moment the player has the race, not only while the rung is active
 */
public record TaskSpec(String id, @Nullable Detector detector, Map<String, String> args, long target, @Nullable String mod,
                       @Nullable String part, boolean total, List<TaskSpec> anyOf) {

    /** The fixed set of detectors. Counting ones add up the events of an active task; the others compare a value with the target. */
    public enum Detector {
        LEVEL(false, 1, List.of("faction"), List.of()),
        STATE(false, 1, List.of("predicate"), List.of()),
        FEED(true, 1, List.of(), List.of()),
        KILL(true, 1, List.of("category"), List.of()),
        RITE(true, 1, List.of("rite", "role"), List.of("other")),
        SIGNAL(true, 1, List.of("signal"), List.of("prey", "vampiric")),
        ITEM_USE(true, 1, List.of("item"), List.of()),
        SIRED(false, 1, List.of(), List.of()),
        DAYS_AS(false, 1, List.of("race"), List.of()),
        THIRST_DAYS(true, 24_000, List.of("min"), List.of()),
        PACK_DAYS(true, 24_000, List.of(), List.of()),
        TENSURA_TIER(false, 1, List.of(), List.of());

        private final boolean counting;
        private final long unit;
        private final List<String> required;
        private final List<String> optional;

        Detector(boolean counting, long unit, List<String> required, List<String> optional) {
            this.counting = counting;
            this.unit = unit;
            this.required = required;
            this.optional = optional;
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

        /** The args a task of this detector may have besides the required ones. */
        public boolean allows(String arg) {
            return required.contains(arg) || optional.contains(arg);
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

    /** The key of the lifetime counter a {@code total} task reads: one per race and kind of event, shared by tasks that count the same thing. */
    public String totalKey(SupernaturalRace race) {
        List<String> parts = new ArrayList<>();
        new TreeMap<>(args).forEach((k, v) -> parts.add(k + "=" + v));
        return race.id() + "/" + detector.id() + "?" + String.join("&", parts);
    }
}
