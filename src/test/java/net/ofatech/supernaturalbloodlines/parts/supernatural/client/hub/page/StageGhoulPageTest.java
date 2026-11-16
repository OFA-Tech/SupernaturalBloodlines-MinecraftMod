package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StageGhoulPageTest {
    @Test
    void theClockIsMinutesAndSecondsOfTheTurningsTicks() {
        assertEquals("14 : 20", StageGhoulPage.clock(17_200));
        assertEquals("00 : 00", StageGhoulPage.clock(0));
        assertEquals("00 : 00", StageGhoulPage.clock(-5));
        assertEquals("00 : 59", StageGhoulPage.clock(59 * 20 + 19));
        assertEquals("01 : 00", StageGhoulPage.clock(60 * 20));
    }

    @Test
    void aVeryLongTurningStopsAtNinetyNineFiftyNine() {
        assertEquals("99 : 59", StageGhoulPage.clock(10_000_000));
    }
}
