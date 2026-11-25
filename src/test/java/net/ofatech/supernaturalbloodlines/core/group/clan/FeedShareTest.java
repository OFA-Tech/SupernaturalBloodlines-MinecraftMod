package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeedShareTest {
    private static final UUID FEEDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MATE = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void theShareTable() {
        int[][] rows = {{0, 0}, {1, 1}, {5, 1}, {10, 1}, {20, 2}, {40, 2}, {100, 2}};
        for (int[] row : rows) {
            assertEquals(row[1], FeedShare.of(row[0], 10, 2), "amount " + row[0]);
        }
        assertEquals(10, FeedShare.of(100, 10, 20));
        assertEquals(0, FeedShare.of(-3, 10, 2));
    }

    @Test
    void aShareIsNeverAboveTheMax() {
        assertEquals(3, FeedShare.of(1000, 50, 3));
    }

    @Test
    void aShareIsAtLeastOneWhenThereIsBlood() {
        assertEquals(1, FeedShare.of(1, 1, 5));
        assertEquals(1, FeedShare.of(3, 10, 5));
    }

    @Test
    void aMateTakesAtMostTheCapPerMinute() {
        FeedLedger l = new FeedLedger();
        assertEquals(2, l.grant(FEEDER, MATE, 2, 1_000, 0, 4, 100));
        assertEquals(2, l.grant(OTHER, MATE, 2, 2_000, 10, 4, 100));
        assertEquals(0, l.grant(FEEDER, MATE, 2, 3_000, 200, 4, 100), "the cap of four is used up");
        FeedLedger t = new FeedLedger();
        assertEquals(3, t.grant(FEEDER, MATE, 3, 1_000, 0, 4, 100));
        assertEquals(1, t.grant(OTHER, MATE, 3, 2_000, 10, 4, 100), "trimmed to what the minute's cap leaves");
        assertEquals(0, t.grant(FEEDER, MATE, 3, 3_000, 300, 4, 100));
    }

    @Test
    void theWindowSlidesAfterAMinute() {
        FeedLedger l = new FeedLedger();
        assertEquals(4, l.grant(FEEDER, MATE, 4, 1_000, 0, 4, 100));
        assertEquals(0, l.grant(OTHER, MATE, 2, 60_999, 5, 4, 100), "still inside the minute");
        assertEquals(2, l.grant(OTHER, MATE, 2, 61_000, 6, 4, 100), "a minute later the blood no longer counts");
    }

    @Test
    void theSameFeederSpacingBlocksTheSecondShare() {
        FeedLedger l = new FeedLedger();
        assertEquals(1, l.grant(FEEDER, MATE, 1, 1_000, 1000, 100, 100));
        assertEquals(0, l.grant(FEEDER, MATE, 1, 1_001, 1099, 100, 100), "inside 100 ticks");
        assertEquals(1, l.grant(FEEDER, MATE, 1, 1_002, 1100, 100, 100), "at 100 ticks");
    }

    @Test
    void anotherFeederIsNotBlockedBySpacing() {
        FeedLedger l = new FeedLedger();
        assertEquals(1, l.grant(FEEDER, MATE, 1, 1_000, 1000, 100, 100));
        assertEquals(1, l.grant(OTHER, MATE, 1, 1_001, 1001, 100, 100));
    }

    @Test
    void aClockThatGoesBackwardsAddsAndClearsNothing() {
        FeedLedger l = new FeedLedger();
        assertEquals(3, l.grant(FEEDER, MATE, 3, 100_000, 0, 4, 100));
        assertEquals(0, l.grant(OTHER, MATE, 3, 50_000, 500, 4, 100), "time went back: nothing is granted");
        assertEquals(1, l.grant(OTHER, MATE, 3, 100_001, 500, 4, 100), "and nothing was added or forgotten meanwhile: one is left of the four");
        assertTrue(l.grant(OTHER, MATE, 3, 100_002, 900, 4, 100) == 0);
    }
}
