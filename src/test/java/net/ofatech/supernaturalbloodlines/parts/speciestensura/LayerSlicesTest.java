package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlice;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlices;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import org.junit.jupiter.api.Test;

/** The slices of each race add up to its plan 05 LayerTable entry, and match the Grants column of the design. */
class LayerSlicesTest {
    private static final LayerTable TABLE = LayerTable.parse(LayerTable.DEFAULTS).table();
    private static final double EPS = 1e-9;

    private static Set<String> bare(Iterable<String> ids) {
        Set<String> out = new HashSet<>();
        for (String id : ids) {
            out.add(id.substring(id.indexOf(':') + 1));
        }
        return out;
    }

    @Test
    void theLastSliceOfEveryRaceIsItsWholeLayer() {
        for (Species species : EnumSet.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID, Species.WITCH,
                Species.SIPHONER, Species.HERETIC, Species.HUNTER_OF_THE_FIVE)) {
            SupernaturalRace race = LayerSlices.raceOf(species);
            LayerSpec spec = TABLE.specFor(species, false);
            int layerRung = LayerSlices.layerRung(race);
            LayerSlice whole = LayerSlices.cumulative(race, layerRung).orElseThrow();
            assertEquals(bare(spec.skills()), whole.skills(), "skills of " + species);
            assertEquals(spec.maxMagiculeBonus() > 0 ? 1.0 : 0.0, whole.magiculeFraction(), EPS, "magicule of " + species);
            assertEquals(spec.maxAuraBonus() > 0 ? 1.0 : 0.0, whole.auraFraction(), EPS, "aura of " + species);
            assertTrue(LayerSlices.slice(race, layerRung).isEmpty(), "the layer rung of " + species + " is not the whole layer");
            assertTrue(LayerSlices.slice(race, layerRung + 3).isEmpty());
            assertTrue(LayerSlices.slice(race, layerRung - 1).isPresent(), "no slice below the layer rung of " + species);
        }
    }

    @Test
    void slicesOnlyGrowAndNeverGrantWhatTheLayerLacks() {
        for (Species species : EnumSet.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID, Species.WITCH,
                Species.SIPHONER, Species.HERETIC, Species.HUNTER_OF_THE_FIVE)) {
            SupernaturalRace race = LayerSlices.raceOf(species);
            Set<String> layer = bare(TABLE.specFor(species, false).skills());
            LayerSlice previous = new LayerSlice(Set.of(), 0, 0);
            for (int rung = 0; rung <= LayerSlices.layerRung(race); rung++) {
                LayerSlice now = LayerSlices.cumulative(race, rung).orElseThrow();
                assertTrue(now.skills().containsAll(previous.skills()), species + " lost a skill at rung " + rung);
                assertTrue(layer.containsAll(now.skills()), species + " grants a skill the layer does not have at rung " + rung);
                assertTrue(now.magiculeFraction() + EPS >= previous.magiculeFraction() && now.auraFraction() + EPS >= previous.auraFraction(),
                        species + " shrank at rung " + rung);
                previous = now;
            }
        }
    }

    // species -> absolute magicule (and aura) the design grants at each rung up to the layer rung
    private static final Map<Species, double[][]> MAGICULE_AND_AURA = Map.of(
            Species.ORIGINAL, new double[][] {{0, 0}, {0.05, 0}, {0.10, 0}, {0.20, 0}},
            Species.ORIGINAL_HYBRID, new double[][] {{0, 0}, {0.08, 0}, {0.15, 0}, {0.25, 0}},
            Species.HYBRID, new double[][] {{0, 0}, {0, 0}, {0.05, 0}, {0.10, 0}},
            Species.TRIBRID, new double[][] {{0, 0}, {0.10, 0}, {0.20, 0}, {0.30, 0}},
            Species.WITCH, new double[][] {{0.05, 0}, {0.10, 0}, {0.20, 0.05}, {0.30, 0.10}},
            Species.SIPHONER, new double[][] {{0, 0}, {0.05, 0}, {0.10, 0}},
            Species.HERETIC, new double[][] {{0, 0}, {0.05, 0}, {0.10, 0}, {0.20, 0}},
            Species.HUNTER_OF_THE_FIVE, new double[][] {{0, 0}, {0, 0}, {0, 0}, {0, 0.05}, {0, 0.10}});

    @Test
    void theAbsoluteBonusesMatchTheGrantsColumn() {
        MAGICULE_AND_AURA.forEach((species, rows) -> {
            SupernaturalRace race = LayerSlices.raceOf(species);
            LayerSpec spec = TABLE.specFor(species, false);
            for (int rung = 0; rung < rows.length; rung++) {
                LayerSlice s = LayerSlices.cumulative(race, rung).orElseThrow();
                assertEquals(rows[rung][0], spec.maxMagiculeBonus() * s.magiculeFraction(), EPS, species + " magicule at rung " + rung);
                assertEquals(rows[rung][1], spec.maxAuraBonus() * s.auraFraction(), EPS, species + " aura at rung " + rung);
            }
        });
    }

    @Test
    void theOriginalGetsItsSkillsInTheDesignOrder() {
        assertEquals(Set.of("drain"), LayerSlices.cumulative(SupernaturalRace.ORIGINAL, 0).orElseThrow().skills());
        assertEquals(Set.of("drain", "self_regeneration"), LayerSlices.slice(SupernaturalRace.ORIGINAL, 1).orElseThrow().skills());
        assertEquals(Set.of("drain", "self_regeneration", "charm"), LayerSlices.slice(SupernaturalRace.ORIGINAL, 2).orElseThrow().skills());
    }

    @Test
    void speciesWithNoSlicedLayerKeepTheWholeLayer() {
        assertEquals(Optional.empty(), Optional.ofNullable(LayerSlices.raceOf(Species.NONE)));
        assertEquals(Optional.empty(), Optional.ofNullable(LayerSlices.raceOf(Species.POTENTIAL)));
        assertEquals(-1, LayerSlices.layerRung(SupernaturalRace.VAMPIRE));
        assertTrue(LayerSlices.slice(SupernaturalRace.VAMPIRE, 0).isEmpty());
    }
}
