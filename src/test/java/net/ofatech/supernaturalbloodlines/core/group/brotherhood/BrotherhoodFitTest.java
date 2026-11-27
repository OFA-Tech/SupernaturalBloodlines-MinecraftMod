package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodFit.Facts;
import org.junit.jupiter.api.Test;

class BrotherhoodFitTest {
    private static Facts of(boolean hunter, Species s, boolean vampire, boolean werewolf, boolean humanLike) {
        return new Facts(hunter, s, vampire, werewolf, humanLike);
    }

    @Test
    void aHunterFits() {
        assertTrue(BrotherhoodFit.fits(of(true, Species.NONE, false, false, true)));
        assertTrue(BrotherhoodFit.fits(of(true, Species.HUNTER_OF_THE_FIVE, false, false, true)));
    }

    @Test
    void aPlainHumanFits() {
        assertTrue(BrotherhoodFit.fits(of(false, Species.NONE, false, false, true)));
    }

    @Test
    void aVampireWithNoSpeciesDoesNotFit() {
        assertFalse(BrotherhoodFit.fits(of(false, Species.NONE, true, false, true)));
    }

    @Test
    void aWerewolfDoesNotFit() {
        assertFalse(BrotherhoodFit.fits(of(false, Species.NONE, false, true, true)));
    }

    @Test
    void aWitchSiphonerAndHereticDoNotFit() {
        for (Species s : new Species[] {Species.WITCH, Species.SIPHONER, Species.HERETIC}) {
            assertFalse(BrotherhoodFit.fits(of(false, s, false, false, true)), s.name());
        }
    }

    @Test
    void aHybridOriginalAugustineAndTribridDoNotFit() {
        for (Species s : new Species[] {Species.HYBRID, Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.AUGUSTINE, Species.TRIBRID}) {
            assertFalse(BrotherhoodFit.fits(of(false, s, false, false, true)), s.name());
        }
    }

    @Test
    void aRaceThatIsNotHumanLikeDoesNotFit() {
        assertFalse(BrotherhoodFit.fits(of(false, Species.NONE, false, false, false)));
    }

    @Test
    void aWitchInTheHuntersFits() {
        assertTrue(BrotherhoodFit.fits(of(true, Species.WITCH, false, false, true)));
    }

    @Test
    void aPotentialFits() {
        assertTrue(BrotherhoodFit.fits(of(true, Species.POTENTIAL, false, false, true)));
    }
}