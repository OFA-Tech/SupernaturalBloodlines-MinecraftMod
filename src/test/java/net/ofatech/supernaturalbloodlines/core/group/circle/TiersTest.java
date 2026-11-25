package net.ofatech.supernaturalbloodlines.core.group.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TiersTest {
    @Test
    void thresholds() {
        assertEquals(0, Tiers.of(2, 3, 7, 13));
        assertEquals(1, Tiers.of(3, 3, 7, 13));
        assertEquals(1, Tiers.of(6, 3, 7, 13));
        assertEquals(2, Tiers.of(7, 3, 7, 13));
        assertEquals(2, Tiers.of(12, 3, 7, 13));
        assertEquals(3, Tiers.of(13, 3, 7, 13));
    }

    @Test
    void aTripleThatIsNotIncreasingIsClamped() {
        assertEquals(0, Tiers.of(6, 7, 3, 13));
        assertEquals(2, Tiers.of(7, 7, 3, 13));
        assertEquals(2, Tiers.of(13, 7, 3, 20));
        assertEquals(3, Tiers.of(20, 7, 3, 20));
    }
}