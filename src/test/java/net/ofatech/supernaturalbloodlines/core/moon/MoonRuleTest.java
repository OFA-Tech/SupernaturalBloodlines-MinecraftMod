package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import org.junit.jupiter.api.Test;

class MoonRuleTest {
    @Test
    void fullMoonOnlyInPhaseZero() {
        assertTrue(MoonRule.isFullMoon(false, 0));
        for (int phase = 1; phase < 8; phase++) {
            assertFalse(MoonRule.isFullMoon(false, phase), "phase " + phase);
        }
    }

    @Test
    void fullMoonNightNeedsTheNightWindowToo() {
        assertTrue(MoonRule.isFullMoonNight(false, 0, 13000));
        assertTrue(MoonRule.isFullMoonNight(false, 0, 192000L + 18000));
        assertFalse(MoonRule.isFullMoonNight(false, 0, 6000), "noon on the full-moon day");
        assertFalse(MoonRule.isFullMoonNight(false, 0, 0), "the morning of the full-moon day");
        assertFalse(MoonRule.isFullMoonNight(false, 0, MoonRule.NIGHT_START));
        assertFalse(MoonRule.isFullMoonNight(false, 0, MoonRule.NIGHT_END));
        assertFalse(MoonRule.isFullMoonNight(false, 3, 13000), "a night with another phase");
        assertFalse(MoonRule.isFullMoonNight(true, 0, 13000), "a fixed-time dimension");
    }

    @Test
    void anyNightIsTheNightWindowInADimensionWithADay() {
        assertTrue(MoonRule.isNight(false, 13000));
        assertTrue(MoonRule.isNight(false, 24000L * 3 + 18000));
        assertFalse(MoonRule.isNight(false, 6000), "noon");
        assertFalse(MoonRule.isNight(false, MoonRule.NIGHT_START));
        assertFalse(MoonRule.isNight(false, MoonRule.NIGHT_END));
        assertFalse(MoonRule.isNight(true, 13000), "a fixed-time dimension has no night");
    }

    @Test
    void aRiteThatDoesNotNeedTheFullMoonStillNeedsANight() {
        assertTrue(MoonRule.nightFor(true, false, 0, 13000, false));
        assertFalse(MoonRule.nightFor(true, false, 3, 13000, false), "the full moon asked for, another phase");
        assertTrue(MoonRule.nightFor(false, false, 3, 13000, false), "no moon asked for: a night of any phase");
        assertFalse(MoonRule.nightFor(false, false, 3, 6000, false), "no moon asked for is not no night asked for");
        assertFalse(MoonRule.nightFor(false, false, 0, 6000, false), "noon on the full-moon day");
        assertFalse(MoonRule.nightFor(false, true, 3, 13000, false));
    }

    @Test
    void anEclipseSatisfiesAFullMoonRite() {
        assertTrue(MoonRule.nightFor(true, false, 3, 18000, true), "an eclipse on a night that is not phase 0");
        assertFalse(MoonRule.nightFor(true, false, 3, 18000, false), "control: no eclipse");
        assertTrue(MoonRule.nightFor(false, false, 3, 18000, true), "a rite of any night is unchanged");
    }

    @Test
    void anEclipseOutsideTheWindowIsNotActive() {
        assertFalse(MoonRule.nightFor(true, false, 0, 6000, true), "noon");
        assertFalse(MoonRule.nightFor(true, false, 3, MoonRule.NIGHT_START, true), "dusk itself");
        assertTrue(MoonRule.nightFor(true, false, 3, MoonRule.NIGHT_START + 1, true));
        assertTrue(MoonRule.nightFor(true, false, 3, MoonRule.NIGHT_END - 1, true));
        assertFalse(MoonRule.nightFor(true, false, 3, MoonRule.NIGHT_END, true), "dawn");
    }

    /** One eclipse rule for every rite (evolution, mastery, the line rites, the Frequency): only at night, only where there is a night. */
    @Test
    void anEclipseCountsOnlyAtNightAndOnlyWhereThereIsANight() {
        assertTrue(MoonRule.eclipseNight(false, 18000, true));
        assertFalse(MoonRule.eclipseNight(false, 6000, true), "an eclipse at noon");
        assertFalse(MoonRule.eclipseNight(true, 18000, true), "an eclipse in the Nether");
        assertFalse(MoonRule.eclipseNight(false, 18000, false), "no eclipse");
        for (String file : java.util.List.of("parts/evolutionrites/rite/EvolutionRiteFactory.java", "parts/evolutionrites/rite/MasteryRiteFactory.java",
                "parts/species/rite/BrokenLineRiteFactory.java", "parts/species/rite/ImprisonedOriginalRiteFactory.java", "parts/bosses/rite/FrequencyRiteFactory.java")) {
            String src = net.ofatech.supernaturalbloodlines.SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/" + file);
            assertTrue(src.contains("MoonRule.eclipseNight("), file + " does not read the one eclipse rule");
            assertFalse(src.contains("LunarEclipse.active("), file + " reads the eclipse without the night");
        }
    }

    @Test
    void aFixedTimeDimensionIsNeverAFullMoon() {
        assertFalse(MoonRule.nightFor(true, true, 0, 18000, true), "the Nether with an eclipse running in the Overworld");
        assertFalse(MoonRule.nightFor(true, true, 0, 18000, false));
    }

    /** night_watch reads the one night window: 12787 is night, 12786 is not, 23215 is (it had its own 13000 to 23000). */
    @Test
    void nightWatchAndTheMoonRitesShareOneWindow() {
        String src = net.ofatech.supernaturalbloodlines.SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/species/rite/NightWatchRiteFactory.java");
        assertTrue(src.contains("MoonRule.isNight("), "night_watch does not read MoonRule.isNight");
        assertFalse(src.matches("(?s).*\\b(13000|23000|13_000|23_000)\\b.*"), "night_watch keeps a night window of its own");
        assertFalse(src.contains("NIGHT_START ="), "night_watch keeps a night window of its own");
        assertTrue(MoonRule.isNight(false, 12787) && !MoonRule.isNight(false, 12786) && MoonRule.isNight(false, 23215) && !MoonRule.isNight(false, 23216));
    }

    @Test
    void fixedTimeDimensionsNeverHaveAFullMoon() {
        assertFalse(MoonRule.isFullMoon(true, 0));
    }
}
