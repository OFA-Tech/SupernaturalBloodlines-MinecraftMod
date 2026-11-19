package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** R141, row by row. */
class SleepRulesTest {
    @Test
    void theConfigValueIsReadLeniently() {
        assertEquals(Mode.OFF, Mode.parse("off"));
        assertEquals(Mode.WEREWOLVES, Mode.parse("werewolves"));
        assertEquals(Mode.WEREWOLVES, Mode.parse(" Werewolves "));
        assertEquals(Mode.EVERYONE, Mode.parse("everyone"));
        assertEquals(Mode.OFF, Mode.parse("nobody"));
        assertEquals(Mode.OFF, Mode.parse(null));
        assertEquals("werewolves", Mode.WEREWOLVES.id());
    }

    @Test
    void offBlocksNobody() {
        assertFalse(SleepRules.blocked(Mode.OFF, true, false, 0, 13000));
    }

    @Test
    void werewolvesModeBlocksAWerewolfOnTheFullMoonNight() {
        assertTrue(SleepRules.blocked(Mode.WEREWOLVES, true, false, 0, 13000));
    }

    @Test
    void werewolvesModeLeavesAHumanAlone() {
        assertFalse(SleepRules.blocked(Mode.WEREWOLVES, false, false, 0, 13000));
    }

    @Test
    void everyoneModeBlocksAHumanToo() {
        assertTrue(SleepRules.blocked(Mode.EVERYONE, false, false, 0, 13000));
    }

    @ParameterizedTest
    @CsvSource({"11999,false", "12000,true", "13000,true", "23215,true", "23999,true", "0,false", "6000,false"})
    void theNightStartsAtDuskNotAtTheShiftWindow(long timeOfDay, boolean blocked) {
        assertEquals(blocked, SleepRules.blocked(Mode.EVERYONE, true, false, 0, timeOfDay));
        assertEquals(blocked, SleepRules.blocked(Mode.EVERYONE, true, false, 0, 24000L * 7 + timeOfDay), "any day number");
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "3", "4", "5", "6", "7"})
    void anotherPhaseIsNeverBlocked(int phase) {
        assertFalse(SleepRules.blocked(Mode.EVERYONE, true, false, phase, 13000));
    }

    @Test
    void theNetherAndTheEndAreNeverBlocked() {
        assertFalse(SleepRules.blocked(Mode.EVERYONE, true, true, 0, 13000));
    }

    @Test
    void theRuleAppliesToWhoTheModeNames() {
        assertFalse(SleepRules.appliesTo(Mode.OFF, true));
        assertTrue(SleepRules.appliesTo(Mode.WEREWOLVES, true));
        assertFalse(SleepRules.appliesTo(Mode.WEREWOLVES, false));
        assertTrue(SleepRules.appliesTo(Mode.EVERYONE, false));
    }
}
