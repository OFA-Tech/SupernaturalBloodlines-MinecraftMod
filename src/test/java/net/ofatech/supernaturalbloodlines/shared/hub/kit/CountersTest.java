package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CountersTest {
    @Test
    void spAndEpReadAsInTheDesign() {
        Counters c = new Counters(6, 1038L);
        assertEquals("6 SP", c.spText());
        assertEquals("1,038 EP", c.epText());
    }

    @Test
    void withoutTensuraThereIsNoEp() {
        assertNull(new Counters(0, null).epText());
        assertEquals("0 SP", new Counters(0, null).spText());
    }
}
