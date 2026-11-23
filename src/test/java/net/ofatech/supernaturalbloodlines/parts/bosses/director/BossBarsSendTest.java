package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BossBarsSendTest {
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
