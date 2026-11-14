package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.jetbrains.annotations.Nullable;

/**
 * How each species layer (plan 05's {@code LayerTable}) is spread over the rungs of its race's ladder: the skills one by one, and
 * the magicule and aura bonus as a cumulative fraction of the layer's own value. The numbers are the "Grants" column of the design
 * (magicule 0.05 of a layer of 0.20 is a fraction of 0.25). Undead treatment and the EP gain are not here: they apply from rung 0.
 * A rung at or above the {@link #layerRung layer rung} is the whole layer, whatever the config says it contains. No game classes.
 */
public final class LayerSlices {
    private record Step(List<String> skills, double magiculeFraction, double auraFraction) {
    }

    private record Plan(int layerRung, List<Step> steps) {
    }

    private static final Map<SupernaturalRace, Plan> PLANS = new EnumMap<>(SupernaturalRace.class);

    private static Step step(double magicule, double aura, String... skills) {
        return new Step(List.of(skills), magicule, aura);
    }

    private static void plan(SupernaturalRace race, int layerRung, Step... steps) {
        PLANS.put(race, new Plan(layerRung, List.of(steps)));
    }

    static {
        // each step is what that rung adds; fractions are cumulative; the step at the layer rung ends at 1.0
        plan(SupernaturalRace.ORIGINAL, 3, step(0, 0, "drain"), step(0.25, 0, "self_regeneration"), step(0.5, 0, "charm"), step(1, 0, "blood_mist"));
        plan(SupernaturalRace.ORIGINAL_HYBRID, 3, step(0, 0, "drain", "self_regeneration"), step(0.32, 0, "beast_transformation"),
                step(0.6, 0, "charm"), step(1, 0, "blood_mist", "godwolf_sense"));
        plan(SupernaturalRace.HYBRID, 3, step(0, 0, "drain"), step(0, 0, "beast_transformation"), step(0.5, 0, "self_regeneration"), step(1, 0));
        plan(SupernaturalRace.TRIBRID, 3, step(0, 0, "drain"), step(1.0 / 3, 0, "beast_transformation"), step(2.0 / 3, 0, "self_regeneration"),
                step(1, 0, "charm"));
        plan(SupernaturalRace.WITCH, 3, step(1.0 / 6, 0), step(1.0 / 3, 0), step(2.0 / 3, 0.5), step(1, 1));
        plan(SupernaturalRace.SIPHONER, 2, step(0, 0, "drain"), step(0.5, 0), step(1, 0));
        plan(SupernaturalRace.HERETIC, 3, step(0, 0, "drain"), step(0.25, 0, "self_regeneration"), step(0.5, 0, "charm"), step(1, 0));
        plan(SupernaturalRace.HUNTER, 4, step(0, 0), step(0, 0), step(0, 0), step(0, 0.5), step(0, 1));
    }

    private LayerSlices() {
    }

    /** The ladder race whose rung decides this species' layer, or null for a species with no sliced layer (it keeps the whole layer). */
    @Nullable
    public static SupernaturalRace raceOf(Species species) {
        return switch (species) {
            case ORIGINAL -> SupernaturalRace.ORIGINAL;
            case ORIGINAL_HYBRID -> SupernaturalRace.ORIGINAL_HYBRID;
            case HYBRID -> SupernaturalRace.HYBRID;
            case TRIBRID -> SupernaturalRace.TRIBRID;
            case WITCH -> SupernaturalRace.WITCH;
            case SIPHONER -> SupernaturalRace.SIPHONER;
            case HERETIC -> SupernaturalRace.HERETIC;
            case HUNTER_OF_THE_FIVE -> SupernaturalRace.HUNTER;
            default -> null;
        };
    }

    /** The rung by which the whole layer is granted; -1 for a race with no sliced layer. */
    public static int layerRung(SupernaturalRace race) {
        Plan p = PLANS.get(race);
        return p == null ? -1 : p.layerRung();
    }

    /** Everything granted up to and including {@code rung} (the last step is used above the end). Empty for a race with no plan. */
    public static Optional<LayerSlice> cumulative(SupernaturalRace race, int rung) {
        Plan p = PLANS.get(race);
        if (p == null) {
            return Optional.empty();
        }
        Set<String> skills = new HashSet<>();
        double magicule = 0;
        double aura = 0;
        for (int i = 0; i <= Math.min(Math.max(rung, 0), p.steps().size() - 1); i++) {
            Step s = p.steps().get(i);
            skills.addAll(s.skills());
            magicule = s.magiculeFraction();
            aura = s.auraFraction();
        }
        return Optional.of(new LayerSlice(skills, magicule, aura));
    }

    /** What the layer grants at {@code rung}; empty when the whole layer applies (no plan, or the layer rung is reached). */
    public static Optional<LayerSlice> slice(SupernaturalRace race, int rung) {
        int layerRung = layerRung(race);
        return layerRung < 0 || rung >= layerRung ? Optional.empty() : cumulative(race, rung);
    }

    /** All skills any step of this race grants. */
    public static List<String> skillsOf(SupernaturalRace race) {
        Plan p = PLANS.get(race);
        List<String> out = new ArrayList<>();
        if (p != null) {
            p.steps().forEach(s -> out.addAll(s.skills()));
        }
        return out;
    }
}
