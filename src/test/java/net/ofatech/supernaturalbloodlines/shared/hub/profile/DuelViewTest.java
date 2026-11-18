package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Role;
import org.junit.jupiter.api.Test;

class DuelViewTest {
    @Test
    void thePhaseWordsMapAndAnythingElseIsNoDuel() {
        assertEquals(DuelView.Phase.ON, DuelView.of("on", "Hayley", 2400).orElseThrow().phase());
        assertEquals(DuelView.Phase.ASKED, DuelView.of("asked", "Hayley", 100).orElseThrow().phase());
        assertEquals(DuelView.Phase.PAUSED, DuelView.of("paused", "Hayley", 900).orElseThrow().phase());
        assertTrue(DuelView.of("nonsense", "Hayley", 900).isEmpty());
    }

    @Test
    void theClockReadsMinutesFromAMinuteAndSecondsBelow() {
        assertEquals("2:00", DuelView.of("on", "H", 2400).orElseThrow().clock());
        assertEquals("1:42", DuelView.of("on", "H", 2040).orElseThrow().clock());
        assertEquals("45 s", DuelView.of("paused", "H", 900).orElseThrow().clock());
        assertEquals("0 s", DuelView.of("on", "H", -5).orElseThrow().clock());
    }

    @Test
    void aDuelWithThemDisablesTheChallengeWithItsReason() {
        UUID id = UUID.randomUUID();
        var viewer = new ProfileActions.Viewer(true, false, true, true, false, true, false);
        var open = ProfileActions.of(viewer, new ProfileActions.Subject(id, "Hayley", false, Role.LEADER, false, false, false), false);
        var inDuel = ProfileActions.of(viewer, new ProfileActions.Subject(id, "Hayley", false, Role.LEADER, false, false, true), false);
        assertTrue(open.get(0).enabled() && open.get(0).disabled() == null);
        assertEquals("challenge", inDuel.get(0).id());
        assertTrue(!inDuel.get(0).enabled() && "in_duel".equals(inDuel.get(0).disabled()));
    }

    @Test
    void anNpcOffersNoPlayerActionsToAnyViewer() {
        UUID id = UUID.randomUUID();
        var leader = new ProfileActions.Viewer(true, true, false, true, false, true, true);
        assertTrue(ProfileActions.of(leader, new ProfileActions.Subject(id, "Thrall", false, Role.NONE, true, false, false, true), true).isEmpty());
        assertTrue(!ProfileActions.of(leader, new ProfileActions.Subject(id, "Kid", false, Role.NONE, true, false, false, false), false).isEmpty());
    }
}
