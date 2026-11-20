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

    @Test
    void aCooldownLearnedHalfWayHasTheRightFractionAndSeconds() {
        // a login 300 ticks into a 1200 tick cooldown: 900 left, so the sweep is 75 percent, not 100
        long now = 5_000;
        long at = ActionCooldown.startedAt(now, 1200, 900);
        assertEquals(900, ActionCooldown.left(at, 1200, now));
        assertEquals(0.75f, ActionCooldown.fraction(ActionCooldown.left(at, 1200, now), 1200), 1e-6);
        assertEquals(45, ActionCooldown.seconds(ActionCooldown.left(at, 1200, now)));
        assertEquals(600, ActionCooldown.left(at, 1200, now + 300), "and it keeps counting down from there");
        assertEquals(0, ActionCooldown.left(at, 1200, now + 900));
    }

    @Test
    void aLeftLongerThanTheTotalOrBelowZeroIsClamped() {
        assertEquals(1200, ActionCooldown.left(ActionCooldown.startedAt(100, 1200, 5000), 1200, 100));
        assertEquals(0, ActionCooldown.left(ActionCooldown.startedAt(100, 1200, -5), 1200, 100));
    }
}
