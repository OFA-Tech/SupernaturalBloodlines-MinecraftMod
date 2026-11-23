package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKind;

/**
 * One chain of the evolution rites (plan 19): who may be evolved into which existing skill, what they must hold, and what the rite takes. Pure: the
 * rite file's factory block decodes to this. {@code epCost} is spent from the target's base Max EP after the learn is confirmed (E15).
 */
public record EvolutionRecipe(String id, Tier tier, String grants, GrantForm grantForm, List<String> consumes, List<Need> needs, String requiresMod,
                              String probe, Set<RiteKind> subjectKinds, double epCost) {
    public record Need(String skill, boolean mastered) {
    }

    public EvolutionRecipe {
        consumes = List.copyOf(consumes);
        needs = List.copyOf(needs);
        subjectKinds = Set.copyOf(subjectKinds);
    }
}
