package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.Set;

/**
 * The part of a species layer a rung has granted so far. {@code skills} are bare skill names ({@code drain}); the fractions are
 * of the layer's own max magicule and aura bonuses (0 to 1). No game classes.
 */
public record LayerSlice(Set<String> skills, double magiculeFraction, double auraFraction) {
    public LayerSlice {
        skills = Set.copyOf(skills);
    }
}
