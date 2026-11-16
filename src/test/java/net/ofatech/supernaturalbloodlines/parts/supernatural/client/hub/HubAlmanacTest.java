package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HubAlmanacTest {
    @Test
    void theFullMoonIsEightDaysApart() {
        assertEquals(0, HubAlmanac.daysToFull(0));
        assertEquals(7, HubAlmanac.daysToFull(1));
        assertEquals(4, HubAlmanac.daysToFull(4));
        assertEquals(1, HubAlmanac.daysToFull(7));
    }

    @Test
    void thePhasesRunOnAndWrap() {
        assertEquals(List.of(6, 7, 0, 1, 2, 3), HubAlmanac.phases(6, 6));
        assertEquals(List.of(0), HubAlmanac.phases(8, 1));
        assertEquals(List.of(7), HubAlmanac.phases(-1, 1));
    }
}
