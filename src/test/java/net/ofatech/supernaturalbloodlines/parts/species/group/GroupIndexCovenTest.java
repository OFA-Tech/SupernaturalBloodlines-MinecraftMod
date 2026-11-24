package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import org.junit.jupiter.api.Test;

/** Plan 20b: the channel meter and the consent level live in the group index, and nothing of 20a is lost. */
class GroupIndexCovenTest {
    private static final UUID A = new UUID(7, 1);
    private static final UUID B = new UUID(7, 2);

    private static GroupIndex roundTrip(GroupIndex i) {
        return GroupIndex.load(i.save(new CompoundTag(), null), null);
    }

    @Test
    void channelRoundTrips() {
        GroupIndex g = new GroupIndex();
        g.setChannel(A, 1200, 5_000L, 3000);
        GroupIndex back = roundTrip(g);
        assertEquals(new GroupIndex.ChannelRow(1200, 5_000L), back.channelOf(A));
        assertNull(back.channelOf(B));
    }

    @Test
    void aFullIdleChannelRowIsNotStored() {
        GroupIndex g = new GroupIndex();
        g.setChannel(A, 100, 1L, 3000);
        assertNotNull(g.channelOf(A));
        g.setChannel(A, 3000, 2L, 3000);
        assertNull(g.channelOf(A));
        assertFalse(g.save(new CompoundTag(), null).getList("channel", 10).size() > 0);
    }

    @Test
    void consentStoresOnlyNonDefault() {
        GroupIndex g = new GroupIndex();
        assertEquals(Consent.NONE, g.consentOf(A));
        g.setConsent(A, Consent.DAMAGE);
        g.setConsent(B, Consent.NONE);
        CompoundTag saved = g.save(new CompoundTag(), null);
        assertEquals(1, saved.getList("consent", 10).size());
        assertEquals(Consent.DAMAGE, roundTrip(g).consentOf(A));
        g.setConsent(A, Consent.NONE);
        assertEquals(0, g.save(new CompoundTag(), null).getList("consent", 10).size());
    }

    @Test
    void unknownKeysStillRoundTrip() {
        CompoundTag tag = new GroupIndex().save(new CompoundTag(), null);
        tag.putString("from_a_later_build", "kept");
        GroupIndex g = GroupIndex.load(tag, null);
        g.setChannel(A, 5, 1L, 3000);
        g.setConsent(B, Consent.NOSEBLEED);
        CompoundTag again = g.save(new CompoundTag(), null);
        assertEquals("kept", again.getString("from_a_later_build"));
        GroupIndex back = GroupIndex.load(again, null);
        assertEquals(5, back.channelOf(A).value());
        assertEquals(Consent.NOSEBLEED, back.consentOf(B));
    }

    @Test
    void aDamagedChannelRowCostsOnlyThatRow() {
        GroupIndex g = new GroupIndex();
        g.setChannel(A, 10, 1L, 3000);
        g.setChannel(B, 20, 2L, 3000);
        CompoundTag saved = g.save(new CompoundTag(), null);
        ListTag rows = saved.getList("channel", 10);
        CompoundTag bad = new CompoundTag();
        bad.putString("key", "not a uuid");
        rows.add(0, bad);
        GroupIndex back = GroupIndex.load(saved, null);
        assertEquals(10, back.channelOf(A).value());
        assertEquals(20, back.channelOf(B).value());
        assertTrue(NbtUtils.createUUID(A) != null);
    }
}