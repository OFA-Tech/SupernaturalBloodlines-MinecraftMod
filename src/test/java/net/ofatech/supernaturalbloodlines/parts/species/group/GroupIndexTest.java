package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The group index's own rules, without a world. */
class GroupIndexTest {
    private static final UUID L1 = new UUID(0, 1);
    private static final UUID L2 = new UUID(0, 2);
    private static final UUID M1 = new UUID(0, 11);
    private static final UUID M2 = new UUID(0, 12);
    private static final UUID M3 = new UUID(0, 13);

    @BeforeEach
    void pin() {
        PackIndex.overrideClock(() -> 5_000L);
    }

    @AfterEach
    void unpin() {
        PackIndex.overrideClock(null);
    }

    private static PackIndex twoPacks() {
        PackIndex p = new PackIndex();
        p.join(L1, M1);
        p.join(L1, M2);
        p.join(L2, M3);
        return p;
    }

    private static GroupIndex roundTrip(GroupIndex i) {
        return GroupIndex.load(i.save(new CompoundTag(), null), null);
    }

    @Test
    void seedMakesOnePackPerLeader() {
        GroupIndex g = new GroupIndex();
        g.seed(twoPacks());
        UUID a = g.existingGroupOf(L1).orElseThrow();
        UUID b = g.existingGroupOf(L2).orElseThrow();
        assertNotEquals(a, b);
        assertEquals(GroupType.PACK, g.typeOf(a));
        assertEquals(GroupType.PACK, g.typeOf(b));
        assertEquals(2, g.leaders().size());
    }

    @Test
    void seedNeverReadsAnOfflineLeader() {
        // seed takes only the index: leaders with no profile and no state still get a pack, nothing can flip a group
        GroupIndex g = new GroupIndex();
        g.seed(twoPacks());
        for (UUID leader : g.leaders()) {
            assertEquals(GroupType.PACK, g.typeOf(g.existingGroupOf(leader).orElseThrow()));
        }
    }

