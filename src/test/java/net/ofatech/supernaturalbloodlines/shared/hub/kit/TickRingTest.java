package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TickRingTest {
    @Test
    void theFirstTickIsAtTheTopAndTheRingGoesClockwise() {
        List<TickRing.Tick> t = TickRing.ticks(50, 50, 20, 24, 0f);
        assertEquals(24, t.size());
        assertEquals(50, t.get(0).x());
        assertEquals(30, t.get(0).y());
        assertTrue(t.get(1).x() > 50 && t.get(1).y() > 30, "the second tick is to the right of the first");
        assertTrue(t.get(6).x() > 66 && Math.abs(t.get(6).y() - 50) <= 1, "a quarter turn is on the right");
    }

    @Test
    void theLitTicksFollowTheProgressAndClamp() {
        assertEquals(0, TickRing.lit(24, 0f));
        assertEquals(12, TickRing.lit(24, 0.5f));
        assertEquals(24, TickRing.lit(24, 1f));
        assertEquals(24, TickRing.lit(24, 7f));
        assertEquals(0, TickRing.lit(24, -3f));
        long on = TickRing.ticks(0, 0, 10, 24, 0.5f).stream().filter(TickRing.Tick::on).count();
        assertEquals(12, on);
        assertTrue(TickRing.ticks(0, 0, 10, 24, 0.5f).get(11).on() && !TickRing.ticks(0, 0, 10, 24, 0.5f).get(12).on(), "the lit ones are the first");
    }
}
