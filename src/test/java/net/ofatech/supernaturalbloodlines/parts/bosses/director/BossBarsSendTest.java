package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BossBarsSendTest {
    @Test
    void aWatcherWhoLeftTheBossesDimensionOrTheGameMayNoLongerWatch() {
        assertTrue(BossBars.stillViewing(true, false));
        assertFalse(BossBars.stillViewing(false, false), "another dimension");
        assertFalse(BossBars.stillViewing(true, true), "logged out");
        assertFalse(BossBars.stillViewing(false, true));
    }

    @Test
    void aBossThatIsAliveNeverShowsZeroHealthAndTheNumbersAreRoundedUp() {
        assertEquals(1, BossBars.shownHealth(0.4f));
        assertEquals(1640, BossBars.shownHealth(1639.2f));
        assertEquals(1640, BossBars.shownHealth(1640f));
        assertEquals(0, BossBars.shownHealth(0f));
        assertEquals(0, BossBars.shownHealth(-3f));
    }

    @Test
    void aViewerComingIntoRangeAlwaysGetsTheFactsEvenWhenNothingChanged() {
        assertTrue(BossBars.mustSend(false, false));
        assertTrue(BossBars.mustSend(false, true));
    }

    @Test
    void aViewerAlreadyWatchingGetsThemOnlyWhenTheyChange() {
        assertTrue(BossBars.mustSend(true, true));
        assertFalse(BossBars.mustSend(true, false), "the same facts are not sent again every half second");
    }
}
