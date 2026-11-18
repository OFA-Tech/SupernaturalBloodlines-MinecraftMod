package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import org.junit.jupiter.api.Test;

class HubAlmanacTest {
    @Test
    void theFullMoonIsEightDaysApart() {
        assertEquals(0, HubAlmanac.daysToFull(0));
        assertEquals(7, HubAlmanac.daysToFull(1));
        assertEquals(4, HubAlmanac.daysToFull(4));
        assertEquals(1, HubAlmanac.daysToFull(7));
    }

    @Test
    void thePhasesRunOnAndWrap() {
        assertEquals(List.of(6, 7, 0, 1, 2, 3), HubAlmanac.phases(6, 6));
        assertEquals(List.of(0), HubAlmanac.phases(8, 1));
        assertEquals(List.of(7), HubAlmanac.phases(-1, 1));
    }

    private static LunarSource.Upcoming event(int days, String path) {
        return new LunarSource.Upcoming(days, LunarMood.ofEvent(LunarMood.EC_DEFAULT_NAMESPACE, path), 0xFF5555, "lunar.event." + path);
    }

    @Test
    void anEventSitsOnItsDayAndTheNextOneIsTheNearest() {
        List<LunarSource.Upcoming> events = List.of(event(2, "blood_moon"), event(4, "super_moon"), event(30, "blue_moon"));
        assertEquals(2, HubAlmanac.eventOn(events, 2).orElseThrow().days());
        assertTrue(HubAlmanac.eventOn(events, 4).orElseThrow().mood().superMoon());
        assertTrue(HubAlmanac.eventOn(events, 3).isEmpty());
        assertEquals(2, HubAlmanac.next(events).orElseThrow().days());
        assertEquals(0, HubAlmanac.next(List.of(event(0, "blue_moon"), event(5, "blood_moon"))).orElseThrow().days(), "tonight counts");
    }

    @Test
    void everyEventOfTheRowIsListedNotOnlyTheNearest() {
        List<LunarSource.Upcoming> events = List.of(event(1, "blue_moon"), event(3, "blood_moon"), event(5, "super_moon"), event(30, "blue_moon"));
        assertEquals(List.of(1, 3, 5), HubAlmanac.listed(events).stream().map(LunarSource.Upcoming::days).toList());
        assertEquals(List.of(30), HubAlmanac.listed(List.of(event(30, "blood_moon"))).stream().map(LunarSource.Upcoming::days).toList(), "none in the row: the nearest beyond it");
        assertTrue(HubAlmanac.listed(List.of()).isEmpty());
    }

    @Test
    void ringsNeverTouchTheirNeighbours() {
        assertEquals(14, HubAlmanac.step(14, 12, false), "no rings: the step is as it was");
        int step = HubAlmanac.step(14, 12, true);
        assertTrue(step - 12 >= 2 * HubAlmanac.RING_REACH, "two rings overlap at step " + step);
        int small = HubAlmanac.step(10, 8, true);
        assertTrue(small - 8 >= 2 * HubAlmanac.RING_REACH, "two small rings overlap at step " + small);
    }

    @Test
    void noEventsMeansNothingToDraw() {
        assertTrue(HubAlmanac.next(List.of()).isEmpty());
        assertTrue(HubAlmanac.eventOn(List.of(), 0).isEmpty());
    }
}
