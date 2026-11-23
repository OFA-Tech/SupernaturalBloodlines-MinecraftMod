package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitThrottleTest {
    @Test
    void theFirstHitIsAlwaysSent() {
        assertTrue(HitThrottle.allow(HitThrottle.NEVER, 0));
        assertTrue(HitThrottle.allow(HitThrottle.NEVER, 123456));
    }

    @Test
    void aFlurryIsOneMessageEveryHalfSecondPerBoss() {
        assertFalse(HitThrottle.allow(100, 101));
        assertFalse(HitThrottle.allow(100, 100 + HitThrottle.MIN_GAP - 1));
        assertTrue(HitThrottle.allow(100, 100 + HitThrottle.MIN_GAP));
        assertTrue(HitThrottle.allow(100, 5000));
    }

    @Test
    void aClockThatWentBackSendsInsteadOfWaitingForever() {
        assertTrue(HitThrottle.allow(5000, 12), "a new world started the game time again");
    }
}
