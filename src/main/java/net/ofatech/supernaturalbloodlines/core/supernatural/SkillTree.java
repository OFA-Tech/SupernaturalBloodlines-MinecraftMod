package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** The skills one race's tree lists, in file order. A skill is one catalogue id; here it gets a rung, a prerequisite and a quest. */
public record SkillTree(SupernaturalRace race, List<Entry> entries) {
    /**
     * @param skill  catalogue id
     * @param rung   the rung of this race's own ladder the skill needs
     * @param after  a skill that must be owned first, or null
     * @param quest  a quest that must be finished first (Q-VAM-6), or null
     */
    public record Entry(String skill, int rung, @Nullable String after, @Nullable String quest) {
    }

    public SkillTree {
        entries = List.copyOf(entries);
    }

    public static SkillTree empty(SupernaturalRace race) {
        return new SkillTree(race, List.of());
    }

    public Optional<Entry> entry(String skillId) {
        return entries.stream().filter(e -> e.skill().equals(skillId)).findFirst();
    }

    public boolean lists(String skillId) {
        return entry(skillId).isPresent();
    }
}
