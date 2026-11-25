package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MidnightTest {
    @Test
    void nextMidnightAfter() {
        assertEquals(18_000, Midnight.nextAtOrAfter(0));
        assertEquals(18_000, Midnight.nextAtOrAfter(18_000));
        assertEquals(42_000, Midnight.nextAtOrAfter(18_001));
        assertEquals(42_000, Midnight.nextAtOrAfter(20_390));
        assertEquals(18_000, Midnight.nextAtOrAfter(-5_000));
        assertEquals(18_000 + 24_000 * 5, Midnight.nextAtOrAfter(18_000 + 24_000 * 4 + 1));
    }
}
