package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What to do with the Vampirism skills our ladder skills stand for (S-VAM-7). A Vampirism skill is enabled only when the player lacks it, and
 * recorded; only a recorded one is ever disabled again. One the player had or bought themselves is never recorded, so it is never taken away. Pure.
 */
public final class VampirismSkillPlan {
    /** @param tracked the new record of the Vampirism skills this mod enabled */
    public record Result(Set<String> enable, Set<String> disable, Set<String> tracked) {
    }

    private VampirismSkillPlan() {
    }

    /**
     * @param wanted  Vampirism skill ids the active ladder skills stand for now
     * @param tracked the ones this mod enabled before
     * @param enabled whether the player has the Vampirism skill now
     */
    public static Result plan(Set<String> wanted, Set<String> tracked, Predicate<String> enabled) {
        Set<String> enable = new HashSet<>();
        Set<String> disable = new HashSet<>();
        Set<String> next = new HashSet<>();
        for (String id : wanted) {
            if (!enabled.test(id)) {
                enable.add(id);
                next.add(id);
            } else if (tracked.contains(id)) {
                next.add(id);
            }
        }
        for (String id : tracked) {
            if (!wanted.contains(id) && enabled.test(id)) {
                disable.add(id);
            }
        }
        return new Result(Set.copyOf(enable), Set.copyOf(disable), Set.copyOf(next));
    }
}
