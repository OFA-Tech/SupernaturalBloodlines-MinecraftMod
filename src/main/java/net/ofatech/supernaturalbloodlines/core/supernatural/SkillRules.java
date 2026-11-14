package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/** Pure rules of the skill trees: who can buy what, and which of the skills a player owns are active. No game classes. */
public final class SkillRules {
    public enum Refusal {
        GHOUL_FROZEN, UNKNOWN_SKILL, GRANTED_ONLY, ALREADY_OWNED, NOT_IN_TREE, RUNG, PREREQUISITE, QUEST, SP;

        public String langKey() {
            return "supernaturalbloodlines.supernatural.skill.refuse." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** @param state the state after the purchase, or the unchanged state on a refusal */
    public record BuyResult(SupernaturalState state, @Nullable Refusal refusal) {
        public boolean ok() {
            return refusal == null;
        }
    }

    private SkillRules() {
    }

    private static List<SupernaturalRace> races(SupernaturalRace creature, Optional<SupernaturalRace> magic) {
        List<SupernaturalRace> out = new ArrayList<>();
        out.add(creature);
        magic.ifPresent(out::add);
        return out;
    }

    /**
     * Buys a skill, or says why not. The skill must be listed in the tree of the creature or the magic race; the first tree whose
     * rung, prerequisite and quest are met is enough, so a skill listed in both trees is bought once and owned in both. One pool of SP.
     */
    public static BuyResult buy(SkillTable table, SupernaturalState s, SupernaturalRace creature, Optional<SupernaturalRace> magic, String skillId) {
        if (creature == SupernaturalRace.GHOUL) {
            return refuse(s, Refusal.GHOUL_FROZEN);
        }
        Optional<SkillSpec> spec = table.skill(skillId);
        if (spec.isEmpty()) {
            return refuse(s, Refusal.UNKNOWN_SKILL);
        }
        if (spec.get().grantedOnly()) {
            return refuse(s, Refusal.GRANTED_ONLY);
        }
        if (s.skills().contains(skillId)) {
            return refuse(s, Refusal.ALREADY_OWNED);
        }
        Refusal first = Refusal.NOT_IN_TREE;
        boolean listed = false;
        for (SupernaturalRace race : races(creature, magic)) {
            Optional<SkillTree.Entry> entry = table.tree(race).entry(skillId);
            if (entry.isEmpty()) {
                continue;
            }
            Refusal why = why(s, race, entry.get(), spec.get());
            if (why == null) {
                Set<String> skills = new HashSet<>(s.skills());
                skills.add(skillId);
                return new BuyResult(s.toBuilder().sp(s.sp() - spec.get().cost()).skills(skills).build(), null);
            }
            if (!listed) {
                first = why;
                listed = true;
            }
        }
        return refuse(s, first);
    }

    @Nullable
    private static Refusal why(SupernaturalState s, SupernaturalRace race, SkillTree.Entry entry, SkillSpec spec) {
        if (s.rung(race) < entry.rung()) {
            return Refusal.RUNG;
        }
        if (entry.after() != null && !s.skills().contains(entry.after())) {
            return Refusal.PREREQUISITE;
        }
        if (entry.quest() != null && !s.quests().contains(entry.quest())) {
            return Refusal.QUEST;
        }
        return s.sp() < spec.cost() ? Refusal.SP : null;
    }

    private static BuyResult refuse(SupernaturalState s, Refusal why) {
        return new BuyResult(s, why);
    }

    /**
     * The owned skills that count now: those the tree of the creature or the magic race lists (dormant otherwise), and the granted-only
     * ones. A Ghoul has none.
     */
    public static Set<String> active(SkillTable table, SupernaturalState s, SupernaturalRace creature, Optional<SupernaturalRace> magic) {
        Set<String> out = new HashSet<>();
        if (creature == SupernaturalRace.GHOUL) {
            return out;
        }
        for (String id : s.skills()) {
            Optional<SkillSpec> spec = table.skill(id);
            if (spec.isPresent() && (spec.get().grantedOnly() || races(creature, magic).stream().anyMatch(r -> table.tree(r).lists(id)))) {
                out.add(id);
            }
        }
        return out;
    }

    public static boolean owns(SkillTable table, SupernaturalState s, SupernaturalRace creature, Optional<SupernaturalRace> magic, String skillId) {
        return s.skills().contains(skillId) && active(table, s, creature, magic).contains(skillId);
    }
}
