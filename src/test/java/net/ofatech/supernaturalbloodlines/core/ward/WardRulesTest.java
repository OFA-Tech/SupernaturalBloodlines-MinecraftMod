package net.ofatech.supernaturalbloodlines.core.ward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WardRulesTest {
    // blocks(enabled, standing, vampiric, wolfLike, blocksWolves, creativeOrSpectator, owner, invited)
    @Test
    void anUninvitedVampireIsBlockedAndEveryoneElseIsNot() {
        assertTrue(WardRules.blocks(true, true, true, false, false, false, false, false));
        assertFalse(WardRules.blocks(true, true, false, false, false, false, false, false), "a human is never blocked");
        assertFalse(WardRules.blocks(true, true, true, false, false, false, true, false), "the owner");
        assertFalse(WardRules.blocks(true, true, true, false, false, false, false, true), "an invited one");
        assertFalse(WardRules.blocks(true, true, true, false, false, true, false, false), "creative or spectator");
        assertFalse(WardRules.blocks(false, true, true, false, false, false, false, false), "ward_enabled off");
        assertFalse(WardRules.blocks(true, false, true, false, false, false, false, false), "a fallen ward");
    }

    @Test
    void werewolvesAreBlockedOnlyWhenTheSettingSaysSo() {
        assertFalse(WardRules.blocks(true, true, false, true, false, false, false, false));
        assertTrue(WardRules.blocks(true, true, false, true, true, false, false, false));
        assertFalse(WardRules.blocks(true, true, false, false, true, false, false, false), "a human with the setting on");
    }

    @Test
    void onlyANonVampiricOwnerMayRaiseAWard() {
        assertTrue(WardRules.mayOwn(false));
        assertFalse(WardRules.mayOwn(true));
    }

    @Test
    void theBoxHasHalfSizePlusOneCellsOnEachSideOfTheCentreCell() {
        WardRules.Box b = new WardRules.Box(100, 64, 100, 2);
        assertTrue(b.contains(98.0, 64.0, 98.0));
        assertTrue(b.contains(102.99, 66.99, 102.99));
        assertFalse(b.contains(97.99, 64, 100));
        assertFalse(b.contains(103.0, 64, 100));
        assertFalse(b.contains(100, 61.99, 100));
        assertFalse(b.contains(100, 67.0, 100));
    }

    @Test
    void nearMeansInsideTheBoxGrownByTheMargin() {
        WardRules.Box b = new WardRules.Box(0, 64, 0, 2);
        assertTrue(b.near(7.9, 64, 0, 5));
        assertFalse(b.near(8.1, 64, 0, 5));
        assertTrue(b.near(-6.9, 64, 0, 5));
        assertFalse(b.near(0, 72.1, 0, 5));
        assertTrue(b.near(0, 64, 0, 0), "inside is near at no margin");
    }

    @Test
    void theNearestFaceIsTheWayOut() {
        WardRules.Box b = new WardRules.Box(0, 64, 0, 10);
        WardRules.Dir west = WardRules.pushDirection(b, -9.0, 0.5);
        assertEquals(-1.0, west.x(), 1e-9);
        assertEquals(0.0, west.z(), 1e-9);
        WardRules.Dir east = WardRules.pushDirection(b, 10.5, 0.5);
        assertEquals(1.0, east.x(), 1e-9);
        WardRules.Dir north = WardRules.pushDirection(b, 0.5, -9.5);
        assertEquals(-1.0, north.z(), 1e-9);
        WardRules.Dir south = WardRules.pushDirection(b, 0.5, 10.0);
        assertEquals(0.0, south.x(), 1e-9);
        assertEquals(1.0, south.z(), 1e-9);
        assertEquals(WardRules.pushDirection(b, 0.5, 0.5), WardRules.pushDirection(b, 0.5, 0.5), "the dead centre is deterministic");
    }

    @Test
    void theExitPointIsJustOutsideTheNearestFace() {
        WardRules.Box b = new WardRules.Box(0, 64, 0, 10);
        WardRules.Point west = WardRules.exitPoint(b, -9.0, 0.5);
        assertTrue(west.x() < -10.0 && !b.contains(west.x(), 64, west.z()));
        assertEquals(0.5, west.z(), 1e-9);
        WardRules.Point east = WardRules.exitPoint(b, 10.4, 0.5);
        assertTrue(east.x() >= 11.0 && !b.contains(east.x(), 64, east.z()));
        WardRules.Point north = WardRules.exitPoint(b, 0.5, -9.9);
        assertTrue(north.z() < -10.0);
        WardRules.Point south = WardRules.exitPoint(b, 0.5, 10.4);
        assertTrue(south.z() >= 11.0);
    }

    @Test
    void theInviteListHasACapAndNoDuplicates() {
        Set<UUID> list = new HashSet<>();
        UUID a = UUID.randomUUID();
        assertEquals(WardRules.InviteResult.ADDED, WardRules.invite(list, a, 2));
        assertEquals(WardRules.InviteResult.ALREADY, WardRules.invite(list, a, 2));
        assertEquals(WardRules.InviteResult.ADDED, WardRules.invite(list, UUID.randomUUID(), 2));
        assertEquals(WardRules.InviteResult.FULL, WardRules.invite(list, UUID.randomUUID(), 2));
        assertEquals(2, list.size());
        assertEquals(WardRules.InviteResult.ALREADY, WardRules.invite(list, a, 2), "a full list still knows who is on it");
    }

    @Test
    void theRadiusIsClamped() {
        assertEquals(1, WardRules.clampRadius(-5));
        assertEquals(10, WardRules.clampRadius(10));
        assertEquals(24, WardRules.clampRadius(24));
        assertEquals(24, WardRules.clampRadius(99));
    }
}
