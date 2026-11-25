package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterRules.Sighting;
import org.junit.jupiter.api.Test;

class CharterRulesTest {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long HOUR = 60L * 60 * 1000;
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void lostAfterSevenDaysWithNoSightingAndNoHolder() {
        assertTrue(CharterRules.isLost(0, false, 0, 7 * DAY, 7, 3));
        assertTrue(CharterRules.isLost(0, false, 0, 9 * DAY, 7, 3));
    }

    @Test
    void notLostAtSixDays() {
        assertFalse(CharterRules.isLost(0, false, 0, 6 * DAY, 7, 3));
        assertFalse(CharterRules.isLost(0, false, 0, 7 * DAY - 1, 7, 3));
    }

    @Test
    void lostWhenTheHolderHasBeenOfflineTheAbsentDays() {
        assertTrue(CharterRules.isLost(0, true, 10 * DAY, 13 * DAY, 7, 3));
        assertFalse(CharterRules.isLost(0, true, 10 * DAY, 13 * DAY - 1, 7, 3));
        assertFalse(CharterRules.isLost(0, true, 0, 100 * DAY, 7, 3), "a holder who is online is never absent");
        assertFalse(CharterRules.isLost(0, true, 10 * DAY, 100 * DAY, 7, 0), "0 days of absence means never absent");
    }

    @Test
    void aStoredCharterIsNotLostBeforeTheDays() {
        assertFalse(CharterRules.isLost(5 * DAY, false, 0, 11 * DAY, 7, 3));
        assertTrue(CharterRules.isLost(5 * DAY, false, 0, 12 * DAY, 7, 3));
    }

    @Test
    void duplicatesKeepTheRowsHolder() {
        List<Sighting> seen = List.of(new Sighting(A, 0), new Sighting(B, 1), new Sighting(C, 2));
        CharterRules.Kept kept = CharterRules.keep(seen, Optional.of(B));
        assertEquals(Optional.of(new Sighting(B, 1)), kept.kept());
        assertEquals(List.of(new Sighting(A, 0), new Sighting(C, 2)), kept.dust());
    }

    @Test
    void duplicatesWithNoRowHolderKeepTheFirstInScanOrder() {
        List<Sighting> seen = List.of(new Sighting(A, 0), new Sighting(B, 1));
        CharterRules.Kept kept = CharterRules.keep(seen, Optional.empty());
        assertEquals(Optional.of(new Sighting(A, 0)), kept.kept());
        assertEquals(List.of(new Sighting(B, 1)), kept.dust());
        assertEquals(Optional.of(new Sighting(A, 0)), CharterRules.keep(seen, Optional.of(C)).kept(), "a row holder who was not seen keeps nobody out");
    }

    @Test
    void oneHolderWithTwoStacksKeepsOne() {
        List<Sighting> seen = List.of(new Sighting(A, 0), new Sighting(A, 1));
        CharterRules.Kept kept = CharterRules.keep(seen, Optional.of(A));
        assertEquals(Optional.of(new Sighting(A, 0)), kept.kept());
        assertEquals(List.of(new Sighting(A, 1)), kept.dust());
        assertEquals(Optional.empty(), CharterRules.keep(List.of(), Optional.of(A)).kept());
    }

    @Test
    void aStackOfAnotherIdIsInvalid() {
        assertTrue(CharterRules.valid(A, A, B, B));
        assertFalse(CharterRules.valid(A, C, B, B));
    }

    @Test
    void aStackOfAnotherGroupIsInvalid() {
        assertFalse(CharterRules.valid(A, A, B, C));
    }

    @Test
    void reforgeNeedsLostAnd24Hours() {
        assertEquals(Optional.empty(), CharterRules.mayReforge(CharterState.LOST, 0, 24 * HOUR));
        assertEquals(Optional.of("reforge_wait"), CharterRules.mayReforge(CharterState.LOST, 0, 24 * HOUR - 60_000));
        assertEquals(Optional.of("not_lost"), CharterRules.mayReforge(CharterState.HELD, 0, 100 * HOUR));
        assertEquals(Optional.of("not_lost"), CharterRules.mayReforge(CharterState.STORED, 0, 100 * HOUR));
    }

    @Test
    void forgeNeedsThreeMembersAndNoLiveCharter() {
        assertEquals(Optional.empty(), CharterRules.mayForge(3, null, 3));
        assertEquals(Optional.of("too_few"), CharterRules.mayForge(2, null, 3));
        assertEquals(Optional.of("already"), CharterRules.mayForge(3, CharterState.HELD, 3));
        assertEquals(Optional.of("already"), CharterRules.mayForge(3, CharterState.STORED, 3));
        assertEquals(Optional.of("already"), CharterRules.mayForge(3, CharterState.DROPPED, 3));
    }

    @Test
    void aLostCharterIsReforgedNotForged() {
        assertEquals(Optional.of("lost_use_reforge"), CharterRules.mayForge(3, CharterState.LOST, 3));
    }
}
