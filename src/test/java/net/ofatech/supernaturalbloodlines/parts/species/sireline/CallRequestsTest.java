package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CallRequestsTest {
    private static final UUID CALLER = UUID.randomUUID();
    private static final UUID SIRED = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void theSiredCanTakeTheirRequestOnce() {
        CallRequests r = new CallRequests();
        String token = r.open(CALLER, SIRED, 100);
        assertEquals(CALLER, r.take(token, SIRED, 150).orElseThrow().caller());
        assertTrue(r.take(token, SIRED, 150).isEmpty());
    }

    @Test
    void peekingLeavesTheRequestInPlaceAndDropsAnExpiredOne() {
        CallRequests r = new CallRequests();
        String token = r.open(CALLER, SIRED, 100);
        assertEquals(CALLER, r.peek(token, SIRED, 150).orElseThrow().caller());
        assertEquals(CALLER, r.peek(token, SIRED, 150).orElseThrow().caller(), "a peek used the request up");
        assertTrue(r.peek(token, OTHER, 150).isEmpty());
        assertTrue(r.peek(token, SIRED, 100 + CallRequests.TTL_TICKS + 1).isEmpty());
        assertEquals(0, r.size(), "an expired request stayed");
    }

    @Test
    void someoneElseCannotUseTheToken() {
        CallRequests r = new CallRequests();
        String token = r.open(CALLER, SIRED, 100);
        assertTrue(r.take(token, OTHER, 100).isEmpty());
        assertTrue(r.take(token, SIRED, 100).isPresent(), "a wrong taker must not use the request up");
    }

    @Test
    void itExpiresAfterSixtySeconds() {
        CallRequests r = new CallRequests();
        String token = r.open(CALLER, SIRED, 100);
        assertTrue(r.take(token, SIRED, 100 + CallRequests.TTL_TICKS + 1).isEmpty());
        assertEquals(0, r.size());
    }

    @Test
    void aSecondCallReplacesTheFirstForTheSamePair() {
        CallRequests r = new CallRequests();
        String first = r.open(CALLER, SIRED, 100);
        String second = r.open(CALLER, SIRED, 200);
        assertNotEquals(first, second);
        assertTrue(r.take(first, SIRED, 200).isEmpty());
        assertTrue(r.take(second, SIRED, 200).isPresent());
    }

    @Test
    void logoutForgetsBothSides() {
        CallRequests r = new CallRequests();
        String a = r.open(CALLER, SIRED, 100);
        String b = r.open(OTHER, CALLER, 100);
        r.forget(CALLER);
        assertTrue(r.take(a, SIRED, 100).isEmpty());
        assertTrue(r.take(b, CALLER, 100).isEmpty());
        assertEquals(0, r.size());
    }

    @Test
    void anUnknownTokenIsNothing() {
        assertTrue(new CallRequests().take("nope", SIRED, 0).isEmpty());
    }

    @Test
    void theHubListsOnlyTheOpenRequestsOfThePlayerNewestFirstAndNeverAnExpiredOne() {
        CallRequests r = new CallRequests();
        String old = r.open(CALLER, SIRED, 100);
        String newer = r.open(OTHER, SIRED, 200);
        r.open(CALLER, OTHER, 300); // somebody else's
        var open = r.openFor(SIRED, 250);
        assertEquals(2, open.size());
        assertEquals(newer, open.get(0).getKey(), "newest first");
        assertEquals(old, open.get(1).getKey());
        assertEquals(newer, r.latestFor(SIRED, 250).orElseThrow(), "a button with no token answers the newest");
        // the older one has run out, the newer is on its last tick, then it too is gone
        assertEquals(1, r.openFor(SIRED, 100 + CallRequests.TTL_TICKS + 1).size());
        assertEquals(newer, r.latestFor(SIRED, 200 + CallRequests.TTL_TICKS).orElseThrow());
        assertTrue(r.latestFor(SIRED, 200 + CallRequests.TTL_TICKS + 1).isEmpty());
        assertTrue(r.openFor(SIRED, 200 + CallRequests.TTL_TICKS + 1).isEmpty());
        assertTrue(r.latestFor(UUID.randomUUID(), 0).isEmpty());
    }
}
