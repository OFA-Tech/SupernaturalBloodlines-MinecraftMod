package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;
import org.junit.jupiter.api.Test;

class WardViewPayloadWireTest {
    private static WardViewPayload roundTrip(WardViewPayload p) {
        ByteBuf buf = Unpooled.buffer();
        WardViewPayload.STREAM_CODEC.encode(buf, p);
        WardViewPayload back = WardViewPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "bytes left over");
        return back;
    }

    @Test
    void anEmptyViewSurvivesTheWire() {
        assertEquals(WardHub.View.EMPTY, roundTrip(new WardViewPayload(WardHub.View.EMPTY)).view());
    }

    @Test
    void aViewWithWardsAndWelcomesSurvivesTheWire() {
        WardHub.Member bob = new WardHub.Member(new UUID(1, 2), "Bob");
        WardHub.View view = new WardHub.View(List.of(
                new WardHub.Own("0a1b2c3d", false, false, "minecraft:overworld", -120, 64, 305, List.of(bob)),
                new WardHub.Own("ffffffff", true, true, "minecraft:the_nether", 7, 70, -9, List.of())), List.of("Alice", "Cara"));
        assertEquals(view, roundTrip(new WardViewPayload(view)).view());
    }

    @Test
    void theCapsHoldAtTheBuildAndOnTheWire() {
        List<WardHub.Member> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            many.add(new WardHub.Member(new UUID(0, i), "a-very-long-player-name-over-sixteen"));
        }
        List<WardHub.Own> wards = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            wards.add(new WardHub.Own("0000000" + (i % 10), false, false, "minecraft:overworld", i, i, i, many));
        }
        WardHub.View view = new WardHub.View(wards, java.util.Collections.nCopies(40, "x"));
        assertEquals(WardHub.MAX_OWN, view.own().size());
        assertEquals(WardHub.MAX_INVITED, view.own().get(0).invited().size());
        assertEquals(WardHub.MAX_WELCOME, view.welcomedBy().size());
        assertEquals(WardHub.MAX_NAME, view.own().get(0).invited().get(0).name().length());
        assertEquals(view, roundTrip(new WardViewPayload(view)).view());
    }

    @Test
    void aHostileListLongerThanTheCapIsRefused() {
        ByteBuf buf = Unpooled.buffer();
        net.minecraft.network.VarInt.write(buf, WardHub.MAX_OWN + 1); // a ward list longer than the cap
        assertThrows(RuntimeException.class, () -> WardViewPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void theTypeIsOnTheSameChannelAsTheOtherHubPayloads() {
        assertTrue(WardViewPayload.TYPE.id().getPath().equals("ward_view"));
    }
}
