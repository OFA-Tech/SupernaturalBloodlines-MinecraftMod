package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.junit.jupiter.api.Test;

/** Spec 2 and R240/R241: the group index is its own file, seeded from the packs, and never touches the pack file. */
class GroupIndexTest {
    private static final UUID L1 = new UUID(1, 1);
    private static final UUID L2 = new UUID(1, 2);
    private static final UUID A = new UUID(2, 1);
    private static final UUID B = new UUID(2, 2);
    private static final UUID C = new UUID(2, 3);

    private static PackIndex twoPacks() {
        PackIndex p = new PackIndex();
        p.join(L1, A);
        p.join(L1, B);
        p.join(L2, C);
        return p;
    }

    private static GroupIndex roundTrip(GroupIndex i) {
        return GroupIndex.load(i.save(new CompoundTag(), null), null);
    }

    @Test
    void seedMakesOnePackPerLeader() {
        GroupIndex g = new GroupIndex();
        g.seed(twoPacks());
        assertEquals(Set.of(L1, L2), g.leaders());
        assertEquals(2, g.groupIds().size());
        for (UUID leader : g.leaders()) {
            assertEquals(GroupType.PACK, g.typeOf(g.existingGroupOf(leader).orElseThrow()));
        }
    }

    @Test
    void seedRunsOnce() {
        GroupIndex g = new GroupIndex();
        PackIndex packs = new PackIndex();
        packs.join(L1, A);
        g.seed(packs);
        packs.join(L2, C);
        g.seed(packs);
        assertEquals(Set.of(L1), g.leaders(), "a leader made after the seed gets a row from groupOf, not from seed");
        assertEquals(GroupIndex.DATA_VERSION, g.dataVersion());
        UUID later = g.groupOf(L2);
        assertEquals(later, g.existingGroupOf(L2).orElseThrow());
        assertEquals(GroupType.PACK, g.typeOf(later));
    }

    @Test
    void packIndexFileIsUntouched() {
        PackIndex packs = twoPacks();
        packs.ally(L1, L2);
        CompoundTag before = packs.save(new CompoundTag(), null);
        GroupIndex g = new GroupIndex();
        g.seed(packs);
        g.setRank(A, GroupRank.DAY_WALKER);
        g.setType(g.groupOf(L1), GroupType.CLAN, 5L);
        CompoundTag groupFile = g.save(new CompoundTag(), null);
        CompoundTag after = packs.save(new CompoundTag(), null);
        assertEquals(before, after, "building and saving a group index changed the pack file");
        for (String key : before.getAllKeys()) {
            assertFalse(groupFile.contains(key) && !key.equals("data_version"), "the group file writes the pack key " + key);
        }
        PackIndex back = PackIndex.load(after, null);
        assertEquals(packs.betasOf(L1), back.betasOf(L1));
        assertEquals(packs.alliesOf(L1), back.alliesOf(L1));
    }

    @Test
    void idFollowsTheLeaderOnPromote() {
        PackIndex packs = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(packs);
        UUID id = g.existingGroupOf(L1).orElseThrow();
        packs.promote(L1, A);
        g.follow(L1, A);
        assertEquals(id, g.existingGroupOf(A).orElseThrow());
        assertTrue(g.existingGroupOf(L1).isEmpty());
    }

    @Test
    void idFollowsTheLeaderOnTransfer() {
        GroupIndex g = new GroupIndex();
        UUID id = g.groupOf(L1);
        g.setType(id, GroupType.COVEN, 9L);
        g.follow(L1, B);
        assertEquals(id, g.existingGroupOf(B).orElseThrow());
        assertEquals(GroupType.COVEN, g.typeOf(id));
        assertEquals(9L, g.typeSince(id));
    }

