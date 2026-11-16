package net.ofatech.supernaturalbloodlines.core.supernatural;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The skill catalogue and the trees, parsed from datapack JSON. A skill file is {@code supernatural/skill/<id>.json}, a tree file
 * {@code supernatural/tree/<race>.json} (the race id, as the ladders). A file is taken whole or rejected with one line; the others
 * are not affected. No game classes.
 *
 * <pre>{@code
 * skill: { "id": "S-VAM-4", "name": "Hardened", "cost": 3, "kinds": ["attr"],
 *          "attribute": { "id": "minecraft:generic.max_health", "operation": "add_value", "amount": 2, "when": "always" } }
 *        { "id": "S-VAM-3", "name": "Silver Tongue", "cost": 2, "kinds": ["rule"], "params": { "duration_factor": 1.5 } }
 *        { "id": "S-VAM-5", "name": "Blood Mist", "cost": 3, "kinds": ["tensura"], "tensura_skill": "tensura:blood_mist" }
 * tree:  { "skills": [ { "skill": "S-VAM-1", "rung": 1 }, { "skill": "S-VAM-6", "rung": 4, "after": "S-VAM-1", "quest": "Q-VAM-6" } ] }
 * }</pre>
 * A TENSURA skill is hidden without Tensura, and so is every tree entry that comes after it.
 */
public final class SkillTable {
    public record Parsed(SkillTable table, List<String> rejected) {
    }

    /** The accepted files as read, by name: skills, trees (by race id) and quests. A client that gets them can parse its own table. */
    public record Sources(Map<String, JsonElement> skills, Map<String, JsonElement> trees, Map<String, JsonElement> quests) {
        static final Sources NONE = new Sources(Map.of(), Map.of(), Map.of());
    }

    public static final SkillTable EMPTY = new SkillTable(Map.of(), Map.of(), Map.of(), Sources.NONE);

    private static volatile SkillTable current = EMPTY;
    private static volatile SkillTable clientCopy = EMPTY;

    private final Map<String, SkillSpec> skills;
    private final Map<SupernaturalRace, SkillTree> trees;
    private final Map<String, QuestSpec> quests;
    private final Sources sources;

    private SkillTable(Map<String, SkillSpec> skills, Map<SupernaturalRace, SkillTree> trees, Map<String, QuestSpec> quests, Sources sources) {
        this.skills = Map.copyOf(skills);
        this.trees = Map.copyOf(trees);
        this.quests = Map.copyOf(quests);
        this.sources = sources;
    }

    /** The table the server is using; replaced on every data reload. */
    public static SkillTable current() {
        return current;
    }

    public static void setCurrent(SkillTable t) {
        current = t;
    }

    /** The table the server sent to this client (the hub reads it); empty until the first sync. */
    public static SkillTable clientCopy() {
        return clientCopy;
    }

    public static void setClientCopy(SkillTable t) {
        clientCopy = t;
    }

    public Sources sources() {
        return sources;
    }

    public Optional<QuestSpec> quest(String id) {
        return Optional.ofNullable(quests.get(id));
    }

    public Collection<QuestSpec> quests() {
        return quests.values();
    }

