package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudActivityTest {
    @Test
    void aWorldYoungerThanTheLastUseOrNoUseAtAllCountsAsJustUsed() {
        // game time restarts in every world: a use at tick 90,000 and a world now at tick 10 must not clamp idle to 0 for good (it must count from the new use)
        assertEquals(0, HudActivity.idleTicks(90_000, 10));
        assertEquals(0, HudActivity.idleTicks(Long.MIN_VALUE, 10));
        assertEquals(HudActivity.IDLE_TICKS + 5, HudActivity.idleTicks(10, 10 + HudActivity.IDLE_TICKS + 5));
    }
}
