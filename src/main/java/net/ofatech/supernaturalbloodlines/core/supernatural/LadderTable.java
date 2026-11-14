package net.ofatech.supernaturalbloodlines.core.supernatural;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The ladders, parsed from datapack JSON (one file per race, named after {@link SupernaturalRace#id()}). A file is
 * either taken whole or rejected with one line; the other files are not affected. No game classes.
 *
 * <pre>{@code
 * { "rungs": [
 *   { "id": "L-VAM-0", "name": "Fledgling", "sp": 0, "ep": 0 },
 *   { "id": "L-VAM-1", "name": "Thirst", "sp": 2, "ep": 2000, "mods": [], "tasks": [
 *     { "id": "L-VAM-1a", "detector": "level", "faction": "vampirism:vampire", "n": 2 },
 *     { "id": "L-VAM-1b", "detector": "feed", "n": 10, "mod": "vampirism" } ] } ] }
 * }</pre>
 * A task is a detector, its args (every other key), a count {@code n} (default 1) and an optional {@code mod}. An
 * {@code any_of} task has a list of such tasks instead of a detector. Rung 0 has no tasks and pays nothing.
 */
public final class LadderTable {
    public record Parsed(LadderTable table, List<String> rejected) {
    }

    /** Where a top-level task sits. */
    public record TaskRef(SupernaturalRace race, int rung, TaskSpec task) {
    }

    public static final LadderTable EMPTY = new LadderTable(Map.of());

    private static final Set<String> RESERVED = Set.of("id", "detector", "n", "mod", "any_of");
    private static volatile LadderTable current = EMPTY;

    private final Map<SupernaturalRace, Ladder> ladders;
    private final Map<String, TaskRef> tasks = new HashMap<>();

    private LadderTable(Map<SupernaturalRace, Ladder> ladders) {
        this.ladders = Map.copyOf(ladders);
        for (Ladder l : ladders.values()) {
            for (int i = 0; i < l.rungs().size(); i++) {
                for (TaskSpec t : l.rungs().get(i).tasks()) {
                    tasks.put(t.id(), new TaskRef(l.race(), i, t));
                }
            }
        }
    }

    /** The table the server is using; replaced on every data reload. */
    public static LadderTable current() {
        return current;
    }

    public static void setCurrent(LadderTable t) {
        current = t;
    }

    /** The ladder of a race; empty (no rungs) when its file was rejected or is missing. */
    public Ladder get(SupernaturalRace race) {
        return ladders.getOrDefault(race, Ladder.empty(race));
    }

    /** A top-level task by id. */
    public Optional<TaskRef> find(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /**
     * @param files      file name (the race id) to its JSON
     * @param modLoaded  tasks of a missing mod are dropped, and a ladder ends before the first rung that needs one
     */
    public static Parsed parse(Map<String, JsonElement> files, Predicate<String> modLoaded) {
        Map<SupernaturalRace, Ladder> out = new EnumMap<>(SupernaturalRace.class);
        List<String> rejected = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                SupernaturalRace race = SupernaturalRace.byId(file.getKey())
                        .orElseThrow(() -> new BadData("unknown race"));
                Set<String> ids = new HashSet<>();
                List<Rung> rungs = readRungs(file.getValue(), ids);
                for (String id : ids) {
                    if (seen.contains(id)) {
                        throw new BadData("duplicate id " + id + " (already used by another ladder)");
                    }
                }
                seen.addAll(ids);
                out.put(race, new Ladder(race, available(rungs, modLoaded)));
            } catch (BadData e) {
                rejected.add(file.getKey() + ": " + e.getMessage());
            }
        }
        return new Parsed(new LadderTable(out), List.copyOf(rejected));
    }

    private static List<Rung> available(List<Rung> rungs, Predicate<String> modLoaded) {
        List<Rung> out = new ArrayList<>();
        for (Rung r : rungs) {
            if (!r.mods().stream().allMatch(modLoaded)) {
                break;
            }
            out.add(new Rung(r.id(), r.name(), r.sp(), r.ep(), r.mods(), prune(r.tasks(), modLoaded)));
        }
        return out;
    }

    private static List<TaskSpec> prune(List<TaskSpec> tasks, Predicate<String> modLoaded) {
        List<TaskSpec> out = new ArrayList<>();
        for (TaskSpec t : tasks) {
            if (t.isGroup()) {
                List<TaskSpec> members = t.anyOf().stream().filter(m -> m.mod() == null || modLoaded.test(m.mod())).toList();
                if (!members.isEmpty()) {
                    out.add(new TaskSpec(t.id(), null, Map.of(), 0, null, members));
                }
            } else if (t.mod() == null || modLoaded.test(t.mod())) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<Rung> readRungs(JsonElement json, Set<String> ids) {
        JsonArray array = array(object(json, "ladder"), "rungs");
        if (array.isEmpty()) {
            throw new BadData("no rungs");
        }
        List<Rung> rungs = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject o = object(array.get(i), "rung " + i);
            String id = unique(string(o, "id"), ids);
            int sp = (int) number(o, "sp", 0, 0, 1_000_000);
            long ep = number(o, "ep", 0, 0, 1_000_000_000L);
            List<String> mods = strings(o, "mods");
            List<TaskSpec> tasks = new ArrayList<>();
            if (o.has("tasks")) {
                for (JsonElement t : array(o, "tasks")) {
                    tasks.add(readTask(object(t, "task of " + id), ids));
                }
            }
            if (i == 0 && (sp != 0 || ep != 0 || !tasks.isEmpty())) {
                throw new BadData("rung 0 (" + id + ") must pay nothing and have no tasks");
            }
            rungs.add(new Rung(id, o.has("name") ? string(o, "name") : id, sp, ep, mods, tasks));
        }
        return rungs;
    }

    private static TaskSpec readTask(JsonObject o, Set<String> ids) {
        String id = unique(string(o, "id"), ids);
        if (!o.has("any_of")) {
            return readLeaf(o, id);
        }
        if (o.has("detector")) {
            throw new BadData("task " + id + " has both any_of and a detector");
        }
        JsonArray members = array(o, "any_of");
        if (members.isEmpty()) {
            throw new BadData("task " + id + " has an empty any_of");
        }
        List<TaskSpec> list = new ArrayList<>();
        for (JsonElement m : members) {
            JsonObject mo = object(m, "member of " + id);
            if (mo.has("any_of")) {
                throw new BadData("task " + id + " nests an any_of");
            }
            list.add(readLeaf(mo, unique(string(mo, "id"), ids)));
        }
        return new TaskSpec(id, null, Map.of(), 0, null, list);
    }

    private static TaskSpec readLeaf(JsonObject o, String id) {
        String detectorId = string(o, "detector");
        TaskSpec.Detector detector = TaskSpec.Detector.byId(detectorId)
                .orElseThrow(() -> new BadData("task " + id + ": unknown detector " + detectorId));
        long n = number(o, "n", 1, 1, 1_000_000_000L);
        Map<String, String> args = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (!RESERVED.contains(e.getKey())) {
                if (!e.getValue().isJsonPrimitive()) {
                    throw new BadData("task " + id + ": arg " + e.getKey() + " is not a plain value");
                }
                args.put(e.getKey(), e.getValue().getAsString());
            }
        }
        for (String required : detector.required()) {
            if (!args.containsKey(required)) {
                throw new BadData("task " + id + ": " + detectorId + " needs " + required);
            }
        }
        if (detector == TaskSpec.Detector.DAYS_AS && SupernaturalRace.byId(args.get("race")).isEmpty()) {
            throw new BadData("task " + id + ": unknown race " + args.get("race"));
        }
        if (detector == TaskSpec.Detector.RITE && !List.of("performer", "target").contains(args.get("role"))) {
            throw new BadData("task " + id + ": role must be performer or target");
        }
        String mod = o.has("mod") ? string(o, "mod") : null;
        return new TaskSpec(id, detector, args, n * detector.unit(), mod, List.of());
    }

    private static String unique(String id, Set<String> ids) {
        if (!ids.add(id)) {
            throw new BadData("duplicate id " + id);
        }
        return id;
    }

    private static JsonObject object(JsonElement e, String what) {
        if (e == null || !e.isJsonObject()) {
            throw new BadData(what + " is not an object");
        }
        return e.getAsJsonObject();
    }

    private static JsonArray array(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            throw new BadData("missing list " + key);
        }
        return o.getAsJsonArray(key);
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString() || e.getAsString().isBlank()) {
            throw new BadData("missing text " + key);
        }
        return e.getAsString();
    }

    private static long number(JsonObject o, String key, long fallback, long min, long max) {
        if (!o.has(key)) {
            return fallback;
        }
        JsonElement e = o.get(key);
        long v;
        try {
            v = e.getAsBigDecimal().longValueExact();
        } catch (RuntimeException ex) {
            throw new BadData(key + " is not a whole number");
        }
        if (v < min || v > max) {
            throw new BadData(key + " must be between " + min + " and " + max);
        }
        return v;
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key)) {
            for (JsonElement e : array(o, key)) {
                if (!e.isJsonPrimitive()) {
                    throw new BadData(key + " must hold text");
                }
                out.add(e.getAsString());
            }
        }
        return out;
    }

    private static final class BadData extends RuntimeException {
        BadData(String message) {
            super(message);
        }
    }
}
