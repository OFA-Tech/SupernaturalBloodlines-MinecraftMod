package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.ContestRow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.junit.jupiter.api.Test;

/** Plan 20c: the Charter and the contest are rows of the group index; a type switch deletes neither, a dissolved or merged group takes both. */
class GroupIndexClanTest {
    private static final UUID A = new UUID(9, 1);
    private static final UUID B = new UUID(9, 2);
    private static final UUID M = new UUID(9, 3);

    private static GroupIndex roundTrip(GroupIndex i) {
        return GroupIndex.load(i.save(new CompoundTag(), null), null);
    }

    private static CharterRow charter() {
        return new CharterRow(new UUID(1, 1), Optional.of(M), CharterState.HELD, 100, 200, Optional.of(new UUID(5, 5)), 300);
    }

    private static ContestRow contest() {
        return new ContestRow(M, ContestRow.Phase.RUNNING, 10, 20, 30, 17_990);
    }

    @Test
    void charterRoundTrips() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setCharter(group, charter());
        assertEquals(Optional.of(charter()), roundTrip(g).charterOf(group));
        CharterRow bare = new CharterRow(new UUID(2, 2), Optional.empty(), CharterState.LOST, 1, 2, Optional.empty(), 3);
        g.setCharter(group, bare);
        assertEquals(Optional.of(bare), roundTrip(g).charterOf(group));
        g.clearCharter(group);
        assertEquals(Optional.empty(), roundTrip(g).charterOf(group));
    }

    @Test
    void contestRoundTrips() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setContest(group, contest());
        assertEquals(Optional.of(contest()), roundTrip(g).contestOf(group));
        ContestRow pending = new ContestRow(M, ContestRow.Phase.PENDING, 10, 20, 0, 0);
        g.setContest(group, pending);
        assertEquals(Optional.of(pending), roundTrip(g).contestOf(group));
        g.clearContest(group);
        assertEquals(Optional.empty(), roundTrip(g).contestOf(group));
    }

    @Test
    void aDamagedCharterRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        UUID first = g.groupOf(A);
        UUID second = g.groupOf(B);
        g.setCharter(first, charter());
        g.setCharter(second, charter());
        CompoundTag saved = g.save(new CompoundTag(), null);
        ListTag rows = saved.getList("charters", 10);
        rows.getCompound(0).putString("id", "not a uuid");
        rows.getCompound(0).remove("state");
        GroupIndex back = GroupIndex.load(saved, null);
        assertEquals(1, back.charters().size());
    }

    @Test
    void aDamagedContestRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setContest(g.groupOf(A), contest());
        g.setContest(g.groupOf(B), contest());
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.getList("contests", 10).getCompound(0).put("challenger", StringTag.valueOf("junk"));
        assertEquals(1, GroupIndex.load(saved, null).contests().size());
    }

    @Test
    void unknownKeysStillRoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setCharter(g.groupOf(A), charter());
        CompoundTag saved = g.save(new CompoundTag(), null);
        saved.putString("plan_20e_key", "kept");
        CompoundTag again = GroupIndex.load(saved, null).save(new CompoundTag(), null);
        assertEquals("kept", again.getString("plan_20e_key"));
        assertEquals(1, again.getList("charters", 10).size());
    }

    @Test
    void aTwentyBJarKeepsTheNewRowsAsUnknownKeys() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setCharter(group, charter());
        g.setContest(group, contest());
        CompoundTag written = g.save(new CompoundTag(), null);
        Set<String> twentyBKnown = Set.of("data_version", "ids", "types", "type_since", "ranks", "channel", "consent", "votes", "bans", "cooldowns");
        CompoundTag extras = new CompoundTag();
        for (String key : written.getAllKeys()) {
            if (!twentyBKnown.contains(key)) {
                extras.put(key, written.get(key).copy());
            }
        }
        assertTrue(extras.contains("charters") && extras.contains("contests"), "a 20b loader would not keep both rows: " + extras.getAllKeys());
        assertTrue(GroupIndex.knownKeys().containsAll(Set.of("charters", "contests")), "this build must know its own rows");
        CompoundTag rewritten = new CompoundTag();
        for (String key : extras.getAllKeys()) {
            rewritten.put(key, extras.get(key).copy());
        }
        rewritten.put("ids", written.get("ids").copy());
        GroupIndex back = GroupIndex.load(rewritten, null);
        assertEquals(Optional.of(charter()), back.charterOf(group));
        assertEquals(Optional.of(contest()), back.contestOf(group));
    }

    @Test
    void aTypeSwitchKeepsBothRows() {
        GroupIndex g = new GroupIndex();
        UUID group = g.ensure(A, GroupType.CLAN, 1);
        g.setCharter(group, charter());
        g.setContest(group, contest());
        g.setType(group, GroupType.COVEN, 5);
        g.setType(group, GroupType.PACK, 6);
        assertTrue(g.charterOf(group).isPresent() && g.contestOf(group).isPresent());
        GroupIndex back = roundTrip(g);
        assertTrue(back.charterOf(group).isPresent() && back.contestOf(group).isPresent());
    }

    @Test
    void aDissolvedGroupDropsBothRows() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setCharter(group, charter());
        g.setContest(group, contest());
        g.drop(A);
        assertTrue(g.charterOf(group).isEmpty() && g.contestOf(group).isEmpty());
        GroupIndex back = roundTrip(g);
        assertTrue(back.charters().isEmpty() && back.contests().isEmpty());
    }

    @Test
    void aMergedGroupDropsBothRows() {
        GroupIndex g = new GroupIndex();
        UUID merging = g.groupOf(A);
        UUID survivor = g.groupOf(B);
        g.setCharter(merging, charter());
        g.setContest(merging, contest());
        g.setCharter(survivor, new CharterRow(new UUID(3, 3), Optional.empty(), CharterState.STORED, 1, 1, Optional.empty(), 0));
        g.follow(A, B);
        assertTrue(g.charterOf(merging).isEmpty() && g.contestOf(merging).isEmpty());
        assertEquals(new UUID(3, 3), g.charterOf(survivor).orElseThrow().id(), "the surviving group keeps its own Charter");
    }

    @Test
    void theLeadMovingKeepsBothRows() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setCharter(group, charter());
        g.setContest(group, contest());
        g.follow(A, B);
        assertEquals(group, g.existingGroupOf(B).orElseThrow());
        assertTrue(g.charterOf(group).isPresent() && g.contestOf(group).isPresent());
    }

    @Test
    void theNoPromoteMarkAndTheExileBanUseTheExistingRows() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.setCooldown(group, "nodw:" + M, Long.MAX_VALUE);
        g.setCooldown(group, "contest", 5_000);
        g.setCooldown(group, "reforge", 6_000);
        g.ban(group.toString(), M, 9_000);
        GroupIndex back = roundTrip(g);
        assertEquals(Long.MAX_VALUE, back.cooldownUntil(group, "nodw:" + M));
        assertEquals(5_000, back.cooldownUntil(group, "contest"));
        assertEquals(6_000, back.cooldownUntil(group, "reforge"));
        assertEquals(9_000, back.banUntil(group.toString(), M));
        assertEquals(0, back.banUntil(UUID.randomUUID().toString(), M), "a ban belongs to its clan only");
    }

    @Test
    void aDissolvedClanForgetsItsExilesAndAMarkCanBeCleared() {
        GroupIndex g = new GroupIndex();
        UUID group = g.groupOf(A);
        g.ban(group.toString(), M, 9_000);
        g.setCooldown(group, "nodw:" + M, Long.MAX_VALUE);
        g.clearCooldown(group, "nodw:" + M);
        assertEquals(0, g.cooldownUntil(group, "nodw:" + M));
        g.clearCooldown(group, "never-set");
        g.drop(A);
        assertEquals(0, g.banUntil(group.toString(), M), "an exile ends with the clan");
    }

    @Test
    void pruneDoesNotDeleteADormantCharter() {
        GroupIndex g = new GroupIndex();
        UUID group = g.ensure(A, GroupType.PACK, 1);
        g.setCharter(group, charter());
        g.setContest(group, contest());
        g.prune(Long.MAX_VALUE - 1, 1);
        g.prune(new PackIndex());
        assertTrue(g.charterOf(group).isPresent() && g.contestOf(group).isPresent());
    }

    @Test
    void aRowNamingAGroupThatDoesNotExistIsDroppedOnLoadWithOneWarning() {
        GroupIndex g = new GroupIndex();
        UUID real = g.groupOf(A);
        g.setCharter(real, charter());
        g.setCharter(new UUID(8, 8), charter());
        g.setContest(new UUID(8, 9), contest());
        GroupIndex back = roundTrip(g);
        assertEquals(1, back.charters().size());
        assertTrue(back.charters().containsKey(real));
        assertFalse(back.contests().containsKey(new UUID(8, 9)));
        assertTrue(back.contests().isEmpty());
    }
}
