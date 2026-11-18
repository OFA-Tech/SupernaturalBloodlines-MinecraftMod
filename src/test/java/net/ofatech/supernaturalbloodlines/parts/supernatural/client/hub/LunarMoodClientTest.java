package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import org.junit.jupiter.api.Test;

class LunarMoodClientTest {
    private static LunarSource.Upcoming event(int days, String path) {
        return new LunarSource.Upcoming(days, LunarMood.ofEvent(LunarMood.EC_DEFAULT_NAMESPACE, path), 0xFF5555, "lunar.event." + path);
    }

    @Test
    void theViewHasRingsForTheRowAndLinesForEveryEventOfIt() {
        var view = LunarMoodClient.build(List.of(event(0, "blue_moon"), event(2, "blood_moon"), event(40, "super_moon")));
        assertEquals(2, view.byDay().size());
        assertTrue(view.onDay(2).isPresent() && view.onDay(3).isEmpty());
        assertEquals(2, view.lines().size(), "a line per event of the row, none for the far one");
    }

    @Test
    void anEventOutsideTheRowGetsNoRingButTheLineSaysWhen() {
        var view = LunarMoodClient.build(List.of(event(31, "blood_moon")));
        assertTrue(view.byDay().isEmpty());
        assertEquals(31, view.lines().get(0).days());
    }

    @Test
    void noEventsMeansAnEmptyView() {
        assertTrue(LunarMoodClient.build(List.of()).isEmpty());
        assertTrue(LunarMoodClient.view(null).isEmpty());
    }
}
