package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackIndexJoinedTest {
    private static final UUID A = new UUID(1, 1);
    private static final UUID B = new UUID(2, 2);
    private static final UUID C = new UUID(3, 3);
    private static final UUID D = new UUID(4, 4);
    private static final List<UUID[]> moves = new ArrayList<>();
    private static boolean listening;
    private final long[] now = {1_000_000_000_000L};

    @BeforeEach
    void clock() {
        PackIndex.overrideClock(() -> now[0]);
        if (!listening) {
            PackIndex.onLeadershipMoved((from, to) -> moves.add(new UUID[] {from, to}));
            listening = true;
        }
        moves.clear();
    }

    @AfterEach
    void restore() {
        PackIndex.overrideClock(System::currentTimeMillis);
    }

    @Test
    void aLeaderWhoJoinsAnotherPackMovesItsPoolButAPlainMemberMovesNothing() {
        PackIndex i = new PackIndex();
        assertTrue(i.join(A, B)); // B is a plain member
        assertTrue(moves.isEmpty(), "a plain member joined and a pool moved: " + moves);
        assertTrue(i.join(C, D)); // D leads nobody yet: still plain
        moves.clear();
        PackIndex j = new PackIndex();
        assertTrue(j.join(B, C)); // B now leads C
        moves.clear();
        assertTrue(j.join(A, B)); // the leader B joins A, bringing C
        assertEquals(1, moves.size());
        assertEquals(B, moves.get(0)[0]);
        assertEquals(A, moves.get(0)[1]);
    }

    @Test
    void aMembersDaysRunFromTheirJoinAndALeadersFromTheFirstMember() {
        PackIndex i = new PackIndex();
        assertTrue(i.join(A, B));
        now[0] += 5 * 86_400_000L;
        assertTrue(i.join(A, C));
        assertEquals(Optional.of(1_000_000_000_000L + 5 * 86_400_000L), i.inPackSince(C));
        assertEquals(Optional.of(1_000_000_000_000L), i.inPackSince(B));
        assertEquals(Optional.of(1_000_000_000_000L), i.inPackSince(A), "the pack is as old as its first member");
        assertTrue(i.inPackSince(D).isEmpty());
    }

    @Test
    void aPromotedLeadersOldSelfKeepsThePacksAgeNotTheMomentOfPromotion() {
        PackIndex i = new PackIndex();
        assertTrue(i.join(A, B));
        now[0] += 9 * 86_400_000L;
        i.promote(A, B); // B leads; A stays as a member
        assertEquals(Optional.of(1_000_000_000_000L), i.inPackSince(A), "the old leader was reset to now");
        assertEquals(Optional.of(1_000_000_000_000L), i.inPackSince(B));
    }

    @Test
    void aPackFromBeforeJoinsWereRecordedStartsCountingWhenItLoadsAndTheStampIsSaved() {
        PackIndex i = new PackIndex();
        assertTrue(i.join(A, B));
        CompoundTag old = i.save(new CompoundTag(), null);
        old.remove("joined_at");
        now[0] += 3 * 86_400_000L;
        PackIndex loaded = PackIndex.load(old, null);
        assertEquals(Optional.of(now[0]), loaded.inPackSince(B));
        assertEquals(Optional.of(now[0]), loaded.inPackSince(A), "a leader of a pre-upgrade pack had no day count");
        assertTrue(loaded.isDirty(), "the stamp must be saved or it would move on every load");
        now[0] += 86_400_000L;
        PackIndex again = PackIndex.load(loaded.save(new CompoundTag(), null), null);
        assertEquals(loaded.inPackSince(B), again.inPackSince(B), "the stamp moved");
        assertFalse(again.inPackSince(B).isEmpty());
    }
}

