package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SirelineIndexTest {
    private static final UUID SIRE = UUID.randomUUID();
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void childrenAndSireAreIndexed() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.add(SIRE, B, "minecraft:cow", "Cow");
        assertEquals(List.of(new SirelineIndex.Node(A, "minecraft:player", "Ann"), new SirelineIndex.Node(B, "minecraft:cow", "Cow")),
                idx.childrenOf(SIRE));
        assertEquals(SIRE, idx.sireOf(A).orElseThrow());
        assertTrue(idx.sireOf(SIRE).isEmpty());
        assertTrue(idx.childrenOf(A).isEmpty());
    }

    @Test
    void removeDropsOnlyTheChildLink() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.add(A, B, "minecraft:player", "Bob");
        idx.remove(A);
        assertTrue(idx.sireOf(A).isEmpty());
        assertTrue(idx.childrenOf(SIRE).isEmpty());
        // Bob is still sired by Ann, and Ann's name is still known for the tree
        assertEquals(A, idx.sireOf(B).orElseThrow());
        assertEquals("Ann", idx.nameOf(A).orElseThrow());
        idx.remove(A); // twice is fine
    }

    @Test
    void addingAChildAgainMovesItAndKeepsOneParent() {
        SirelineIndex idx = new SirelineIndex();
        UUID other = UUID.randomUUID();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.add(other, A, "minecraft:player", "Ann2");
        assertTrue(idx.childrenOf(SIRE).isEmpty());
        assertEquals(List.of(new SirelineIndex.Node(A, "minecraft:player", "Ann2")), idx.childrenOf(other));
        idx.add(other, A, "minecraft:player", "Ann2");
        assertEquals(1, idx.childrenOf(other).size());
    }

    @Test
    void nobodySiresThemselves() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(A, A, "minecraft:player", "Ann");
        assertTrue(idx.sireOf(A).isEmpty());
        assertTrue(idx.childrenOf(A).isEmpty());
    }

    @Test
    void renameUpdatesKnownNodesOnly() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.setName(A, "Anna");
        assertEquals("Anna", idx.childrenOf(SIRE).get(0).name());
        UUID stranger = UUID.randomUUID();
        idx.setName(stranger, "Nobody");
        assertTrue(idx.nameOf(stranger).isEmpty());
    }

    @Test
    void survivesSaveAndLoad() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.add(SIRE, B, "minecraft:cow", "Cow");
        idx.setName(SIRE, "Sire");
        SirelineIndex back = SirelineIndex.load(idx.save(new CompoundTag(), null), null);
        assertEquals(idx.childrenOf(SIRE), back.childrenOf(SIRE));
        assertEquals(SIRE, back.sireOf(B).orElseThrow());
        assertEquals("Sire", back.nameOf(SIRE).orElseThrow());
        back.remove(A);
        assertFalse(back.childrenOf(SIRE).stream().anyMatch(n -> n.id().equals(A)));
    }

    @Test
    void descendantClimbsTheLineAndStopsOnACycle() {
        SirelineIndex idx = new SirelineIndex();
        idx.add(SIRE, A, "minecraft:player", "Ann");
        idx.add(A, B, "minecraft:player", "Bob");
        assertTrue(idx.isDescendant(SIRE, B));
        assertTrue(idx.isDescendant(A, B));
        assertFalse(idx.isDescendant(B, SIRE));
        assertFalse(idx.isDescendant(SIRE, SIRE));
        assertFalse(idx.isDescendant(SIRE, UUID.randomUUID()));
        idx.add(B, SIRE, "minecraft:player", "Sire"); // a cure and a re-turn closed the loop
        UUID outsider = UUID.randomUUID();
        assertFalse(idx.isDescendant(outsider, A));
        assertTrue(idx.isDescendant(B, A));
    }

    @Test
    void pendingMobDeathsAreTakenOnceAndSurviveSaveAndLoad() {
        SirelineIndex idx = new SirelineIndex();
        idx.addPendingMob(B);
        assertTrue(idx.isPendingMob(B));
        assertFalse(idx.isPendingMob(A));
        SirelineIndex back = SirelineIndex.load(idx.save(new CompoundTag(), null), null);
        assertTrue(back.takePendingMob(B));
        assertFalse(back.isPendingMob(B));
    }

    @Test
    void pendingMobsOlderThanThirtyDaysAreForgotten() {
        SirelineIndex idx = new SirelineIndex();
        long day = 24L * 60 * 60 * 1000;
        long now = 100 * day;
        idx.addPendingMob(A, now - 31 * day);
        idx.addPendingMob(B, now - 29 * day);
        idx.prunePending(now);
        assertFalse(idx.isPendingMob(A), "a 31 day old debt was kept");
        assertTrue(idx.isPendingMob(B), "a 29 day old debt was forgotten");
        // adding prunes too, so the set cannot grow without bound
        UUID c = UUID.randomUUID();
        idx.addPendingMob(c, now + 40 * day);
        assertFalse(idx.isPendingMob(B));
        assertTrue(idx.isPendingMob(c));
    }

    @Test
    void pendingStampsAndTheOldKeysSurviveSaveAndLoad() {
        SirelineIndex idx = new SirelineIndex();
        idx.addPendingMob(A, System.currentTimeMillis());
        idx.addLegacyPendingPlayer(B);
        SirelineIndex back = SirelineIndex.load(idx.save(new CompoundTag(), null), null);
        assertTrue(back.isPendingMob(A));
        assertTrue(back.takeLegacyPendingPlayer(B));
        assertFalse(back.takeLegacyPendingPlayer(B));
        // an old save: the list of ids without a time is kept, stamped as of loading
        CompoundTag old = new CompoundTag();
        net.minecraft.nbt.ListTag ids = new net.minecraft.nbt.ListTag();
        ids.add(net.minecraft.nbt.NbtUtils.createUUID(A));
        old.put("pendingMobs", ids);
        net.minecraft.nbt.ListTag players = new net.minecraft.nbt.ListTag();
        players.add(net.minecraft.nbt.NbtUtils.createUUID(B));
        old.put("pendingDeaths", players);
        SirelineIndex loaded = SirelineIndex.load(old, null);
        assertTrue(loaded.isPendingMob(A));
        assertTrue(loaded.takeLegacyPendingPlayer(B));
    }
}
