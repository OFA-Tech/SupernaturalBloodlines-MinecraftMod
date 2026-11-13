package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The Tensura skills of a werewolf rank, from the config list {@code rank_skills}. An entry is {@code KEY=skill,skill};
 * KEY is ALPHA, TRUE_ALPHA or BORN_LEVEL5 (a born werewolf of level 5). A skill without a namespace is a Tensura one.
 * No game classes.
 */
public final class RankSkillTable {
    public static final String ALPHA = "ALPHA";
    public static final String TRUE_ALPHA = "TRUE_ALPHA";
    public static final String BORN_LEVEL5 = "BORN_LEVEL5";

    public static final List<String> DEFAULTS = List.of(
            ALPHA + "=tensura:beast_transformation,tensura:godwolf_sense",
            TRUE_ALPHA + "=tensura:beast_transformation,tensura:godwolf_sense,tensura:commander",
            BORN_LEVEL5 + "=tensura:godwolf_sense");

    private static final Set<String> KEYS = Set.of(ALPHA, TRUE_ALPHA, BORN_LEVEL5);
    private static final Pattern SKILL_ID = Pattern.compile("[a-z0-9_.-]+(:[a-z0-9_./-]+)?");

    /** The usable table plus the config entries that could not be used. */
    public record Parsed(RankSkillTable table, List<String> rejected) {}

    private final Map<String, List<String>> skills;

    private RankSkillTable(Map<String, List<String>> skills) {
        this.skills = skills;
    }

    public List<String> skillsFor(String key) {
        return skills.getOrDefault(key, List.of());
    }

    /** The defaults, with every valid config entry replacing the one of the same key. */
    public static Parsed parse(List<String> entries) {
        Map<String, List<String>> out = new HashMap<>(parseAll(DEFAULTS, new ArrayList<>()));
        List<String> rejected = new ArrayList<>();
        out.putAll(parseAll(entries, rejected));
        return new Parsed(new RankSkillTable(Map.copyOf(out)), List.copyOf(rejected));
    }

    private static Map<String, List<String>> parseAll(List<String> entries, List<String> rejected) {
        Map<String, List<String>> out = new HashMap<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            String key = eq > 0 ? entry.substring(0, eq).trim().toUpperCase(Locale.ROOT) : "";
            List<String> list = KEYS.contains(key) ? parseSkills(entry.substring(eq + 1)) : null;
            if (list == null) {
                rejected.add(entry);
            } else {
                out.put(key, list);
            }
        }
        return out;
    }

    private static List<String> parseSkills(String body) {
        List<String> skills = new ArrayList<>();
        for (String raw : body.split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            if (!SKILL_ID.matcher(id).matches()) {
                return null;
            }
            String full = id.contains(":") ? id : "tensura:" + id;
            if (!skills.contains(full)) {
                skills.add(full);
            }
        }
        return skills;
    }
}
