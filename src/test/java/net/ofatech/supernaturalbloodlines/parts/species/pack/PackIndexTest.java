package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The index's own rules, without a world. */
class PackIndexTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID M = new UUID(0, 4);

    @Test
    void anAllianceIsMadeBothWaysOnceAndNeverWithYourself() {
        PackIndex i = new PackIndex();
        assertFalse(i.ally(A, A), "allied with themselves");
        assertTrue(i.ally(A, B));
        assertFalse(i.ally(A, B), "the same alliance twice");
        assertFalse(i.ally(B, A), "the same alliance from the other side");
        assertTrue(i.alliesOf(A).contains(B) && i.alliesOf(B).contains(A), "not both ways");
        assertTrue(i.unally(B, A));
        assertTrue(i.alliesOf(A).isEmpty() && i.alliesOf(B).isEmpty());
    }

    @Test
    void onlyALeaderAllies() {
        PackIndex i = new PackIndex();
        i.join(A, M);
        assertFalse(i.ally(M, B), "a member allied");
        assertFalse(i.ally(B, M), "allied with a member");
        assertTrue(i.ally(A, B));
        assertTrue(i.alliedPlayers(M, B) && i.alliedPlayers(B, M), "a member is covered by their pack's alliance");
        assertFalse(i.alliedPlayers(A, M), "the same pack is not allied with itself");
    }

    @Test
    void anEmissaryWhoLeavesOrIsKickedIsNobodysEmissaryAndAJoiningLeaderLosesTheirOwn() {
        PackIndex i = new PackIndex();
        UUID witch = new UUID(0, 9);
        i.join(A, witch);
        i.setEmissary(A, witch);
        assertTrue(i.kick(witch));
        assertTrue(i.emissaryOf(A).isEmpty() && i.emissaryAlpha(witch).isEmpty(), "a kicked Emissary is still named");

        i.join(A, witch);
        i.setEmissary(A, witch);
        i.leave(witch);
        assertTrue(i.emissaryOf(A).isEmpty(), "an Emissary who left is still named");

        // a leader who joins another pack no longer has an Emissary of their own
        i.join(B, witch);
        i.setEmissary(B, witch);
        assertTrue(i.join(A, B));
        assertTrue(i.emissaryOf(B).isEmpty(), "a joined leader kept their Emissary");
    }

    @Test
    void noticesAreAShortListPerPlayerKeptInOrderAndSurviveASave() {
        PackIndex i = new PackIndex();
        for (int n = 0; n < PackIndex.MAX_NOTICES + 3; n++) {
            i.addNotice(A, "k", Integer.toString(n));
        }
        i.addNotice(B, "other", "x");
        assertEquals(PackIndex.MAX_NOTICES, i.noticeCount(A), "the list is not capped");
        PackIndex back = PackIndex.load(i.save(new net.minecraft.nbt.CompoundTag(), null), null);
        List<String[]> got = back.takeNotices(A);
        assertEquals(PackIndex.MAX_NOTICES, got.size());
        assertEquals("3", got.get(0)[1], "the oldest were not the ones dropped");
        assertEquals("10", got.get(got.size() - 1)[1]);
        assertEquals(1, back.takeNotices(B).size());
        assertTrue(back.takeNotices(A).isEmpty(), "a notice came twice");
    }

    @Test
    void aDamagedEntryIsSkippedAndTheRestOfTheIndexLoads() {
        PackIndex i = new PackIndex();
        i.join(A, M);
        i.setStrength(A, 1005);
        i.addNotice(M, "k", "v");
        net.minecraft.nbt.CompoundTag tag = i.save(new net.minecraft.nbt.CompoundTag(), null);
        // two damaged entries in the middle of good ones: a list entry that is no compound with ids, and an id that is no uuid
        net.minecraft.nbt.ListTag members = tag.getList("members", net.minecraft.nbt.Tag.TAG_COMPOUND);
        members.add(0, new net.minecraft.nbt.CompoundTag());
        tag.put("members", members);
        net.minecraft.nbt.ListTag former = new net.minecraft.nbt.ListTag();
        former.add(new net.minecraft.nbt.IntArrayTag(new int[] {1, 2}));
        tag.put("former_alphas", former);
        net.minecraft.nbt.ListTag notices = tag.getList("notices", net.minecraft.nbt.Tag.TAG_COMPOUND);
        notices.add(0, new net.minecraft.nbt.CompoundTag());
        tag.put("notices", notices);

        PackIndex back = PackIndex.load(tag, null);
        assertEquals(A, back.leaderOf(M), "the good member was lost with the bad entry");
        assertEquals(1005, back.strengthOf(A));
        assertEquals(1, back.takeNotices(M).size(), "the good notice was lost with the bad one");
    }

    @Test
    void samePackNeedsTwoPlayersInOnePack() {
        PackIndex i = new PackIndex();
        assertFalse(i.samePack(A, A));
        assertFalse(i.samePack(A, B), "two strangers");
        i.join(A, M);
        assertTrue(i.samePack(A, M) && i.samePack(M, A));
        assertFalse(i.samePack(M, M));
        assertFalse(i.samePack(M, B));
        assertTrue(i.isLeader(A) && !i.isLeader(M) && i.isLeader(B));
    }

    @Test
    void aLeaderWhoJoinsBringsTheirMembersAndTheirAlliesGoWithTheLeadership() {
        PackIndex i = new PackIndex();
        i.join(B, M);
        i.ally(B, C);
        assertTrue(i.join(A, B));
        assertEquals(List.of(M, B), i.betasOf(A), "the members come along, the leader after them");
        assertTrue(i.alliesOf(B).isEmpty(), "a member has no alliance of their own");
        // the leadership moves to B: the alliance of the pack goes with it
        i.ally(A, C);
        i.promote(A, B);
        assertTrue(i.alliesOf(B).contains(C) && i.alliesOf(A).isEmpty());
        assertTrue(i.alliedPlayers(A, C), "the old leader is still in the allied pack");
        assertEquals(B, i.leaderOf(A), "the old leader does not follow the new one");
        assertEquals(B, i.leaderOf(M), "a member does not follow the new leader");
        assertEquals(B, i.leaderOf(B));
        assertTrue(i.isLeader(B) && !i.isLeader(A) && !i.isLeader(M));
        assertEquals(java.util.Set.of(M, A), new java.util.HashSet<>(i.betasOf(B)));
    }

    @Test
    void aServerStartBeginsEveryRecordedTimeAgain() {
        PackIndex i = new PackIndex();
        i.setLastSeen(A, 100);
        i.setLastSeen(B, 200);
        i.resetLastSeen(9_000);
        assertEquals(9_000L, i.lastSeen(A).orElseThrow());
        assertEquals(9_000L, i.lastSeen(B).orElseThrow());
        assertTrue(i.lastSeen(C).isEmpty(), "somebody the index never saw got a time");
    }

    @Test
    void theNoticeThatSaysYouLostTheLeadIsNeverDroppedForRoom() {
        PackIndex i = new PackIndex();
        i.addNotice(A, PackIndex.KEPT_NOTICE, "x");
        for (int n = 0; n < PackIndex.MAX_NOTICES + 5; n++) {
            i.addNotice(A, "k", Integer.toString(n));
        }
        List<String[]> got = i.takeNotices(A);
        assertEquals(PackIndex.MAX_NOTICES, got.size());
        assertEquals(PackIndex.KEPT_NOTICE, got.get(0)[0], "the absence notice was dropped to make room");
        assertEquals("12", got.get(got.size() - 1)[1], "the newest one is gone");
    }

    @Test
    void someoneWhoLeftAMomentAgoStillCountsForAKillUntilTheGraceEnds() {
        long[] clock = {5_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            PackIndex i = new PackIndex();
            i.join(A, M);
            i.leave(M);
            assertFalse(i.samePack(A, M), "they are not in it any more");
            assertTrue(i.killsInPack(M, A, 10) && i.killsInPack(A, M, 10), "leaving a moment ago must not free a kill: a lone leader matches who left them");
            assertFalse(i.killsInPack(M, A, 0), "the switch at 0 is off");
            assertFalse(i.killsInPack(M, B, 10), "a stranger is no member");
            assertFalse(i.killsInPack(M, M, 10), "nobody is their own pack mate");
            clock[0] += 10 * 60_000L - 1;
            assertTrue(i.killsInPack(M, A, 10), "ten minutes minus a millisecond");
            clock[0] += 1;
            assertFalse(i.killsInPack(M, A, 10), "ten minutes");
            // kicked counts like leaving, and joining again clears it
            clock[0] = 5_000_000L;
            i.join(A, M);
            i.kick(M);
            assertTrue(i.killsInPack(A, M, 10), "a kick frees a kill too");
            i.join(B, M);
            assertFalse(i.killsInPack(M, A, 10), "somebody who joined another pack is no member of the old one");
            assertTrue(i.killsInPack(M, B, 10));
        } finally {
            PackIndex.overrideClock(null);
        }
    }
}
