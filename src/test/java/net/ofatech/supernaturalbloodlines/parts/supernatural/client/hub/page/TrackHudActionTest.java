package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;
import org.junit.jupiter.api.Test;

/** "Track on HUD" is a real action in every theme (the Grimoire's links and the Stage pages' buttons both come from it), not a stub. */
class TrackHudActionTest {
    private static String[] install() {
        String[] id = {null};
        TrackerAccess.installHub(Optional::empty, () -> id[0], v -> id[0] = v);
        TrackerAccess.installHud();
        return id;
    }

    @Test
    void withTheHudItTracksAndStopsOnASecondPress() {
        String[] id = install();
        Detail.Action a = TrackHud.action("thirst", false);
        assertTrue(a.enabled());
        assertNotNull(a.run());
        a.run().run();
        assertEquals("thirst", id[0]);
        assertTrue(TrackHud.action("thirst", false).label().getString().contains("untrack"), "the label turns to stop tracking");
        TrackHud.action("thirst", false).run().run();
        assertEquals(null, id[0]);
    }

    @Test
    void aTaskMissingForAFewSecondsIsKeptAndThenForgotten() {
        assertFalse(TrackHud.expired(Long.MIN_VALUE, 10_000), "never missing");
        assertFalse(TrackHud.expired(1_000, 1_000 + TrackHud.GRACE_TICKS), "a sync gap of a moment");
        assertTrue(TrackHud.expired(1_000, 1_001 + TrackHud.GRACE_TICKS), "gone for good");
    }

    @Test
    void aClaimedTaskCannotBeTrackedAndSaysWhy() {
        install();
        Detail.Action a = TrackHud.action("thirst", true);
        assertFalse(a.enabled());
        assertNotNull(a.disabledReason());
    }
}
