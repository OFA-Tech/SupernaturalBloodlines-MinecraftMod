package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReadyGlowTrackerTest {
    private static final boolean[] ALL = {true, true, true};

    private static float[] frame(ReadyGlow.Tracker t, float[] cooldowns, boolean[] eligible, int context, long time, boolean reduce) {
        float[] out = new float[cooldowns.length];
        t.update(cooldowns, eligible, context, time, reduce, out);
        return out;
    }

    @Test
    void aSlotWhoseCooldownRunsOutGlowsAndAnotherOneDoesNot() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0f, 0f}, ALL, 1, 100, false);
        float[] out = frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 101, false);
        assertTrue(out[0] > 0f, "the slot that was cooling");
        assertEquals(0f, out[1]);
        assertEquals(0f, out[2]);
    }

    @Test
    void aModeSwitchOrANewLoadoutNeverMakesASlotGlow() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0.2f, 0f}, ALL, 1, 100, false);
        // the other mode has other slots in these places, none cooling: the cooldowns "ending" are not ready slots
        float[] out = frame(t, new float[] {0f, 0f, 0f}, ALL, 2, 101, false);
        for (float f : out) {
            assertEquals(0f, f, "a switch is not a slot becoming ready");
        }
        // and afterwards the tracking is normal again
        frame(t, new float[] {0.3f, 0f, 0f}, ALL, 2, 102, false);
        assertTrue(frame(t, new float[] {0f, 0f, 0f}, ALL, 2, 103, false)[0] > 0f);
    }

    @Test
    void theHudHiddenForAWhileStartsTheTrackingOver() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0f, 0f}, ALL, 1, 100, false);
        t.reset();
        float[] out = frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 500, false);
        assertEquals(0f, out[0], "the cooldown ended while nobody was looking");
    }

    @Test
    void anEmptyOrGreyedSlotNeverGlowsAndDoesNotGlowWhenItComesBack() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0f, 0f}, ALL, 1, 100, false);
        boolean[] greyed = {false, true, true};
        assertEquals(0f, frame(t, new float[] {0f, 0f, 0f}, greyed, 1, 101, false)[0], "greyed while its cooldown ended");
        assertEquals(0f, frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 102, false)[0], "and not when it is usable again");
    }

    @Test
    void reduceMotionSwitchesTheGlowOffAtTheSource() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0f, 0f}, ALL, 1, 100, true);
        assertEquals(0f, frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 101, true)[0]);
    }

    @Test
    void theGlowEndsAfterOneLoop() {
        ReadyGlow.Tracker t = new ReadyGlow.Tracker(3);
        frame(t, new float[] {0.4f, 0f, 0f}, ALL, 1, 100, false);
        assertTrue(frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 101, false)[0] > 0f);
        assertEquals(0f, frame(t, new float[] {0f, 0f, 0f}, ALL, 1, 101 + ReadyGlow.TICKS, false)[0]);
    }
}
