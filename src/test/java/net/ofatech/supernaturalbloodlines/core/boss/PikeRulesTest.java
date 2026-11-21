package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules.Giver;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules.Kind;
import org.junit.jupiter.api.Test;

class PikeRulesTest {
    private static Giver player(Species s, int vampire, int wolf, boolean alpha) {
        return new Giver(Kind.PLAYER, s, vampire, wolf, alpha);
    }

    @Test
    void aPlainPlayersBloodIsNotEnough() {
        assertFalse(PikeRules.qualifies(player(Species.NONE, 0, 0, false)));
        assertFalse(PikeRules.qualifies(player(Species.NONE, 3, 3, false)), "one rung under advanced");
        assertFalse(PikeRules.qualifies(player(Species.WITCH, 0, 0, false)), "a witch's own blood is not enough either");
        assertFalse(PikeRules.qualifies(player(Species.POTENTIAL, 0, 0, false)));
        assertFalse(PikeRules.qualifies(player(Species.SIPHONER, 0, 0, false)));
        assertFalse(PikeRules.qualifies(player(Species.HERETIC, 1, 1, false)));
    }

    @Test
    void aHunterOfTheFiveAnOriginalAHybridOrATribridQualifiesWhateverTheirRungs() {
        assertTrue(PikeRules.qualifies(player(Species.HUNTER_OF_THE_FIVE, 0, 0, false)));
        assertTrue(PikeRules.qualifies(player(Species.ORIGINAL, 0, 0, false)));
        assertTrue(PikeRules.qualifies(player(Species.ORIGINAL_HYBRID, 0, 0, false)));
        assertTrue(PikeRules.qualifies(player(Species.HYBRID, 0, 0, false)));
        assertTrue(PikeRules.qualifies(player(Species.TRIBRID, 0, 0, false)));
    }

    @Test
    void anAdvancedVampireOrWerewolfOrAnAlphaQualifiesAtTheRungEdge() {
        assertTrue(PikeRules.qualifies(player(Species.NONE, 4, 0, false)), "an advanced vampire");
        assertTrue(PikeRules.qualifies(player(Species.NONE, 0, 4, false)), "an advanced werewolf");
        assertFalse(PikeRules.qualifies(player(Species.NONE, 3, 0, false)));
        assertFalse(PikeRules.qualifies(player(Species.NONE, 0, 3, false)));
        assertTrue(PikeRules.qualifies(player(Species.NONE, 0, 0, true)), "an Alpha or a True Alpha");
        assertTrue(PikeRules.qualifies(player(Species.NONE, 6, 6, true)));
    }

    @Test
    void anNpcOriginalsBloodQualifiesAndAnAbsentGiversDoesNot() {
        assertTrue(PikeRules.qualifies(Giver.of(Kind.NPC_ORIGINAL)));
        assertFalse(PikeRules.qualifies(Giver.of(Kind.ABSENT)));
        assertFalse(PikeRules.qualifies(new Giver(Kind.ABSENT, Species.ORIGINAL, 6, 6, true)), "nor for a giver who is not here");
    }

    @Test
    void onlyThePikeHurtsTheSpiritAndItsBlowEndsTheBeastOnlyForTheSpirit() {
        assertTrue(PikeRules.hurtsSpirit(true, true));
        assertFalse(PikeRules.hurtsSpirit(true, false), "a direct blow of something else");
        assertFalse(PikeRules.hurtsSpirit(false, true), "a snowball or an arrow thrown while holding the pike is not a blow of it");
        assertFalse(PikeRules.hurtsSpirit(false, false));
        assertTrue(PikeRules.endsBeast(BeastPhase.SPIRIT, true));
        assertFalse(PikeRules.endsBeast(BeastPhase.SPIRIT, false));
        for (BeastPhase p : BeastPhase.values()) {
            if (p != BeastPhase.SPIRIT) {
                assertFalse(PikeRules.endsBeast(p, true), p + " ended by a pike");
            }
        }
    }
}
