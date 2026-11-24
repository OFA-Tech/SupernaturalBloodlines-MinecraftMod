package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.GroupWire;
import org.junit.jupiter.api.Test;

/** The group view on the wire: whole, bounded, and about the viewer's own group only. */
class GroupViewWireTest {
    private static GroupView.Row row(int n) {
        return new GroupView.Row("Name" + n, n == 0 ? "leader" : "member", n == 1 ? GroupRank.ELDER : GroupRank.NONE, n % 2 == 0, Optional.of(new UUID(0, n)));
    }

    private static GroupViewPayload roundTrip(GroupViewPayload p) {
        ByteBuf buf = Unpooled.buffer();
        GroupViewPayload.STREAM_CODEC.encode(buf, p);
        return GroupViewPayload.STREAM_CODEC.decode(buf);
    }

    @Test
    void roundTrip() {
        GroupView v = new GroupView(GroupType.COVEN, true, GroupView.ELDER, "Morgana", 12, List.of(row(0), row(1), row(2)));
        assertEquals(new GroupViewPayload(v), roundTrip(new GroupViewPayload(v)));
    }

    @Test
    void anEmptyViewRoundTrips() {
        GroupViewPayload back = roundTrip(new GroupViewPayload(GroupView.none(GroupType.CLAN)));
        assertFalse(back.view().hasGroup());
        assertEquals(GroupType.CLAN, back.view().type());
        assertTrue(back.view().members().isEmpty());
    }

    @Test
    void maxMembersRoundTrips() {
        List<GroupView.Row> rows = new ArrayList<>();
        for (int i = 0; i < GroupWire.MAX_MEMBERS; i++) {
            rows.add(row(i));
        }
        assertEquals(GroupWire.MAX_MEMBERS, roundTrip(new GroupViewPayload(new GroupView(GroupType.PACK, true, GroupView.MEMBER, "L", 0, rows))).view().members().size());
    }

    @Test
    void theRecordCutsAListOverTheCap() {
        List<GroupView.Row> rows = new ArrayList<>();
        for (int i = 0; i < GroupWire.MAX_MEMBERS + 5; i++) {
            rows.add(row(i));
        }
        assertEquals(GroupWire.MAX_MEMBERS, new GroupView(GroupType.PACK, true, GroupView.MEMBER, "L", 0, rows).members().size());
    }

    @Test
    void aCountOverTheCapIsRejected() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("coven", GroupWire.MAX_RANK);
        buf.writeBoolean(true);
        buf.writeByte(GroupView.MEMBER);
        buf.writeUtf("L", GroupWire.MAX_NAME);
        buf.writeVarInt(0);
        buf.writeVarInt(GroupWire.MAX_MEMBERS + 1);
        assertThrows(DecoderException.class, () -> GroupViewPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void aLongNameIsRejected() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("coven", GroupWire.MAX_RANK);
        buf.writeBoolean(true);
        buf.writeByte(GroupView.MEMBER);
        buf.writeUtf("x".repeat(GroupWire.MAX_NAME + 1), 400);
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        assertThrows(DecoderException.class, () -> GroupViewPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void anUnknownTypeIsRejected() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("warband", GroupWire.MAX_RANK);
        assertThrows(DecoderException.class, () -> GroupViewPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void theClipCutsToTheWireBound() {
        assertEquals(GroupWire.MAX_NAME, GroupView.clip("y".repeat(100), GroupWire.MAX_NAME).length());
        assertEquals("ok", GroupView.clip("ok", GroupWire.MAX_NAME));
    }

    @Test
    void theViewNeverCarriesAMeterOrAVote() {
        for (Class<?> c : new Class<?>[] {GroupView.class, GroupView.Row.class, GroupViewPayload.class}) {
            for (var component : c.getRecordComponents()) {
                String n = component.getName().toLowerCase();
                assertFalse(n.contains("meter") || n.contains("consent") || n.contains("vote"), c.getSimpleName() + "." + n);
            }
        }
    }
}
