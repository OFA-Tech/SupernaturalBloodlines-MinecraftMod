package net.ofatech.supernaturalbloodlines.core.ward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class WardTableTest {
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CARA = UUID.randomUUID();

    private static Supplier<String> ids(String... values) {
        Deque<String> q = new ArrayDeque<>(java.util.List.of(values));
        return q::poll;
    }

    private static WardTable.Ward area(WardTable t, UUID owner, Supplier<String> ids) {
        return t.add(WardRules.Kind.AREA, "minecraft:overworld", 0, 64, 0, 10, owner, 3, ids).orElseThrow();
    }

    @Test
    void anIdIsEightHexDigitsAndATakenOneIsAskedAgain() {
        WardTable t = new WardTable();
        WardTable.Ward a = area(t, ALICE, ids("aaaaaaaa"));
        WardTable.Ward b = area(t, BOB, ids("aaaaaaaa", "bbbbbbbb"));
        assertEquals("aaaaaaaa", a.id());
        assertEquals("bbbbbbbb", b.id());
        assertTrue(a.id().matches("[0-9a-f]{8}"));
    }

    @Test
    void theOwnerIsOnTheListFromTheStart() {
        WardTable t = new WardTable();
        assertTrue(area(t, ALICE, ids("00000001")).invited().contains(ALICE));
    }

    @Test
    void theCapCountsStandingWardsOnly() {
        WardTable t = new WardTable();
        area(t, ALICE, ids("00000001"));
        area(t, ALICE, ids("00000002"));
        area(t, ALICE, ids("00000003"));
        assertEquals(Optional.empty(), t.add(WardRules.Kind.DOOR, "d", 0, 0, 0, 1, ALICE, 3, ids("00000004")));
        assertEquals(3, t.fallAllOf(ALICE), "all three of Alice's fell");
        assertEquals(0, t.countStanding(ALICE));
        assertTrue(t.add(WardRules.Kind.DOOR, "d", 0, 0, 0, 1, ALICE, 3, ids("00000005")).isPresent(), "a fallen ward does not count against the cap");
    }

    @Test
    void onlyTheOwnerDropsAWard() {
        WardTable t = new WardTable();
        WardTable.Ward w = area(t, ALICE, ids("00000001"));
        assertFalse(t.drop(w.id(), BOB));
        assertTrue(t.get(w.id()).isPresent());
        assertFalse(t.drop("ffffffff", ALICE), "no such ward is the same answer");
        assertTrue(t.drop(w.id(), ALICE));
        assertTrue(t.get(w.id()).isEmpty());
    }

    @Test
    void invitingGoesIntoEveryStandingWardOfTheOwner() {
        WardTable t = new WardTable();
        WardTable.Ward one = area(t, ALICE, ids("00000001"));
        WardTable.Ward two = area(t, ALICE, ids("00000002"));
        WardTable.Ward other = area(t, BOB, ids("00000003"));
        assertEquals(Optional.of(WardRules.InviteResult.ADDED), t.inviteAll(ALICE, CARA, 16));
        assertTrue(one.invited().contains(CARA) && two.invited().contains(CARA));
        assertFalse(other.invited().contains(CARA), "another owner's ward is not touched");
        assertEquals(Optional.of(WardRules.InviteResult.ALREADY), t.inviteAll(ALICE, CARA, 16));
        assertEquals(Optional.empty(), t.inviteAll(CARA, BOB, 16), "no ward of their own");
    }

    @Test
    void aFullListAnswersFull() {
        WardTable t = new WardTable();
        area(t, ALICE, ids("00000001"));
        assertEquals(Optional.of(WardRules.InviteResult.FULL), t.inviteAll(ALICE, BOB, 1), "only the owner fits");
    }

    @Test
    void withdrawingTakesThemOutOfEveryWardAndNeverTheOwner() {
        WardTable t = new WardTable();
        WardTable.Ward one = area(t, ALICE, ids("00000001"));
        WardTable.Ward two = area(t, ALICE, ids("00000002"));
        t.inviteAll(ALICE, BOB, 16);
        assertTrue(t.uninviteAll(ALICE, BOB));
        assertFalse(one.invited().contains(BOB) || two.invited().contains(BOB));
        assertFalse(t.uninviteAll(ALICE, BOB), "nothing left to take out");
        assertFalse(t.uninviteAll(ALICE, ALICE));
        assertTrue(one.invited().contains(ALICE));
    }

    @Test
    void theWelcomeListHoldsOtherPeoplesStandingWardsThatInviteYou() {
        WardTable t = new WardTable();
        WardTable.Ward a = area(t, ALICE, ids("00000001"));
        area(t, BOB, ids("00000002"));
        t.inviteAll(ALICE, CARA, 16);
        assertEquals(1, t.welcoming(CARA).size());
        assertEquals(a.id(), t.welcoming(CARA).get(0).id());
        assertEquals(0, t.welcoming(ALICE).size(), "your own ward does not welcome you");
        t.fallAllOf(ALICE);
        assertEquals(0, t.welcoming(CARA).size(), "a fallen ward welcomes nobody");
    }

    @Test
    void fallingIsPerOwnerAndIdempotent() {
        WardTable t = new WardTable();
        WardTable.Ward a1 = area(t, ALICE, ids("00000001"));
        WardTable.Ward a2 = area(t, ALICE, ids("00000002"));
        WardTable.Ward b = area(t, BOB, ids("00000003"));
        assertEquals(2, t.fallAllOf(ALICE));
        assertEquals(0, t.fallAllOf(ALICE));
        assertTrue(a1.fallen() && a2.fallen());
        assertFalse(b.fallen());
        assertEquals(0, t.standingOf(ALICE).size());
        assertEquals(1, t.standingOf(BOB).size());
        assertEquals(2, t.ownedBy(ALICE).size(), "a fallen ward stays until its owner drops it");
    }
}
