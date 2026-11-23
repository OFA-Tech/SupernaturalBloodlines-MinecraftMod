package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CooldownClockTest {
    @Test
    void aRunningCooldownKeepsTheSameEndWhateverTheTick() {
        CooldownClock c = new CooldownClock();
        CooldownClock.Reading first = c.observe("a", 1000, 60, 1, 0);
        assertEquals(1060, first.end());
        for (int t = 1; t < 60; t++) {
            assertEquals(first, c.observe("a", 1000 + t, 60 - t, 1, 0), "tick " + t);
        }
        assertEquals(CooldownClock.Reading.READY, c.observe("a", 1060, 0, 1, 0));
    }

    @Test
    void tensurasWholeSecondsDoNotMoveTheEnd() {
        CooldownClock c = new CooldownClock();
        // 3 seconds from tick 1000: the source says 60, 60 ... then 40 once a second went by, and so on
        CooldownClock.Reading first = c.observe("t", 1000, 60, 20, 0);
        assertEquals(first, c.observe("t", 1010, 60, 20, 0));
        assertEquals(first, c.observe("t", 1019, 60, 20, 0));
        assertEquals(first, c.observe("t", 1020, 40, 20, 0));
        assertEquals(first, c.observe("t", 1045, 20, 20, 0));
    }

    @Test
    void aNewCooldownIsANewEnd() {
        CooldownClock c = new CooldownClock();
        c.observe("a", 1000, 60, 1, 0);
        CooldownClock.Reading again = c.observe("a", 1100, 200, 1, 0);
        assertEquals(1300, again.end());
        assertEquals(200, again.total());
    }

    @Test
    void theFullLengthIsTheKnownOneOrTheFirstSeen() {
        CooldownClock c = new CooldownClock();
        assertEquals(100, c.observe("a", 1000, 40, 1, 100).total());
        CooldownClock d = new CooldownClock();
        assertEquals(40, d.observe("a", 1000, 40, 1, 0).total());
        assertEquals(40, d.observe("a", 1010, 30, 1, 0).total());
    }

    @Test
    void aPressSetsTheExactEndAndTotal() {
        CooldownClock c = new CooldownClock();
        c.notePress("a", 500, 400);
        assertEquals(new CooldownClock.Reading(900, 400), c.observe("a", 520, 380, 20, 0));
    }
}
