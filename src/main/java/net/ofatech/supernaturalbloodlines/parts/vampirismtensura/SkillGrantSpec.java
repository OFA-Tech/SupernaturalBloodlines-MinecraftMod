package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import java.util.ArrayList;
import java.util.List;

/** One "level=skillId" config entry: the skill is held from that faction level up. Level -1 means the maximum level. No game classes. */
public record SkillGrantSpec(int level, String skill) {
    public static final List<String> DEFAULTS = List.of("-1=tensura:godwolf_sense");

    /** The usable grants plus the config entries that could not be used. */
    public record Parsed(List<SkillGrantSpec> grants, List<String> rejected) {}

    /** playerLevel 0 means not in the faction. A level above the maximum counts as the maximum. */
    public boolean reachedAt(int playerLevel, int maxLevel) {
        int needed = level == -1 ? maxLevel : Math.min(level, maxLevel);
        return playerLevel > 0 && playerLevel >= needed;
    }

    public static Parsed parse(List<String> entries) {
        List<SkillGrantSpec> grants = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (String e : entries) {
            int eq = e.indexOf('=');
            String skill = eq > 0 ? e.substring(eq + 1).trim() : "";
            try {
                int level = eq > 0 ? Integer.parseInt(e.substring(0, eq).trim()) : 0;
                if ((level == -1 || level > 0) && skill.indexOf(':') > 0 && !skill.endsWith(":")) {
                    grants.add(new SkillGrantSpec(level, skill));
                    continue;
                }
            } catch (NumberFormatException ignored) {
                // falls through to rejected
            }
            rejected.add(e);
        }
        return new Parsed(List.copyOf(grants), List.copyOf(rejected));
    }
}
