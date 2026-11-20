package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElijahRulesTest {
    private static ElijahRules.Facts facts(boolean knows, boolean holds, boolean overworld, boolean ground, boolean spectator) {
        return new ElijahRules.Facts(knows, holds, overworld, ground, spectator);
    }

    @Test
    void heVisitsSomeoneWhoKnowsAnOriginalOrCarriesTheirBlood() {
        assertTrue(ElijahRules.eligible(facts(true, false, true, true, false)));
        assertTrue(ElijahRules.eligible(facts(false, true, true, true, false)));
        assertFalse(ElijahRules.eligible(facts(false, false, true, true, false)), "knows nothing and holds nothing");
    }

    @Test
    void notInAnotherDimensionOffTheGroundOrToASpectator() {
        assertFalse(ElijahRules.eligible(facts(true, true, false, true, false)));
        assertFalse(ElijahRules.eligible(facts(true, true, true, false, false)));
        assertFalse(ElijahRules.eligible(facts(true, true, true, true, true)));
    }

    @Test
    void heFightsOnlyAfterAnOathWasBrokenOrWhenHeIsHit() {
        assertFalse(ElijahRules.fights(false, false));
        assertTrue(ElijahRules.fights(true, false));
        assertTrue(ElijahRules.fights(false, true));
    }
}