    @Test
    void aDissolvedGroupLeavesNoRows() {
        PackIndex packs = twoPacks();
        GroupIndex g = new GroupIndex();
        g.seed(packs);
        UUID old = g.existingGroupOf(L1).orElseThrow();
        g.setRank(A, GroupRank.ELDER);
        g.setType(old, GroupType.COVEN, 3L);
        packs.dissolve(L1);
        g.drop(L1);
        g.prune(packs);
        assertTrue(g.existingGroupOf(L1).isEmpty());
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.COVEN), "a stale rank survived the dissolve");
        assertEquals(GroupType.PACK, g.typeOf(old), "the type row survived");
        assertEquals(0L, g.typeSince(old));
        assertNotEquals(old, g.groupOf(L1), "a new group got the old id");
    }

    @Test
    void ranksAreForgottenWhenAMemberLeaves() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        g.setRank(A, GroupRank.ELDER);
        g.setRank(B, GroupRank.ELDER);
        g.clearRanks(A);
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.CLAN));
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.COVEN));
        assertEquals(GroupRank.ELDER, g.rankOf(B, GroupType.COVEN));
    }

    @Test
    void eachTypeKeepsItsOwnRank() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        g.setRank(A, GroupRank.ELDER); // promoted in a coven while the clan row is dormant
        assertEquals(GroupRank.DAY_WALKER, g.rankOf(A, GroupType.CLAN), "a rank earned in the coven overwrote the clan's");
        assertEquals(GroupRank.ELDER, g.rankOf(A, GroupType.COVEN));
        GroupIndex back = roundTrip(g);
        assertEquals(GroupRank.DAY_WALKER, back.rankOf(A, GroupType.CLAN));
        assertEquals(GroupRank.ELDER, back.rankOf(A, GroupType.COVEN));
    }

    @Test
    void clearRankTouchesOnlyThatType() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        g.setRank(A, GroupRank.ELDER);
        g.clearRank(A, GroupType.COVEN);
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.COVEN));
        assertEquals(GroupRank.DAY_WALKER, g.rankOf(A, GroupType.CLAN), "clearing the coven rank took the clan's with it");
        assertThrows(IllegalArgumentException.class, () -> g.setRank(A, GroupRank.NONE));
    }

    @Test
    void aDamagedRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        g.setRank(B, GroupRank.ELDER);
        CompoundTag tag = g.save(new CompoundTag(), null);
        ListTag ranks = tag.getList("ranks", 10);
        CompoundTag noKey = new CompoundTag();
        noKey.putString("type", "coven");
        noKey.putString("value", "elder");
        CompoundTag badKey = new CompoundTag();
        badKey.putString("key", "not a uuid");
        badKey.putString("type", "coven");
        badKey.putString("value", "elder");
        CompoundTag noType = new CompoundTag();
        noType.put("key", NbtUtils.createUUID(C));
        noType.putString("value", "elder");
        ranks.add(noKey);
        ranks.add(badKey);
        ranks.add(noType);
        GroupIndex back = GroupIndex.load(tag, null);
        assertEquals(GroupRank.DAY_WALKER, back.rankOf(A, GroupType.CLAN));
        assertEquals(GroupRank.ELDER, back.rankOf(B, GroupType.COVEN));
        assertEquals(GroupRank.NONE, back.rankOf(C, GroupType.COVEN), "a rank row with no type was believed");
    }

    @Test
    void unknownKeysSurviveARoundTrip() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.ELDER);
        CompoundTag tag = g.save(new CompoundTag(), null);
        ListTag marks = new ListTag();
        CompoundTag row = new CompoundTag();
        row.put("key", NbtUtils.createUUID(A));
        row.putInt("value", 7);
        marks.add(row);
        tag.put("marks", marks);
        tag.putString("future", "kept");
        CompoundTag again = roundTrip(GroupIndex.load(tag, null)).save(new CompoundTag(), null);
        assertEquals(marks, again.getList("marks", 10));
        assertEquals("kept", again.getString("future"));
    }

    @Test
    void aTypeOfAnotherBuildIsKeptDormant() {
        GroupIndex g = new GroupIndex();
        UUID id = g.groupOf(L1);
        CompoundTag tag = g.save(new CompoundTag(), null);
        CompoundTag row = tag.getList("types", 10).getCompound(0);
        row.putString("value", "warband");
        GroupIndex back = GroupIndex.load(tag, null);
        assertEquals(GroupType.PACK, back.typeOf(id));
        assertEquals("warband", back.save(new CompoundTag(), null).getList("types", 10).getCompound(0).getString("value"));
    }

    @Test
    void aRankOfAnotherTypeIsHiddenNotDeleted() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        assertEquals(GroupRank.DAY_WALKER, g.rankOf(A, GroupType.CLAN));
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.COVEN));
        assertEquals(GroupRank.NONE, g.rankOf(A, GroupType.BROTHERHOOD));
        assertEquals(GroupRank.DAY_WALKER, g.rankOf(A, GroupType.CLAN), "asking in another type deleted the row");
    }

    @Test
    void aRankStoredUnderTheWrongTypeIsNotShown() {
        GroupIndex g = new GroupIndex();
        g.setRank(A, GroupRank.DAY_WALKER);
        CompoundTag tag = g.save(new CompoundTag(), null);
        tag.getList("ranks", 10).getCompound(0).putString("type", "coven"); // a damaged file: a Day Walker row filed under the coven
        assertEquals(GroupRank.NONE, GroupIndex.load(tag, null).rankOf(A, GroupType.COVEN));
    }
}
