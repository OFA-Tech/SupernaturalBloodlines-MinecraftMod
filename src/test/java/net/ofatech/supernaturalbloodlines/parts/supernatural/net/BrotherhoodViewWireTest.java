package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodWire;
import org.junit.jupiter.api.Test;

class BrotherhoodViewWireTest {
    private static BrotherhoodView roundTrip(BrotherhoodView v) {
        ByteBuf buf = Unpooled.buffer();
        BrotherhoodViewPayload.STREAM_CODEC.encode(buf, new BrotherhoodViewPayload(v));
        BrotherhoodView back = BrotherhoodViewPayload.STREAM_CODEC.decode(buf).view();
        assertEquals(0, buf.readableBytes(), "bytes were left over");
        return back;
    }

    private static BrotherhoodView sample() {
        return new BrotherhoodView(true, true, true, 12, 20, true, 4, 12, true, true, 2, 3, 43_200, true);
    }

    @Test
    void roundTrip() {
        assertEquals(sample(), roundTrip(sample()));
        BrotherhoodView edge = new BrotherhoodView(true, true, true, 20, 20, true, 100, 100, false, false, 100, 20, BrotherhoodWire.MAX_SECONDS, false);
        assertEquals(edge, roundTrip(edge));
    }

    @Test
    void anEmptyViewRoundTrips() {
        assertEquals(BrotherhoodView.NONE, roundTrip(BrotherhoodView.NONE));
        assertFalse(BrotherhoodView.NONE.inBrotherhood());
    }

    private static ByteBuf wire(int own, int leader, int strikes, int seconds) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBoolean(true);
        buf.writeBoolean(true);
        buf.writeBoolean(true);
        ByteBufCodecs.VAR_INT.encode(buf, own);
        ByteBufCodecs.VAR_INT.encode(buf, leader);
        buf.writeBoolean(false);
        ByteBufCodecs.VAR_INT.encode(buf, 0);
        ByteBufCodecs.VAR_INT.encode(buf, 0);
        buf.writeBoolean(true);
        buf.writeBoolean(true);
        ByteBufCodecs.VAR_INT.encode(buf, strikes);
        ByteBufCodecs.VAR_INT.encode(buf, 3);
        ByteBufCodecs.VAR_INT.encode(buf, seconds);
        buf.writeBoolean(true);
        return buf;
    }

    @Test
    void aNumberOverTheBoundIsRejected() {
        assertEquals(20, BrotherhoodViewPayload.STREAM_CODEC.decode(wire(20, 0, 0, 0)).view().ownMark());
        assertThrows(DecoderException.class, () -> BrotherhoodViewPayload.STREAM_CODEC.decode(wire(21, 0, 0, 0)));
        assertThrows(DecoderException.class, () -> BrotherhoodViewPayload.STREAM_CODEC.decode(wire(0, 21, 0, 0)));
        assertEquals(100, BrotherhoodViewPayload.STREAM_CODEC.decode(wire(0, 0, 100, 0)).view().strikes());
        assertThrows(DecoderException.class, () -> BrotherhoodViewPayload.STREAM_CODEC.decode(wire(0, 0, 101, 0)));
        assertEquals(BrotherhoodWire.MAX_SECONDS, BrotherhoodViewPayload.STREAM_CODEC.decode(wire(0, 0, 0, BrotherhoodWire.MAX_SECONDS)).view().breakerSecondsLeft());
        assertThrows(DecoderException.class, () -> BrotherhoodViewPayload.STREAM_CODEC.decode(wire(0, 0, 0, BrotherhoodWire.MAX_SECONDS + 1)));
        assertThrows(DecoderException.class, () -> BrotherhoodViewPayload.STREAM_CODEC.decode(wire(-1, 0, 0, 0)));
    }

    @Test
    void theRecordNeverHoldsANumberOverTheBound() {
        BrotherhoodView v = new BrotherhoodView(true, true, true, 99, -4, false, 500, 500, true, true, 900, 900, Integer.MAX_VALUE, true);
        assertEquals(20, v.ownMark());
        assertEquals(0, v.leaderMark());
        assertEquals(100, v.armsBrothers());
        assertEquals(100, v.strikes());
        assertEquals(BrotherhoodWire.MAX_SECONDS, v.breakerSecondsLeft());
        assertEquals(v, roundTrip(v));
    }

    @Test
    void theViewIsOnlyPrimitives() {
        List<String> bad = new ArrayList<>();
        for (RecordComponent c : BrotherhoodView.class.getRecordComponents()) {
            if (c.getType() != boolean.class && c.getType() != int.class) {
                bad.add(c.getName() + ":" + c.getType().getSimpleName());
            }
        }
        assertEquals(List.of(), bad, "a view that carries more than booleans and numbers");
        assertEquals(14, BrotherhoodView.class.getRecordComponents().length);
    }

    @Test
    void theBreakerFlagIsTheSecondsLeft() {
        assertTrue(new BrotherhoodView(true, true, true, 0, 0, false, 0, 0, true, true, 1, 3, 1, true).breaker());
        assertFalse(new BrotherhoodView(true, true, true, 0, 0, false, 0, 0, true, true, 1, 3, 0, true).breaker());
    }
}