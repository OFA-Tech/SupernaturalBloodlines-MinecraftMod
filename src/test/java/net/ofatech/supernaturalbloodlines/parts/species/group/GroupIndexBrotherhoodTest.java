package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.junit.jupiter.api.Test;

/** Plan 20d: the marks, the curse ends and the Code-breakers are player rows of the group index; a type switch, a dissolved group and a lead change keep them. */
class GroupIndexBrotherhoodTest {
    private static final UUID L = new UUID(8, 1);
    private static final UUID A = new UUID(8, 2);
    private static final UUID B = new UUID(8, 3);
    private static final UUID LONE = new UUID(8, 4);
    private static final long DAY = 24L * 3_600_000L;

    private static GroupIndex roundTrip(GroupIndex i) {
        return GroupIndex.load(i.save(new CompoundTag(), null), null);
    }

    private static BreakerRow breaker() {
        return new BreakerRow(5_000, List.of(100L, 200L, 300L));
    }

    @Test
    void marksRoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setMark(A, 7);
        g.setMark(B, 0);
        GroupIndex back = roundTrip(g);
        assertEquals(OptionalInt.of(7), back.markOf(A));
        assertEquals(OptionalInt.of(0), back.markOf(B));
        assertEquals(OptionalInt.empty(), back.markOf(L));
        g.clearMark(A);
        assertEquals(OptionalInt.empty(), roundTrip(g).markOf(A));
        assertEquals(1, roundTrip(g).marks().size());
    }

    @Test
    void cursesRoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setCurse(A, 1_700_000_000_123L);
        assertEquals(OptionalLong.of(1_700_000_000_123L), roundTrip(g).curseUntil(A));
        assertEquals(OptionalLong.empty(), roundTrip(g).curseUntil(B));
        g.clearCurse(A);
        assertTrue(roundTrip(g).curses().isEmpty());
    }

    @Test
    void breakersRoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setBreaker(A, breaker());
        assertEquals(Optional.of(breaker()), roundTrip(g).breakerOf(A));
        BreakerRow bare = new BreakerRow(9, List.of());
        g.setBreaker(B, bare);
        assertEquals(Optional.of(bare), roundTrip(g).breakerOf(B));
        g.clearBreaker(A);
        assertEquals(Optional.empty(), roundTrip(g).breakerOf(A));
    }

    @Test
    void aDamagedMarkRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setMark(A, 3);
        g.setMark(B, 4);
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.getList("marks", 10).getCompound(0).put("key", StringTag.valueOf("junk"));
        assertEquals(1, GroupIndex.load(saved, null).marks().size());
    }

    @Test
    void aDamagedCurseRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setCurse(A, 3);
        g.setCurse(B, 4);
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.getList("curses", 10).getCompound(0).put("key", StringTag.valueOf("junk"));
        assertEquals(1, GroupIndex.load(saved, null).curses().size());
    }

    @Test
    void aDamagedBreakerRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setBreaker(A, breaker());
        g.setBreaker(B, breaker());
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.getList("code_breakers", 10).getCompound(0).put("strikes", StringTag.valueOf("junk"));
        saved.getList("code_breakers", 10).getCompound(0).put("key", StringTag.valueOf("junk"));
        assertEquals(1, GroupIndex.load(saved, null).breakers().size());
    }

    @Test
    void unknownKeysStillRoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setMark(A, 2);
        g.setCurse(A, 5);
        g.setBreaker(A, breaker());
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.putString("plan_20e_key", "kept");
        CompoundTag again = GroupIndex.load(saved, null).save(new CompoundTag(), null);
        assertEquals("kept", again.getString("plan_20e_key"));
        assertEquals(1, again.getList("marks", 10).size());
        assertEquals(1, again.getList("curses", 10).size());
        assertEquals(1, again.getList("code_breakers", 10).size());
    }

    @Test
    void aTwentyCJarKeepsTheNewRowsAsUnknownKeys() {
        GroupIndex g = new GroupIndex();
        g.setMark(A, 2);
        g.setCurse(A, 5);
        g.setBreaker(A, breaker());
        CompoundTag written = g.save(new CompoundTag(), null);
        Set<String> twentyCKnown = Set.of("data_version", "ids", "types", "type_since", "ranks", "channel", "consent", "votes", "bans", "cooldowns", "charters", "contests");
        CompoundTag extras = new CompoundTag();
        for (String key : written.getAllKeys()) {
            if (!twentyCKnown.contains(key)) {
                extras.put(key, written.get(key).copy());
            }
        }
        assertEquals(Set.of("marks", "curses", "code_breakers"), extras.getAllKeys());
        CompoundTag asOldJarWouldRead = new CompoundTag();
        for (String key : written.getAllKeys()) {
            asOldJarWouldRead.put(key, written.get(key).copy());
        }
        CompoundTag rewritten = GroupIndex.load(asOldJarWouldRead, null).save(new CompoundTag(), null);
        for (String key : extras.getAllKeys()) {
            assertEquals(extras.get(key), rewritten.get(key), key);
        }
        assertTrue(GroupIndex.knownKeys().containsAll(Set.of("marks", "curses", "code_breakers")), "this build must know its own rows");
    }

    @Test
    void aTypeSwitchKeepsAllThree() {
        GroupIndex g = new GroupIndex();
        UUID group = g.ensure(L, GroupType.BROTHERHOOD, 1);
        g.setMark(A, 5);
        g.setCurse(A, 77);
        g.setBreaker(A, breaker());
        g.setType(group, GroupType.COVEN, 2);
        assertEquals(OptionalInt.of(5), g.markOf(A));
        assertEquals(OptionalLong.of(77), g.curseUntil(A));
        assertEquals(Optional.of(breaker()), g.breakerOf(A));
    }

    @Test
    void aDissolvedGroupKeepsThePlayerRows() {
        GroupIndex g = new GroupIndex();
        g.ensure(L, GroupType.BROTHERHOOD, 1);
        g.setMark(A, 5);
        g.setCurse(A, 77);
        g.setBreaker(A, breaker());
        g.drop(L);
        assertEquals(OptionalInt.of(5), g.markOf(A));
        assertEquals(OptionalLong.of(77), g.curseUntil(A));
        assertEquals(Optional.of(breaker()), g.breakerOf(A));
    }

    @Test
    void aLeadMovingKeepsThePlayerRows() {
        GroupIndex g = new GroupIndex();
        g.ensure(L, GroupType.BROTHERHOOD, 1);
        g.setMark(A, 5);
        g.setCurse(B, 77);
        g.setBreaker(A, breaker());
        g.follow(L, A);
        assertEquals(OptionalInt.of(5), g.markOf(A));
        assertEquals(OptionalLong.of(77), g.curseUntil(B));
        assertEquals(Optional.of(breaker()), g.breakerOf(A));
    }

    @Test
    void pruneDropsAMarkOfSomeoneInNoGroup() {
        PackIndex packs = new PackIndex();
        packs.join(L, A);
        GroupIndex g = new GroupIndex();
        g.setMark(A, 5);
        g.setMark(L, 3);
        g.setMark(LONE, 9);
        g.prune(packs);
        assertEquals(OptionalInt.of(5), g.markOf(A));
        assertEquals(OptionalInt.of(3), g.markOf(L), "a leader with members is in a group");
        assertEquals(OptionalInt.empty(), g.markOf(LONE));
    }

    @Test
    void pruneKeepsAMarkOfADormantType() {
        PackIndex packs = new PackIndex();
        packs.join(L, A);
        GroupIndex g = new GroupIndex();
        UUID group = g.ensure(L, GroupType.BROTHERHOOD, 1);
        g.setMark(A, 5);
        g.setType(group, GroupType.COVEN, 2);
        g.prune(packs);
        assertEquals(OptionalInt.of(5), g.markOf(A));
    }

    @Test
    void pruneCodeDropsOldStrikesAndEndedBreakers() {
        GroupIndex g = new GroupIndex();
        long now = 20 * DAY;
        g.setBreaker(A, new BreakerRow(now - 1, List.of(now - 8 * DAY, now - 7 * DAY, now - 6 * DAY)));
        g.setBreaker(B, new BreakerRow(now - 5, List.of(now - 9 * DAY)));
        g.pruneCode(now, 7 * DAY);
        assertEquals(Optional.of(new BreakerRow(now - 1, List.of(now - 6 * DAY))), g.breakerOf(A), "the strike at exactly the window leaves too");
        assertEquals(Optional.empty(), g.breakerOf(B), "an ended breaker with no strikes is gone");
    }

    @Test
    void pruneCodeKeepsAnActiveBreakerWithNoStrikes() {
        GroupIndex g = new GroupIndex();
        long now = 20 * DAY;
        g.setBreaker(A, new BreakerRow(now + 1, List.of(now - 30 * DAY)));
        g.pruneCode(now, 7 * DAY);
        assertEquals(Optional.of(new BreakerRow(now + 1, List.of())), g.breakerOf(A));
        g.pruneCode(now + 1, 7 * DAY);
        assertEquals(Optional.empty(), g.breakerOf(A));
    }

    @Test
    void theExpulsionBarUsesTheExistingBansRow() {
        GroupIndex g = new GroupIndex();
        UUID group = g.ensure(L, GroupType.BROTHERHOOD, 1);
        g.ban(group.toString(), A, 12_345);
        assertEquals(12_345, roundTrip(g).banUntil(group.toString(), A));
        assertFalse(roundTrip(g).banUntil(group.toString(), B) > 0);
    }

    @Test
    void aClockThatWentBackKeepsAStrikeFromTheFuture() {
        GroupIndex g = new GroupIndex();
        g.setBreaker(A, new BreakerRow(5, List.of(1_000L)));
        g.pruneCode(500, 7 * DAY);
        assertEquals(Optional.of(new BreakerRow(5, List.of(1_000L))), g.breakerOf(A));
    }

    @Test
    void theRowsOfAListAreNotShared() {
        GroupIndex g = new GroupIndex();
        List<Long> strikes = new java.util.ArrayList<>(List.of(1L));
        g.setBreaker(A, new BreakerRow(5, strikes));
        strikes.add(2L);
        assertEquals(List.of(1L), g.breakerOf(A).orElseThrow().strikes());
    }
}
