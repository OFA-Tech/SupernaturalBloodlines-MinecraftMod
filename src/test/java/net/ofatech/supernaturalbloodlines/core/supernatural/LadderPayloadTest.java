package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ClaimTaskPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.LadderTablePayload;
import org.junit.jupiter.api.Test;

/** The ladder table goes to the client as text and comes back as the same ladders. */
class LadderPayloadTest {
    @Test
    void theTableSurvivesTheWire() {
        LadderTable server = ShippedLadders.all();
        ByteBuf buf = Unpooled.buffer();
        LadderTablePayload.STREAM_CODEC.encode(buf, LadderTablePayload.of(server.sources(), mod -> true, part -> true));
        LadderTablePayload received = LadderTablePayload.STREAM_CODEC.decode(buf);
        LadderTable.Parsed client = received.parseWithServerSets();
        assertEquals(java.util.List.of(), client.rejected());
        for (SupernaturalRace race : SupernaturalRace.values()) {
            assertEquals(server.get(race), client.table().get(race), "ladder of " + race);
        }
    }

    @Test
    void theClientPrunesWithTheServersSetsNotItsOwn() {
        // the server has no Tensura and no species part: the client, whatever it has installed, gets the same pruned ladder
        LadderTable server = LadderTable.parse(ShippedLadders.files(), mod -> !mod.equals("tensura"), part -> !part.equals("species")).table();
        ByteBuf buf = Unpooled.buffer();
        LadderTablePayload.STREAM_CODEC.encode(buf, LadderTablePayload.of(ShippedLadders.files(), mod -> !mod.equals("tensura"), part -> !part.equals("species")));
        LadderTable client = LadderTablePayload.STREAM_CODEC.decode(buf).parseWithServerSets().table();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            assertEquals(server.get(race), client.get(race), "ladder of " + race);
        }
        assertEquals(6, client.get(SupernaturalRace.VAMPIRE).rungs().size());
    }

    @Test
    void theServersSetsListOnlyWhatItHas() {
        LadderTablePayload p = LadderTablePayload.of(ShippedLadders.files(), mod -> mod.equals("tensura"), part -> part.equals("species"));
        assertEquals(java.util.List.of("tensura"), p.mods());
        assertEquals(java.util.List.of("species"), p.parts());
    }

    @Test
    void aFileThatIsNotJsonIsDroppedNotACrash() {
        LadderTablePayload bad = new LadderTablePayload(java.util.Map.of("vampire", "{ not json", "witch", "{\"rungs\":[{\"id\":\"A-0\"}]}"), java.util.List.of(), java.util.List.of());
        assertEquals(java.util.Set.of("witch"), bad.parsed().keySet());
    }

    @Test
    void theClaimPayloadCarriesJustAName() {
        ByteBuf buf = Unpooled.buffer();
        ClaimTaskPayload.STREAM_CODEC.encode(buf, new ClaimTaskPayload("L-VAM-1a"));
        assertEquals("L-VAM-1a", ClaimTaskPayload.STREAM_CODEC.decode(buf).taskId());
        assertTrue(buf.readableBytes() == 0);
    }

    @Test
    void aClaimNameIsLimited() {
        ByteBuf buf = Unpooled.buffer();
        ClaimTaskPayload.STREAM_CODEC.encode(Unpooled.buffer(), new ClaimTaskPayload("x"));
        io.netty.handler.codec.EncoderException tooLong = null;
        try {
            ClaimTaskPayload.STREAM_CODEC.encode(buf, new ClaimTaskPayload("x".repeat(65)));
        } catch (io.netty.handler.codec.EncoderException e) {
            tooLong = e;
        }
        assertTrue(tooLong != null, "a 65 character name was sent");
    }
}
