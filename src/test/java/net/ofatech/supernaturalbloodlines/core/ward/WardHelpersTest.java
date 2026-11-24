package net.ofatech.supernaturalbloodlines.core.ward;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The pure helpers behind the plan 21 review fixes: the expel's search, the claim box, the invite notice and the drop confirmation. */
class WardHelpersTest {
    private static final WardRules.Box BOX = new WardRules.Box(0, 64, 0, 3);

    @Test
    void exitPointsAreOutsideAndNearestFaceFirst() {
        List<WardRules.Point> out = WardRules.exitPoints(BOX, 2.5, 0.5, 1);
        assertEquals(4, out.size());
        assertEquals(new WardRules.Point(5, 0.5), out.get(0), "east is nearest from x 2.5");
        out.forEach(p -> assertFalse(BOX.contains(p.x(), 64, p.z()), "inside: " + p));
    }

    @Test
    void heightOffsetsAreHereThenDownThenUp() {
        assertArrayEquals(new int[] {0, -1, -2, -3, 1, 2}, WardRules.heightOffsets(3, 2));
    }

    @Test
    void perimeterRingsTheBoxOneColumnEachAndStaysOutside() {
        List<WardRules.Point> ring = WardRules.perimeter(BOX, 1);
        // a 7 wide box grown by one: 9 columns on each z face and 7 more on each x face
        assertEquals(9 * 2 + 7 * 2, ring.size());
        assertEquals(ring.size(), new HashSet<>(ring).size(), "a column twice");
        ring.forEach(p -> assertFalse(BOX.contains(p.x(), 64, p.z()), "inside: " + p));
        assertTrue(ring.contains(new WardRules.Point(-3.5, -4)) && ring.contains(new WardRules.Point(5, 3.5)), "a face is missing");
    }

    @Test
    void chunksOfCoverTheWholeBox() {
        assertEquals(List.of(new WardRules.Chunk(-1, -1), new WardRules.Chunk(-1, 0), new WardRules.Chunk(0, -1), new WardRules.Chunk(0, 0)),
                WardRules.chunksOf(BOX), "a box on a chunk corner reaches four chunks");
        assertEquals(List.of(new WardRules.Chunk(0, 0)), WardRules.chunksOf(new WardRules.Box(8, 64, 8, 3)));
        assertEquals(16, WardRules.chunksOf(new WardRules.Box(8, 64, 8, WardRules.MAX_RADIUS)).size(), "radius 24 is at most 4 by 4 chunks");
    }

    @Test
    void theInviteNoticeIsOncePerPairPerCooldown() {
        PairCooldown c = new PairCooldown();
        UUID a = new UUID(1, 1);
        UUID b = new UUID(2, 2);
        assertTrue(c.tryUse(a, b, 100, WardRules.INVITE_NOTICE_TICKS));
        assertFalse(c.tryUse(a, b, 100 + WardRules.INVITE_NOTICE_TICKS - 1, WardRules.INVITE_NOTICE_TICKS));
        assertTrue(c.tryUse(b, a, 101, WardRules.INVITE_NOTICE_TICKS), "the other direction is its own pair");
        assertTrue(c.tryUse(a, b, 100 + WardRules.INVITE_NOTICE_TICKS, WardRules.INVITE_NOTICE_TICKS));
        assertTrue(c.tryUse(a, b, 5, WardRules.INVITE_NOTICE_TICKS), "a clock that went back frees the pair");
        c.clear();
        assertTrue(c.tryUse(a, b, 6, WardRules.INVITE_NOTICE_TICKS));
    }

    @Test
    void dropRunsOnlyOnTheSecondPressOfTheSameWard() {
        DropConfirm.Result first = DropConfirm.press("", "w1");
        assertFalse(first.run());
        assertEquals("w1", first.armed());
        DropConfirm.Result other = DropConfirm.press("w1", "w2");
        assertFalse(other.run(), "a press on another ward moves the arming");
        assertEquals("w2", other.armed());
        DropConfirm.Result second = DropConfirm.press("w2", "w2");
        assertTrue(second.run());
        assertEquals("", second.armed());
        assertFalse(DropConfirm.press("", "").run());
    }

    @Test
    void theOwnerCapFitsWhatTheHubShows() {
        assertTrue(WardRules.MAX_PER_OWNER_LIMIT <= net.ofatech.supernaturalbloodlines.shared.hub.WardHub.MAX_OWN, "a ward the hub cannot show cannot be dropped from it");
    }
}
