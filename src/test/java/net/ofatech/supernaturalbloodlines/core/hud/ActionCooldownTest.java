package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActionCooldownTest {
    @Test
    void theTicksLeftCountDownFromTheLengthAndEndAtZero() {
        assertEquals(1200, ActionCooldown.left(100, 1200, 100));
        assertEquals(600, ActionCooldown.left(100, 1200, 700));
        assertEquals(1, ActionCooldown.left(100, 1200, 1299));
        assertEquals(0, ActionCooldown.left(100, 1200, 1300));
    }

    @Test
    void aNewWorldsClockThatWentBackMeansNoCooldown() {
        assertEquals(0, ActionCooldown.left(90_000, 1200, 10));
    }

    @Test
    void theSecondsMoveAndNeverReadZeroWhileRunning() {
        assertEquals(60, ActionCooldown.seconds(1200));
        assertEquals(59, ActionCooldown.seconds(1180));
        assertEquals(1, ActionCooldown.seconds(1));
        assertEquals(0, ActionCooldown.seconds(0));
    }

    @Test
    void theSweepIsTheShareLeft() {
        assertEquals(1f, ActionCooldown.fraction(1200, 1200));
        assertEquals(0.5f, ActionCooldown.fraction(600, 1200));
        assertEquals(0f, ActionCooldown.fraction(0, 1200));
        assertEquals(0f, ActionCooldown.fraction(5, 0));
    }
}
