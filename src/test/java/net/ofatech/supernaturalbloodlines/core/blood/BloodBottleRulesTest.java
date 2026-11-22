package net.ofatech.supernaturalbloodlines.core.blood;

import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.HUMAN;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.OTHER;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.WEREWOLF;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Drinker.WEREWOLF_IN_TRANSITION;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.MARK;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.MARK_AND_NAME_SIRE;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.NAME_SIRE;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.NO_EFFECT;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.REFUSED;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.TRANSITION_BLOOD;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.UNSTABLE;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.decide;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.COMMON_VAMPIRE;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.HERETIC;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.HYBRID;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.ORIGINAL;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.ORIGINAL_HYBRID;
import static net.ofatech.supernaturalbloodlines.core.blood.BloodKind.TRIBRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Every cell of the R210 table. */
class BloodBottleRulesTest {
    @Test
    void aHumanWhoCanBeMarkedIsMarked() {
        assertEquals(MARK_AND_NAME_SIRE, decide(HUMAN, ORIGINAL, true, false));
        assertEquals(MARK_AND_NAME_SIRE, decide(HUMAN, ORIGINAL_HYBRID, true, false));
        for (BloodKind k : new BloodKind[] {COMMON_VAMPIRE, HYBRID, TRIBRID, HERETIC}) {
            assertEquals(MARK, decide(HUMAN, k, true, false), k.name());
        }
    }

    @Test
    void aHumanAlreadyInTheTurningOnlyNamesTheSireOfAnOriginalAndKeepsAnyOtherBottle() {
        assertEquals(NAME_SIRE, decide(HUMAN, ORIGINAL, false, false));
        assertEquals(NAME_SIRE, decide(HUMAN, ORIGINAL_HYBRID, false, false));
        for (BloodKind k : new BloodKind[] {COMMON_VAMPIRE, HYBRID, TRIBRID, HERETIC}) {
            assertEquals(REFUSED, decide(HUMAN, k, false, false), k.name());
        }
    }

    @Test
    void aWerewolfStartsTheTransitionOnlyWithAnAcceptedOriginalAndIsUntouchedByOtherBlood() {
        assertEquals(TRANSITION_BLOOD, decide(WEREWOLF, ORIGINAL_HYBRID, false, true));
        assertEquals(TRANSITION_BLOOD, decide(WEREWOLF, ORIGINAL, false, true));
        assertEquals(REFUSED, decide(WEREWOLF, ORIGINAL, false, false));
        assertEquals(REFUSED, decide(WEREWOLF, ORIGINAL_HYBRID, false, false));
        for (BloodKind k : new BloodKind[] {COMMON_VAMPIRE, HYBRID, TRIBRID, HERETIC}) {
            assertEquals(NO_EFFECT, decide(WEREWOLF, k, false, false), k.name());
        }
    }

    @Test
    void aWerewolfInTransitionDrinksAcceptedOriginalBloodAgainAndGoesUnstableOnOtherBlood() {
        assertEquals(TRANSITION_BLOOD, decide(WEREWOLF_IN_TRANSITION, ORIGINAL_HYBRID, false, true));
        assertEquals(TRANSITION_BLOOD, decide(WEREWOLF_IN_TRANSITION, ORIGINAL, false, true));
        assertEquals(REFUSED, decide(WEREWOLF_IN_TRANSITION, ORIGINAL, false, false));
        for (BloodKind k : new BloodKind[] {COMMON_VAMPIRE, HYBRID, TRIBRID, HERETIC}) {
            assertEquals(UNSTABLE, decide(WEREWOLF_IN_TRANSITION, k, false, false), k.name());
        }
    }

    @Test
    void aVampireOrAHunterKeepsTheBottle() {
        for (BloodKind k : BloodKind.values()) {
            assertEquals(REFUSED, decide(OTHER, k, true, true), k.name());
            assertEquals(REFUSED, decide(OTHER, k, false, false), k.name());
        }
    }
}