    public Optional<SkillSpec> skill(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    public Collection<SkillSpec> skills() {
        return skills.values();
    }

    /** The tree of a race; empty when it has none (a Ghoul) or its file was rejected. */
    public SkillTree tree(SupernaturalRace race) {
        return trees.getOrDefault(race, SkillTree.empty(race));
    }

    /**
     * @param skillFiles file name to its JSON
     * @param treeFiles  race id to its JSON
     * @param modLoaded  a skill of a missing mod is hidden
     */
    public static Parsed parse(Map<String, JsonElement> skillFiles, Map<String, JsonElement> treeFiles, Predicate<String> modLoaded) {
        return parse(skillFiles, treeFiles, Map.of(), modLoaded, part -> true);
    }

    /**
     * @param questFiles file name to its JSON
     * @param partEnabled a quest task whose part is off is dropped (its signal never comes), as in a ladder
     */
    public static Parsed parse(Map<String, JsonElement> skillFiles, Map<String, JsonElement> treeFiles, Map<String, JsonElement> questFiles,
                               Predicate<String> modLoaded, Predicate<String> partEnabled) {
        List<String> rejected = new ArrayList<>();
        Map<String, JsonElement> acceptedSkills = new HashMap<>();
        Map<String, JsonElement> acceptedTrees = new HashMap<>();
        Map<String, JsonElement> acceptedQuests = new HashMap<>();
        Map<String, SkillSpec> all = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> file : new TreeMap<>(skillFiles).entrySet()) {
            try {
                SkillSpec s = readSkill(file.getValue());
                if (all.putIfAbsent(s.id(), s) != null) {
                    throw new BadData("duplicate id " + s.id());
                }
                acceptedSkills.put(file.getKey(), file.getValue());
            } catch (RuntimeException e) {
                rejected.add("skill " + file.getKey() + ": " + describe(e));
            }
        }
        Map<String, SkillSpec> visible = new LinkedHashMap<>();
        all.forEach((id, s) -> {
            if (s.mod() == null || modLoaded.test(s.mod())) {
                visible.put(id, s);
            }
        });
        Map<SupernaturalRace, SkillTree> trees = new EnumMap<>(SupernaturalRace.class);
        for (Map.Entry<String, JsonElement> file : new TreeMap<>(treeFiles).entrySet()) {
            try {
                SupernaturalRace race = SupernaturalRace.byId(file.getKey()).orElseThrow(() -> new BadData("unknown race"));
                trees.put(race, new SkillTree(race, prune(readTree(file.getValue(), all), visible.keySet())));
                acceptedTrees.put(file.getKey(), file.getValue());
            } catch (RuntimeException e) {
                rejected.add("tree " + file.getKey() + ": " + describe(e));
            }
        }
        Map<String, QuestSpec> quests = new LinkedHashMap<>();
        Set<String> taskIds = new HashSet<>();
        for (Map.Entry<String, JsonElement> file : new TreeMap<>(questFiles).entrySet()) {
            try {
                QuestSpec q = readQuest(file.getValue(), taskIds, modLoaded, partEnabled);
                if (quests.putIfAbsent(q.id(), q) != null) {
                    throw new BadData("duplicate id " + q.id());
                }
                acceptedQuests.put(file.getKey(), file.getValue());
            } catch (RuntimeException e) {
                rejected.add("quest " + file.getKey() + ": " + describe(e));
            }
        }
        return new Parsed(new SkillTable(visible, trees, quests, new Sources(acceptedSkills, acceptedTrees, acceptedQuests)), List.copyOf(rejected));
    }

    private static QuestSpec readQuest(JsonElement json, Set<String> taskIds, Predicate<String> modLoaded, Predicate<String> partEnabled) {
        JsonObject o = object(json, "quest");
        String id = string(o, "id");
        List<TaskSpec> tasks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> mine = new HashSet<>();
        for (JsonElement el : array(o, "tasks")) {
            JsonObject t = object(el, "task of " + id);
            if (t.has("any_of") || t.has("total")) {
                throw new BadData(id + ": a quest task is a plain task (no any_of, no total)");
            }
            String taskId = string(t, "id");
            if (!mine.add(taskId) || taskIds.contains(taskId)) {
                throw new BadData("duplicate id " + taskId);
            }
            TaskSpec task = LadderTable.readLeaf(t, taskId, warnings);
            if ((task.mod() == null || modLoaded.test(task.mod())) && (task.part() == null || partEnabled.test(task.part()))) {
                tasks.add(task);
            }
        }
        if (array(o, "tasks").isEmpty()) {
            throw new BadData(id + ": no tasks");
        }
        taskIds.addAll(mine);
        return new QuestSpec(id, o.has("name") ? string(o, "name") : id, tasks);
    }

    private static String describe(RuntimeException e) {
        return e instanceof BadData || e instanceof LadderTable.BadData ? e.getMessage() : "unreadable value (" + e + ")";
    }

    /** Drops what is hidden, and what comes after it (it could never be bought). */
    private static List<SkillTree.Entry> prune(List<SkillTree.Entry> entries, Set<String> visible) {
        List<SkillTree.Entry> out = new ArrayList<>();
        Set<String> kept = new HashSet<>();
        for (SkillTree.Entry e : entries) {
            if (visible.contains(e.skill()) && (e.after() == null || kept.contains(e.after()))) {
                out.add(e);
                kept.add(e.skill());
            }
        }
        return out;
    }

