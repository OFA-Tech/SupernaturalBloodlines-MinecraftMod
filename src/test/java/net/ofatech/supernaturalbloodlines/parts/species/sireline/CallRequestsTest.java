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
}
