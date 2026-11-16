package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudActivityTest {
    @Test
    void theHudFadesAfterTenSecondsOfNothing() {
        assertEquals(1f, HudActivity.alphaFor(HudActivity.idleTicks(1_000, 1_000 + HudActivity.IDLE_TICKS)));
        assertEquals(HudActivity.FADED, HudActivity.alphaFor(HudActivity.idleTicks(1_000, 1_001 + HudActivity.IDLE_TICKS)));
    }

    @Test
    void aWorldYoungerThanTheLastUseOrNoUseAtAllCountsAsJustUsed() {
        // game time restarts in every world: a use at tick 90,000 and a world now at tick 10 must not clamp idle to 0 for good (it must count from the new use)
        assertEquals(0, HudActivity.idleTicks(90_000, 10));
        assertEquals(0, HudActivity.idleTicks(Long.MIN_VALUE, 10));
        // and once the next use is noted in the new world, it fades as usual
        assertEquals(HudActivity.FADED, HudActivity.alphaFor(HudActivity.idleTicks(10, 10 + HudActivity.IDLE_TICKS + 5)));
    }
}
