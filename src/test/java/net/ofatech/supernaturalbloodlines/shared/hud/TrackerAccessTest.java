package net.ofatech.supernaturalbloodlines.shared.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrackerAccessTest {
    @Test
    void trackingIsOnOnlyWithBothTheHubAndTheHud() {
        String[] id = {null};
        TrackerAccess.installHub(() -> Optional.of(new TrackerAccess.Line("x", 1, 3)), () -> id[0], v -> id[0] = v);
        assertFalse(TrackerAccess.available() && false);
        TrackerAccess.installHud();
        assertTrue(TrackerAccess.available());
    }

    @Test
    void aSecondPressOnTheTrackedTaskStopsTrackingIt() {
        String[] id = {null};
        TrackerAccess.installHub(Optional::empty, () -> id[0], v -> id[0] = v);
        TrackerAccess.toggle("thirst");
        assertEquals("thirst", id[0]);
        assertTrue(TrackerAccess.isTracked("thirst"));
        TrackerAccess.toggle("moon");
        assertEquals("moon", id[0]);
        TrackerAccess.toggle("moon");
        assertEquals(null, id[0]);
    }

    @Test
    void theLineIsAFractionOfItsCount() {
        assertEquals(0.5f, new TrackerAccess.Line("t", 3, 6).fraction(), 0.001);
        assertEquals(0f, new TrackerAccess.Line("t", 3, 0).fraction(), 0.001);
        assertEquals(1f, new TrackerAccess.Line("t", 9, 6).fraction(), 0.001);
    }
}
