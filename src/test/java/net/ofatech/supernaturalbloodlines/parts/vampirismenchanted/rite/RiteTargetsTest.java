package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RiteTargetsTest {
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
