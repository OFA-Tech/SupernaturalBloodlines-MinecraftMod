package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotionStateTest {
    @Test
    void aTweenRunsFromItsStartToItsDuration() {
        assertEquals(0f, Motion.progress(10, 10, 6, Easing.LINEAR, false), 1e-6);
        assertEquals(0.5f, Motion.progress(10, 13, 6, Easing.LINEAR, false), 1e-6);
        assertEquals(1f, Motion.progress(10, 16, 6, Easing.LINEAR, false), 1e-6);
        assertEquals(1f, Motion.progress(10, 99, 6, Easing.LINEAR, false), 1e-6);
    }

    @Test
    void positionsAreWholeTexels() {
        assertEquals(4, Motion.texels(0, 4, 1f));
        assertEquals(0, Motion.texels(0, 4, 0f));
        assertEquals(2, Motion.texels(0, 4, 0.5f));
        assertEquals(1, Motion.texels(0, 4, 0.3f));
        assertEquals(-4, Motion.texels(-4, 0, 0f));
        assertEquals(7, Motion.texels(10, 4, 0.5f));
    }

    @Test
    void theReadyGlowLoopsThroughFourBands() {
        assertEquals(32, Motion.glowPercent(0, false));
        assertEquals(50, Motion.glowPercent(8, false));
        assertEquals(60, Motion.glowPercent(15, false));
        assertEquals(50, Motion.glowPercent(23, false));
        assertEquals(32, Motion.glowPercent(30, false));
        assertEquals(60, Motion.glowPercent(30 * 5 + 15, false));
    }

    @Test
    void anElementThatIsSeenForTheFirstTimeIsAtRest() {
        AnimClock clock = new AnimClock();
        assertEquals(1f, clock.progress("tab:path", "off", 100, Motion.PAGE_CHANGE, Easing.LINEAR, false), 1e-6);
    }

    @Test
    void aChangeOfStateStartsTheTweenAndTheSameStateDoesNotRestartIt() {
        AnimClock clock = new AnimClock();
        clock.progress("slot", "idle", 100, 4, Easing.LINEAR, false);
        assertEquals(0f, clock.progress("slot", "pressed", 120, 4, Easing.LINEAR, false), 1e-6);
        assertEquals(0.5f, clock.progress("slot", "pressed", 122, 4, Easing.LINEAR, false), 1e-6);
        assertEquals(1f, clock.progress("slot", "pressed", 124, 4, Easing.LINEAR, false), 1e-6);
        // drawn again at 130 in the same state: still finished, not restarted
        assertEquals(1f, clock.progress("slot", "pressed", 130, 4, Easing.LINEAR, false), 1e-6);
    }

    @Test
    void eachElementHasItsOwnClock() {
        AnimClock clock = new AnimClock();
        clock.progress("a", 0, 10, 4, Easing.LINEAR, false);
        clock.progress("b", 0, 10, 4, Easing.LINEAR, false);
        clock.progress("a", 1, 20, 4, Easing.LINEAR, false);
        assertEquals(0.5f, clock.progress("a", 1, 22, 4, Easing.LINEAR, false), 1e-6);
        assertEquals(1f, clock.progress("b", 0, 22, 4, Easing.LINEAR, false), 1e-6);
    }

    @Test
    void clearingForgetsEveryElement() {
        AnimClock clock = new AnimClock();
        clock.progress("a", 0, 10, 4, Easing.LINEAR, false);
        clock.clear();
        assertTrue(clock.since("a", 5, 50) < 0, "after a clear the next sighting is a first sighting, at rest");
    }
}
