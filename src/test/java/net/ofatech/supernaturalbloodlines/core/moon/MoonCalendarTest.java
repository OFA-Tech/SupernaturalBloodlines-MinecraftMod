package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** R140, row by row. */
class MoonCalendarTest {
    private static final LunarMood BLOOD = new LunarMood(LunarMood.Kind.BLOOD, false);
    private static final LunarMood BLUE = new LunarMood(LunarMood.Kind.BLUE, false);

    @ParameterizedTest
    @CsvSource({"0,0", "1,7", "2,6", "3,5", "4,4", "5,3", "6,2", "7,1"})
    void daysToTheFullMoonByPhase(int phase, int days) {
        assertEquals(days, MoonCalendar.daysToFull(phase));
    }

    @Test
    void theCalendarRunsFromTodayThroughTheCycle() {
        var days = MoonCalendar.days(6, 4);
        assertEquals(List.of(6, 7, 0, 1), days.stream().map(MoonCalendar.Day::phase).toList());
        assertEquals(List.of(0, 1, 2, 3), days.stream().map(MoonCalendar.Day::offset).toList());
        assertEquals(List.of(false, false, true, false), days.stream().map(MoonCalendar.Day::full).toList());
    }

    @Test
    void sixteenDaysHoldTwoFullMoonsAndAFullMoonTodayIsDayZero() {
        assertEquals(List.of(5, 13), MoonCalendar.fullMoons(3, MoonCalendar.DAYS));
        assertEquals(List.of(0, 8), MoonCalendar.fullMoons(0, MoonCalendar.DAYS));
        assertEquals(List.of(1, 9), MoonCalendar.fullMoons(7, MoonCalendar.DAYS));
    }

    @Test
    void aNegativePhaseWrapsLikeTheAlmanacDoes() {
        assertEquals(List.of(1), MoonCalendar.fullMoons(-1, 3));
    }

    @Test
    void theFullMoonDayIsTonightFromMorningToTheEndOfTheNightWindow() {
        assertEquals(MoonCalendar.Night.FULL_MOON, MoonCalendar.tonight(false, 0, 0, LunarMood.NONE, false));
        assertEquals(MoonCalendar.Night.FULL_MOON, MoonCalendar.tonight(false, 0, MoonRule.NIGHT_END - 1, LunarMood.NONE, false));
    }

    @Test
    void afterTheNightWindowTheFullMoonDayIsOver() {
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, 0, MoonRule.NIGHT_END, LunarMood.NONE, false));
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, 0, 23999, LunarMood.NONE, false));
    }

    @Test
    void theTimeOfDayIsTheDayTimeModulo24000() {
        assertEquals(MoonCalendar.Night.FULL_MOON, MoonCalendar.tonight(false, 0, 24000L * 5 + 100, LunarMood.NONE, false));
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, 0, 24000L * 5 + MoonRule.NIGHT_END, LunarMood.NONE, false));
    }

    @Test
    void theNetherAndTheEndHaveNoNight() {
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(true, 0, 0, LunarMood.NONE, false));
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(true, 3, 0, BLOOD, true));
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "3", "4", "5", "6", "7"})
    void anOrdinaryNightOfAnyOtherPhaseIsNothing(int phase) {
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, phase, 13000, LunarMood.NONE, false));
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, phase, 13000, BLUE, false));
    }

    @Test
    void aBloodMoonOnAnotherPhaseIsABloodMoon() {
        assertEquals(MoonCalendar.Night.BLOOD_MOON, MoonCalendar.tonight(false, 3, 13000, BLOOD, false));
    }

    @Test
    void aBloodMoonOnAFullMoonIsTheFullMoon() {
        assertEquals(MoonCalendar.Night.FULL_MOON, MoonCalendar.tonight(false, 0, 13000, BLOOD, false));
    }

    @Test
    void aBloodMoonIsOverWithTheNight() {
        assertEquals(MoonCalendar.Night.NONE, MoonCalendar.tonight(false, 3, MoonRule.NIGHT_END, BLOOD, false));
    }

    @Test
    void aRunningEclipseBeatsEverything() {
        assertEquals(MoonCalendar.Night.ECLIPSE, MoonCalendar.tonight(false, 0, 13000, BLOOD, true));
        assertEquals(MoonCalendar.Night.ECLIPSE, MoonCalendar.tonight(false, 0, 13000, LunarMood.NONE, true));
    }
}
