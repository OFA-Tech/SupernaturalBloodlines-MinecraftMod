package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RiteTargetsTest {
    private static final String SELF = "supernaturalbloodlines.refusal.not_original";
    private static final String TARGET = "supernaturalbloodlines.refusal.no_original_blood_in_you";

    @Test
    void withoutConsentTheCasterGetsOneAnswerWhateverTheTargetIs() {
        var original = RiteTargets.bloodAnswer(false, false, true, SELF, TARGET);
        var human = RiteTargets.bloodAnswer(false, false, false, SELF, TARGET);
        assertEquals(original, human, "an Original and a human who do not sneak must be answered the same");
        assertEquals(FactionRiteFactory.NO_EFFECT, human.caster());
        assertEquals(RiteTargets.SNEAK_TO_GIVE_BLOOD, human.target());
    }

    @Test
    void withConsentTheRiteGoesAheadForTheRightKindAndTheWrongKindIsToldByItsOwnWords() {
        assertNull(RiteTargets.bloodAnswer(false, true, true, SELF, TARGET));
        var wrong = RiteTargets.bloodAnswer(false, true, false, SELF, TARGET);
        assertEquals(FactionRiteFactory.NO_EFFECT, wrong.caster());
        assertEquals(TARGET, wrong.target());
    }

    @Test
    void aCasterWhoIsTheirOwnTargetNeedsNoConsentAndIsToldPlainly() {
        assertNull(RiteTargets.bloodAnswer(true, false, true, SELF, TARGET));
        var told = RiteTargets.bloodAnswer(true, false, false, SELF, TARGET);
        assertEquals(SELF, told.caster());
        assertNull(told.target());
    }

    @Test
    void aCasterWhoStandsOnTheCircleThemselvesIsToldTheReal() {
        var told = RiteTargets.refusalAbout(true, "supernaturalbloodlines.refusal.no_gene");
        assertEquals("supernaturalbloodlines.refusal.no_gene", told.caster());
        assertNull(told.target());
    }

    @Test
    void aCasterWhoIsNotTheTargetLearnsNothingAboutThemAndTheTargetIsTold() {
        var told = RiteTargets.refusalAbout(false, "supernaturalbloodlines.refusal.no_gene");
        assertEquals(FactionRiteFactory.NO_EFFECT, told.caster());
        assertEquals("supernaturalbloodlines.refusal.no_gene", told.target());
        // the same neutral answer whatever the refusal is, so two refusals cannot be told apart
        assertEquals(told.caster(), RiteTargets.refusalAbout(false, "supernaturalbloodlines.refusal.not_original").caster());
    }
}
