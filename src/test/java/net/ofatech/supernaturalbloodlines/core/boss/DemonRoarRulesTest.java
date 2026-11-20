package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DemonRoarRulesTest {
    private static boolean shifts(boolean werewolf, boolean alpha, boolean self, boolean ownPack, boolean sameLevel, double d2) {
        return DemonRoarRules.shifts(werewolf, alpha, self, ownPack, sameLevel, d2);
    }

    @Test
    void anotherWerewolfAlphaWithinThirtyTwoBlocksOfAnotherPackShifts() {
        assertTrue(shifts(true, true, false, false, true, 32 * 32));
        assertFalse(shifts(true, true, false, false, true, 32 * 32 + 0.01), "beyond 32 blocks");
    }

    @Test
    void aBetaTheRoarerTheirOwnPackAnotherLevelAndANonWerewolfDoNot() {
        assertFalse(shifts(true, false, false, false, true, 4), "a Beta");
        assertFalse(shifts(true, true, true, false, true, 0), "the roarer");
        assertFalse(shifts(true, true, false, true, true, 4), "their own pack");
        assertFalse(shifts(true, true, false, false, false, 4), "another dimension");
        assertFalse(shifts(false, true, false, false, true, 4), "not a werewolf");
    }

    @Test
    void theCooldownIsItsOwnAndEndsOnItsTick() {
        assertTrue(DemonRoarRules.ready(-1, 0), "never roared");
        assertTrue(DemonRoarRules.ready(100, 100 + DemonRoarRules.COOLDOWN));
        assertFalse(DemonRoarRules.ready(100, 100 + DemonRoarRules.COOLDOWN - 1));
        assertEquals(120, DemonRoarRules.secondsLeft(0, 0));
        assertEquals(1, DemonRoarRules.secondsLeft(0, DemonRoarRules.COOLDOWN - 1));
        assertEquals(0, DemonRoarRules.secondsLeft(0, DemonRoarRules.COOLDOWN));
    }

    @Test
    void theSkillIsTheOneInTheCatalogue() {
        assertEquals("S-WOL-11", DemonRoarRules.SKILL);
    }
}