    @Test
    void seedRunsOnce() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        UUID late = new UUID(0, 3);
        p.join(late, new UUID(0, 14));
        g.seed(p);
        assertTrue(g.existingGroupOf(late).isEmpty(), "a second seed added a row");
        assertEquals(GroupIndex.DATA_VERSION, g.dataVersion());
        GroupIndex back = roundTrip(g);
        back.seed(p);
        assertTrue(back.existingGroupOf(late).isEmpty(), "a reloaded index seeded again");
        assertTrue(back.existingGroupOf(L1).isPresent());
    }

    /** One comparable line per row, so sets compare as sets and the join order of {@code members} is kept. */
    private static Map<String, List<String>> snapshot(CompoundTag tag) {
        Map<String, List<String>> out = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            Tag t = tag.get(key);
            if (t instanceof ListTag list) {
                List<String> rows = new ArrayList<>();
                list.forEach(r -> rows.add(r.toString()));
                if (!key.equals("members")) {
                    rows.sort(null);
                }
                out.put(key, rows);
            } else {
                out.put(key, List.of(String.valueOf(t)));
            }
        }
        return out;
    }

    @Test
    void packIndexFileIsUntouched() {
        PackIndex p = twoPacks();
        UUID witch = new UUID(0, 20);
        p.join(L2, witch);
        p.setEmissary(L2, witch);
        p.setAnchor(M1, M2);
        p.markFormerAlpha(M3);
        p.dissolve(new UUID(0, 30));
        p.setMirror(M1, L1);
        p.recordParty(L1, new UUID(0, 40));
        p.setPartyEnded(L2, true);
        p.addNotice(M1, "key", "arg");
        p.setSparks(L1, 3);
        p.setLastSeen(M1, 123L);
        p.setStrength(L1, 7);
        p.ally(L1, L2);
        p.setHybridLike(L1, true);
        p.setAliveAt(777L);
        CompoundTag first = p.save(new CompoundTag(), null);
        Map<String, List<String>> before = snapshot(first);
        for (String key : List.of("members", "emissaries", "anchors", "former_alphas", "party_mirrors", "party_ids", "party_ended", "notices", "sparks", "last_seen", "joined_at", "alive_at", "strengths", "allies")) {
            assertTrue(first.contains(key), key + " is missing from the fixture");
            assertFalse(before.get(key).isEmpty(), key + " is empty in the fixture");
        }

        PackIndex loaded = PackIndex.load(first, null);
        assertEquals(before, snapshot(loaded.save(new CompoundTag(), null)), "the pack index changed on a round trip");

        GroupIndex g = new GroupIndex();
        g.seed(loaded);
        g.save(new CompoundTag(), null);
        assertEquals(before, snapshot(loaded.save(new CompoundTag(), null)), "the group index wrote into the pack index");
    }

    @Test
    void idFollowsTheLeaderOnPromote() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        UUID id = g.groupOf(L1);
        p.promote(L1, M1);
        g.follow(L1, M1);
        assertEquals(id, g.existingGroupOf(M1).orElseThrow());
        assertTrue(g.existingGroupOf(L1).isEmpty());
    }

    @Test
    void idFollowsTheLeaderOnTransfer() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        UUID id = g.groupOf(L1);
        p.transfer(L1, M2);
        g.follow(L1, M2);
        assertEquals(id, g.existingGroupOf(M2).orElseThrow());
        assertTrue(g.existingGroupOf(L1).isEmpty());
    }

    @Test
    void aLeaderWhoJoinsAnotherGroupDropsTheirOwnId() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        UUID kept = g.groupOf(L2);
        p.join(L2, L1);
        g.follow(L1, L2);
        assertEquals(kept, g.existingGroupOf(L2).orElseThrow());
        assertTrue(g.existingGroupOf(L1).isEmpty());
    }

    @Test
    void aDissolvedGroupLeavesNoRows() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        UUID old = g.groupOf(L1);
        g.setType(old, GroupType.CLAN, 9L);
        g.setRank(M1, GroupRank.DAY_WALKER);
        p.dissolve(L1);
        g.drop(L1);
        g.prune(p);
        assertTrue(g.existingGroupOf(L1).isEmpty());
        assertEquals(GroupRank.NONE, g.rankOf(M1));
        assertEquals(GroupType.PACK, g.typeOf(old), "the type row of a dissolved group stayed");
        assertEquals(0L, g.typeSince(old));
        p.join(L1, M1);
        assertNotEquals(old, g.groupOf(L1), "a new group reused the old id");
        assertEquals(GroupRank.NONE, g.rankOf(M1), "a stale rank leaked into the new group");
    }

    @Test
    void ranksAreForgottenWhenAMemberLeaves() {
        PackIndex p = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(p);
        g.setRank(M1, GroupRank.ELDER);
        g.setRank(M2, GroupRank.ELDER);
        p.leave(M1);
        g.prune(p);
        assertEquals(GroupRank.NONE, g.rankOf(M1));
        assertEquals(GroupRank.ELDER, g.rankOf(M2));
        g.clearRanks(M2);
        assertEquals(GroupRank.NONE, g.rankOf(M2));
    }

    @Test
    void aDamagedRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setRank(M1, GroupRank.ELDER);
        g.setRank(M2, GroupRank.DAY_WALKER);
        CompoundTag tag = g.save(new CompoundTag(), null);
        ListTag ranks = tag.getList("ranks", Tag.TAG_COMPOUND);
        CompoundTag bad = new CompoundTag();
        bad.put("key", StringTag.valueOf("not a uuid"));
        bad.put("value", IntTag.valueOf(5));
        ranks.add(bad);
        GroupIndex back = GroupIndex.load(tag, null);
        assertEquals(GroupRank.ELDER, back.rankOf(M1));
        assertEquals(GroupRank.DAY_WALKER, back.rankOf(M2));
    }

    @Test
    void unknownKeysSurviveARoundTrip() {
        GroupIndex g = new GroupIndex();
        g.seed(twoPacks());
        CompoundTag tag = g.save(new CompoundTag(), null);
        ListTag votes = new ListTag();
        CompoundTag row = new CompoundTag();
        row.put("key", NbtUtils.createUUID(M1));
        row.putString("value", "yes");
        votes.add(row);
        tag.put("votes", votes);
        tag.putString("future", "x");
        CompoundTag again = GroupIndex.load(tag, null).save(new CompoundTag(), null);
        assertEquals(votes, again.getList("votes", Tag.TAG_COMPOUND));
        assertEquals("x", again.getString("future"));
    }

    @Test
    void aTypeOfAnotherBuildIsKeptDormant() {
        GroupIndex g = new GroupIndex();
        g.seed(twoPacks());
        UUID id = g.groupOf(L1);
        CompoundTag tag = g.save(new CompoundTag(), null);
        for (Tag t : tag.getList("types", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            if (NbtUtils.loadUUID(e.get("key")).equals(id)) {
                e.putString("value", "warband");
            }
        }
        GroupIndex back = GroupIndex.load(tag, null);
        assertEquals(GroupType.PACK, back.typeOf(id));
        boolean kept = false;
        for (Tag t : back.save(new CompoundTag(), null).getList("types", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            kept |= NbtUtils.loadUUID(e.get("key")).equals(id) && e.getString("value").equals("warband");
        }
        assertTrue(kept, "the unknown type was not written back");
    }

    @Test
    void aRankOfAnotherTypeIsHiddenNotDeleted() {
        GroupIndex g = new GroupIndex();
        g.setRank(M1, GroupRank.DAY_WALKER);
        assertEquals(GroupRank.DAY_WALKER, g.activeRankOf(M1, GroupType.CLAN));
        assertEquals(GroupRank.NONE, g.activeRankOf(M1, GroupType.COVEN));
        assertEquals(GroupRank.DAY_WALKER, g.activeRankOf(M1, GroupType.CLAN), "the row did not come back");
    }
}
