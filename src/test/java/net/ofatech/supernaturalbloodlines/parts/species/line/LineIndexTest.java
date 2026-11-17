package net.ofatech.supernaturalbloodlines.parts.species.line;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import org.junit.jupiter.api.Test;

class LineIndexTest {
    private static final UUID O = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void setRootAndQuery() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, true);
        assertEquals(O, idx.rootOf(A).orElseThrow());
        assertEquals(List.of(new LineIndex.Member(A, true)), idx.membersOf(O));
    }

    @Test
    void anOriginalsOwnEntryIsNotItsMember() {
        LineIndex idx = new LineIndex();
        idx.setRoot(O, O, true);
        assertEquals(O, idx.rootOf(O).orElseThrow());
        assertTrue(idx.membersOf(O).isEmpty());
    }

    @Test
    void freeMovesTheMemberToItsOwnRoot() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, true);
        idx.free(A, O, LineIndex.Kind.BROKEN_LINE);
        assertEquals(A, idx.rootOf(A).orElseThrow());
        assertEquals(new LineIndex.Freed(O, LineIndex.Kind.BROKEN_LINE), idx.freedOf(A).orElseThrow());
        assertTrue(idx.membersOf(O).isEmpty());
        assertTrue(idx.membersOf(A).isEmpty());
    }

    @Test
    void settingARootClearsFreed() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, true);
        idx.free(A, O, LineIndex.Kind.IMPRISONED);
        idx.setRoot(A, O, true);
        assertTrue(idx.freedOf(A).isEmpty());
        assertEquals(O, idx.rootOf(A).orElseThrow());
    }

    @Test
    void removeForgetsEverything() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, false);
        idx.remove(A);
        assertTrue(idx.rootOf(A).isEmpty());
        assertTrue(idx.membersOf(O).isEmpty());
        idx.remove(A); // twice is fine
    }

    @Test
    void deadRootsAndOwedRoundTrip() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, true);
        idx.setRoot(B, NpcOriginal.KLAUS.uuid(), false);
        idx.free(A, O, LineIndex.Kind.IMPRISONED);
        idx.markDead(NpcOriginal.KLAUS.uuid());
        idx.addOwed(B, NpcOriginal.KLAUS.uuid());

        LineIndex back = LineIndex.load(idx.save(new CompoundTag(), null), null);
        assertEquals(A, back.rootOf(A).orElseThrow());
        assertEquals(new LineIndex.Freed(O, LineIndex.Kind.IMPRISONED), back.freedOf(A).orElseThrow());
        assertEquals(List.of(new LineIndex.Member(B, false)), back.membersOf(NpcOriginal.KLAUS.uuid()));
        assertTrue(back.isDead(NpcOriginal.KLAUS.uuid()));
        assertFalse(back.isDead(O));
        assertTrue(back.deadNpcRoots().contains(NpcOriginal.KLAUS));
        assertEquals(NpcOriginal.KLAUS.uuid(), back.takeOwed(B).orElseThrow());
        assertTrue(back.takeOwed(B).isEmpty());
    }

    @Test
    void loadsAnEmptyTagLeniently() {
        LineIndex idx = LineIndex.load(new CompoundTag(), null);
        assertTrue(idx.membersOf(O).isEmpty());
        CompoundTag tag = new LineIndex().save(new CompoundTag(), null);
        LineIndex one = new LineIndex();
        one.setRoot(A, O, true);
        CompoundTag saved = one.save(new CompoundTag(), null);
        // a record with a freed_from but no freed_kind loads as not freed
        ((CompoundTag) saved.getList("entries", 10).get(0)).put("freed_from", net.minecraft.nbt.NbtUtils.createUUID(O));
        assertTrue(LineIndex.load(saved, null).freedOf(A).isEmpty());
        assertTrue(LineIndex.load(tag, null).rootOf(A).isEmpty());
    }

    @Test
    void aBadEntryIsSkippedAndTheGoodOnesLoad() {
        LineIndex idx = new LineIndex();
        idx.setRoot(A, O, true);
        idx.setRoot(B, O, true);
        idx.addOwed(B, O);
        idx.markDead(NpcOriginal.KLAUS.uuid());
        CompoundTag saved = idx.save(new CompoundTag(), null);
        net.minecraft.nbt.ListTag entries = saved.getList("entries", 10);
        CompoundTag bad = new CompoundTag();
        bad.putString("id", "not a uuid");
        bad.putString("root", "nor this");
        entries.add(0, bad);
        CompoundTag half = new CompoundTag();
        half.put("id", net.minecraft.nbt.NbtUtils.createUUID(UUID.randomUUID()));
        entries.add(1, half);
        net.minecraft.nbt.ListTag dead = saved.getList("dead", 11);
        dead.add(0, new net.minecraft.nbt.IntArrayTag(new int[] {1, 2, 3}));
        net.minecraft.nbt.ListTag owed = saved.getList("owed", 10);
        owed.add(0, new CompoundTag());
        LineIndex back = LineIndex.load(saved, null);
        assertEquals(O, back.rootOf(A).orElseThrow());
        assertEquals(O, back.rootOf(B).orElseThrow());
        assertEquals(O, back.takeOwed(B).orElseThrow());
        assertTrue(back.isDead(NpcOriginal.KLAUS.uuid()));
    }
}
