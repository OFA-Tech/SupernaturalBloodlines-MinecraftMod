package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DemonRoarRulesTest {
    private static DemonRoarRules.Candidate rival(double d2) {
        return new DemonRoarRules.Candidate(true, true, false, false, true, d2, false, true);
    }

    private static boolean shifts(DemonRoarRules.Candidate c) {
        return DemonRoarRules.shifts(c, DemonRoarRules.RANGE, true);
    }

    @Test
    void anotherWerewolfAlphaWithinThirtyTwoBlocksOfAnotherPackShifts() {
        assertTrue(shifts(rival(32 * 32)));
        assertFalse(shifts(rival(32 * 32 + 0.01)), "beyond 32 blocks");
    }

    @Test
    void theRangeIsTheSkillsAndNotAConstant() {
        assertTrue(DemonRoarRules.shifts(rival(40 * 40), 40, true));
        assertFalse(DemonRoarRules.shifts(rival(40 * 40 + 0.01), 40, true));
        assertFalse(DemonRoarRules.shifts(rival(20 * 20 + 0.01), 20, true));
    }

    @Test
    void aBetaTheRoarerTheirOwnPackAnotherLevelAndANonWerewolfDoNot() {
        assertFalse(shifts(new DemonRoarRules.Candidate(true, false, false, false, true, 4, false, true)), "a Beta");
        assertFalse(shifts(new DemonRoarRules.Candidate(true, true, true, false, true, 0, false, true)), "the roarer");
        assertFalse(shifts(new DemonRoarRules.Candidate(true, true, false, true, true, 4, false, true)), "their own pack");
        assertFalse(shifts(new DemonRoarRules.Candidate(true, true, false, false, false, 4, false, true)), "another dimension");
        assertFalse(shifts(new DemonRoarRules.Candidate(false, true, false, false, true, 4, false, true)), "not a werewolf");
    }

    @Test
    void itFollowsTheRulesOfHurtingAPlayerPvpOffTheSameTeamAndSpectators() {
        assertFalse(DemonRoarRules.shifts(rival(4), DemonRoarRules.RANGE, false), "where PvP is off");
        assertFalse(shifts(new DemonRoarRules.Candidate(true, true, false, false, true, 4, true, true)), "a spectator");
        assertFalse(shifts(new DemonRoarRules.Candidate(true, true, false, false, true, 4, false, false)), "somebody the roarer may not harm (the same team)");
        assertTrue(DemonRoarRules.shifts(rival(4), DemonRoarRules.RANGE, true), "control");
    }

    @Test
    void theCooldownIsItsOwnAndEndsOnItsTick() {
        int c = DemonRoarRules.COOLDOWN;
        assertTrue(DemonRoarRules.ready(-1, 0, c), "never roared");
        assertTrue(DemonRoarRules.ready(100, 100 + c, c));
        assertFalse(DemonRoarRules.ready(100, 100 + c - 1, c));
        assertFalse(DemonRoarRules.ready(0, 1199, 1200), "the cooldown is the skill's, 1200 here");
        assertTrue(DemonRoarRules.ready(0, 1200, 1200));
        assertEquals(120, DemonRoarRules.secondsLeft(0, 0, c));
        assertEquals(1, DemonRoarRules.secondsLeft(0, c - 1, c));
        assertEquals(0, DemonRoarRules.secondsLeft(0, c, c));
        assertEquals(60, DemonRoarRules.secondsLeft(0, 0, 1200));
    }

    @Test
    void theSkillIsTheOneInTheCatalogueAndItsParamsAreWhatTheRulesFallBackTo() throws Exception {
        assertEquals("S-WOL-11", DemonRoarRules.SKILL);
        try (var in = new InputStreamReader(DemonRoarRulesTest.class.getResourceAsStream("/data/supernaturalbloodlines/supernatural/skill/s-wol-11.json"), StandardCharsets.UTF_8)) {
            JsonObject skill = JsonParser.parseReader(in).getAsJsonObject();
            assertTrue(skill.get("granted_only").getAsBoolean());
            JsonObject params = skill.getAsJsonObject("params");
            assertEquals(DemonRoarRules.RANGE, params.get("range").getAsDouble());
            assertEquals(DemonRoarRules.COOLDOWN, params.get("cooldown_ticks").getAsInt());
        }
    }
}