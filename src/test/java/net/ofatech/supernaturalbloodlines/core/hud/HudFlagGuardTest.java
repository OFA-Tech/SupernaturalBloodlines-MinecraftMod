package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The in-memory Tensura HUD flag we lower while our HUD is on, and put back exactly as the player had it (so ManasCore never saves our value). */
class HudFlagGuardTest {
    @Test
    void hidingRemembersThePlayersValueAndLowersTheFlagOnce() {
        HudFlagGuard g = new HudFlagGuard();
        assertEquals(Optional.of(false), g.tick(true, true));
        assertEquals(Optional.empty(), g.tick(true, false), "already lowered: no second write");
    }

    @Test
    void turningOurHudOffPutsTheOriginalBack() {
        HudFlagGuard g = new HudFlagGuard();
        g.tick(true, true);
        assertEquals(Optional.of(true), g.tick(false, false));
        assertEquals(Optional.empty(), g.tick(false, true), "nothing left to restore");
        assertFalse(g.active());
    }

    @Test
    void aPlayerWhoHadItOffGetsItOffBack() {
        HudFlagGuard g = new HudFlagGuard();
        assertEquals(Optional.empty(), g.tick(true, false), "it was already off: nothing to write");
        assertEquals(Optional.empty(), g.tick(false, false), "and nothing to restore: false it was, false it is");
        assertFalse(g.active());
    }

    @Test
    void nothingIsWrittenWhileWeNeverHidAnything() {
        HudFlagGuard g = new HudFlagGuard();
        assertEquals(Optional.empty(), g.tick(false, true));
        assertEquals(Optional.empty(), g.screenOpening());
    }

    @Test
    void tensurasSettingsScreenSeesThePlayersValueAndKeepsWhatThePlayerLeaves() {
        HudFlagGuard g = new HudFlagGuard();
        g.tick(true, true);
        assertEquals(Optional.of(true), g.screenOpening(), "the screen must show (and save) the player's value");
        assertEquals(Optional.empty(), g.tick(true, true), "while the screen is open we do not lower it again");
        g.screenClosed(false); // the player switched Tensura's own skill HUD off in the screen
        assertEquals(Optional.empty(), g.tick(true, false), "the flag is already false: nothing to write");
        assertEquals(Optional.empty(), g.tick(false, false), "and when ours goes off, the player's new false is what stays");
    }

    @Test
    void aPlayerWhoLeavesTheValueAloneInTheScreenGetsItBackLowered() {
        HudFlagGuard g = new HudFlagGuard();
        g.tick(true, true);
        g.screenOpening();
        g.screenClosed(true);
        assertEquals(Optional.of(false), g.tick(true, true), "ours is still on: lowered again");
        assertEquals(Optional.of(true), g.tick(false, false));
    }

    @Test
    void restoreNowGivesBackTheValueAtOnce() {
        HudFlagGuard g = new HudFlagGuard();
        g.tick(true, true);
        assertEquals(Optional.of(true), g.restoreNow(false));
        assertTrue(!g.active());
        assertEquals(Optional.empty(), g.restoreNow(true));
    }
}