    private static SkillSpec readSkill(JsonElement json) {
        JsonObject o = object(json, "skill");
        String id = string(o, "id");
        boolean granted = o.has("granted_only") && bool(o, "granted_only");
        int cost = (int) number(o, "cost", granted ? 0 : -1, granted ? 0 : 1, 100, "cost");
        if (granted && cost != 0) {
            throw new BadData(id + ": a granted-only skill costs nothing");
        }
        Set<SkillSpec.Kind> kinds = new HashSet<>();
        for (JsonElement k : array(o, "kinds")) {
            try {
                kinds.add(SkillSpec.Kind.byId(k.getAsString()));
            } catch (RuntimeException e) {
                throw new BadData(id + ": unknown kind " + k);
            }
        }
        if (kinds.isEmpty()) {
            throw new BadData(id + ": kinds is empty");
        }
        SkillSpec.Attr attr = null;
        if (kinds.contains(SkillSpec.Kind.ATTR)) {
            JsonObject a = object(o.get("attribute"), id + ": attribute");
            String operation = string(a, "operation");
            if (!SkillSpec.OPERATIONS.contains(operation)) {
                throw new BadData(id + ": unknown operation " + operation);
            }
            SkillSpec.When when;
            try {
                when = a.has("when") ? SkillSpec.When.byId(string(a, "when")) : SkillSpec.When.ALWAYS;
            } catch (IllegalArgumentException e) {
                throw new BadData(id + ": unknown when " + a.get("when"));
            }
            attr = new SkillSpec.Attr(string(a, "id"), operation, finite(a, "amount", id), when);
        }
        String tensura = null;
        if (kinds.contains(SkillSpec.Kind.TENSURA)) {
            tensura = string(o, "tensura_skill");
        }
        String vampirism = null;
        if (kinds.contains(SkillSpec.Kind.VAMPIRISM)) {
            vampirism = string(o, "vampirism_skill");
        }
        Map<String, Double> params = new HashMap<>();
        if (o.has("params")) {
            JsonObject given = object(o.get("params"), id + ": params");
            for (String key : given.keySet()) {
                params.put(key, finite(given, key, id + " param"));
            }
        }
        return new SkillSpec(id, o.has("name") ? string(o, "name") : id, cost, granted, kinds, attr, tensura, vampirism, params);
    }

    private static List<SkillTree.Entry> readTree(JsonElement json, Map<String, SkillSpec> catalogue) {
        List<SkillTree.Entry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement el : array(object(json, "tree"), "skills")) {
            JsonObject o = object(el, "tree entry");
            String id = string(o, "skill");
            SkillSpec spec = catalogue.get(id);
            if (spec == null) {
                throw new BadData("unknown skill " + id);
            }
            if (spec.grantedOnly()) {
                throw new BadData(id + " is granted, not bought: it cannot be in a tree");
            }
            if (!seen.add(id)) {
                throw new BadData("duplicate entry " + id);
            }
            int rung = (int) number(o, "rung", -1, 1, 99, "rung of " + id);
            String after = o.has("after") ? string(o, "after") : null;
            if (after != null && !seen.contains(after) || id.equals(after)) {
                throw new BadData(id + " comes after " + after + ", which must be listed before it");
            }
            out.add(new SkillTree.Entry(id, rung, after, o.has("quest") ? string(o, "quest") : null));
        }
        return out;
    }

    private static double finite(JsonObject o, String key, String what) {
        double v;
        try {
            v = o.get(key).getAsDouble();
        } catch (RuntimeException e) {
            throw new BadData(what + " " + key + " is not a number");
        }
        if (!Double.isFinite(v)) {
            throw new BadData(what + " " + key + " is not a finite number");
        }
        return v;
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

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) {
            throw new BadData(key + " must be true or false");
        }
        return e.getAsBoolean();
    }

    private static long number(JsonObject o, String key, long fallback, long min, long max, String what) {
        if (!o.has(key)) {
            if (fallback < min) {
                throw new BadData("missing " + what);
            }
            return fallback;
        }
        long v;
        try {
            v = o.get(key).getAsBigDecimal().longValueExact();
        } catch (RuntimeException ex) {
            throw new BadData(what + " is not a whole number");
        }
        if (v < min || v > max) {
            throw new BadData(what + " must be between " + min + " and " + max);
        }
        return v;
    }

    private static final class BadData extends RuntimeException {
        BadData(String message) {
            super(message);
        }
    }
}
