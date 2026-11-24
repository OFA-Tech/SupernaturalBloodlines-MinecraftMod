package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenWire;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import org.junit.jupiter.api.Test;

class CovenViewWireTest {
    private static CovenView roundTrip(CovenView v) {
        ByteBuf buf = Unpooled.buffer();
        CovenViewPayload.STREAM_CODEC.encode(buf, new CovenViewPayload(v));
        CovenView back = CovenViewPayload.STREAM_CODEC.decode(buf).view();
        assertEquals(0, buf.readableBytes(), "bytes left over");
        return back;
    }

    private static List<String> names(int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add("witch" + i);
        }
        return out;
    }

    @Test
    void roundTrip() {
        CovenView v = new CovenView(2, 5, CovenView.Circle.JOINED, "Esther", names(3), 1200, 3000, 240, Consent.NOSEBLEED, 700, 285_714, true, 0,
                Optional.of(new CovenView.Vote(VoteKind.SHUN, "Rebekah", 2, 1, 3, CovenView.Ballot.YES, 17)));
        assertEquals(v, roundTrip(v));
    }

    @Test
    void anEmptyViewRoundTrips() {
        assertEquals(CovenView.NONE, roundTrip(CovenView.NONE));
        CovenView shunned = CovenView.NONE.shunned(5);
        assertEquals(5, roundTrip(shunned).shunnedDaysLeft());
    }

    @Test
    void maxCircleRoundTrips() {
        CovenView v = new CovenView(3, 13, CovenView.Circle.ANCHOR, "a".repeat(CovenWire.MAX_NAME), names(CovenWire.MAX_CIRCLE), 3000, 3000, 0, Consent.DAMAGE, 0, 0, false, 0, Optional.empty());
        assertEquals(CovenWire.MAX_CIRCLE, roundTrip(v).circleNames().size());
        assertEquals(v, roundTrip(v));
    }

    @Test
    void aCountOverTheCapIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        net.minecraft.network.codec.StreamCodec<ByteBuf, List<String>> wide = ByteBufCodecs.stringUtf8(CovenWire.MAX_NAME).apply(ByteBufCodecs.list(100));
        wide.encode(buf, names(CovenWire.MAX_CIRCLE + 1));
        assertThrows(DecoderException.class, () -> CovenViewPayload.NAMES.decode(buf));
    }

    @Test
    void aLongNameIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.stringUtf8(500).encode(buf, "n".repeat(CovenWire.MAX_NAME + 1));
        assertThrows(DecoderException.class, () -> CovenViewPayload.NAME.decode(buf));
    }

    @Test
    void aLongNameInAViewIsCutNotCarried() {
        CovenView v = new CovenView(1, 3, CovenView.Circle.JOINED, "x".repeat(200), List.of("y".repeat(200)), 0, 0, 0, Consent.NONE, 0, 0, false, 0, Optional.empty());
        assertEquals(CovenWire.MAX_NAME, v.anchorName().length());
        assertEquals(CovenWire.MAX_NAME, roundTrip(v).circleNames().get(0).length());
    }

    @Test
    void anUnknownEnumIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, 1);
        ByteBufCodecs.VAR_INT.encode(buf, 3);
        ByteBufCodecs.VAR_INT.encode(buf, 9);
        assertThrows(DecoderException.class, () -> CovenViewPayload.STREAM_CODEC.decode(buf));
    }

    /** No component of the view (or of its vote) can hold another person: no id, no map, no list of anything but the viewer's own coven's names. */
    @Test
    void theViewNeverCarriesAnotherMembersMeterConsentOrBallot() {
        List<String> top = new ArrayList<>();
        for (RecordComponent c : CovenView.class.getRecordComponents()) {
            top.add(c.getName() + ":" + c.getType().getSimpleName());
        }
        assertEquals(List.of("tier:int", "size:int", "circle:Circle", "anchorName:String", "circleNames:List", "meter:int", "meterCap:int", "regenSeconds:int", "consent:Consent",
                "sharePower:int", "shareEp:long", "shareKnown:boolean", "shunnedDaysLeft:int", "vote:Optional"), top);
        List<String> vote = new ArrayList<>();
        for (RecordComponent c : CovenView.Vote.class.getRecordComponents()) {
            vote.add(c.getName() + ":" + c.getType().getSimpleName());
        }
        assertEquals(List.of("kind:VoteKind", "subjectName:String", "yes:int", "no:int", "electors:int", "mine:Ballot", "hoursLeft:int"), vote);
        for (RecordComponent c : CovenView.class.getRecordComponents()) {
            assertTrue(!c.getType().getName().contains("UUID") && !java.util.Map.class.isAssignableFrom(c.getType()), c.getName());
        }
    }
}