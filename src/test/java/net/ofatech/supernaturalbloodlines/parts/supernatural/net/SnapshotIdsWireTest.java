package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

class SnapshotIdsWireTest {
    @Test
    void theIdsOfNamedPeopleSurviveTheWire() {
        UUID sire = UUID.randomUUID();
        UUID mate = UUID.randomUUID();
        var genes = new LinkedHashMap<String, Genes.Knowledge>();
        Genes.ALL.forEach(g -> genes.put(g, Genes.Knowledge.UNKNOWN));
        HubSnapshotPayload s = new HubSnapshotPayload(new HubSnapshotPayload.Family(Optional.of("Klaus"), false, List.of(), List.of(), List.of(), HubSocial.Origin.NONE),
                List.of(new HubSocial.Member("Derek", "beta", true, Optional.of(mate))), 0, genes, List.of(new HubSocial.Person("Klaus", sire)));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
        HubSnapshotPayload back = HubSnapshotPayload.STREAM_CODEC.decode(buf);
        assertEquals(s.family(), back.family(), "family");
        assertEquals(s.pack(), back.pack(), "pack");
        assertEquals(s.people(), back.people(), "people");
        assertEquals(s.genes(), back.genes(), "genes");
        assertEquals(s, back);
    }

    @Test
    void theWorldNowFlagsSurviveTheWireInEveryCombination() {
        for (boolean sleep : new boolean[] {false, true}) {
            for (boolean stirs : new boolean[] {false, true}) {
                HubSnapshotPayload s = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, new LinkedHashMap<>(), List.of(),
                        new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(sleep, stirs));
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
                HubSnapshotPayload back = HubSnapshotPayload.STREAM_CODEC.decode(buf);
                assertEquals(sleep, back.world().sleepRule(), "sleep rule " + sleep + "/" + stirs);
                assertEquals(stirs, back.world().awakeningPending(), "pending " + sleep + "/" + stirs);
                assertEquals(0, back.world().trial(), "trial " + sleep + "/" + stirs);
                assertEquals(0, buf.readableBytes(), "nothing left on the wire");
            }
        }
    }

    /**
     * The wire format of the hub snapshot and the protocol number go together: a client and a server on different formats must not connect (they would
     * misread each other's bytes). This pins the bytes of an empty snapshot with both flags set, and the protocol they belong to. If you change
     * the snapshot, this fails: raise {@code LadderNet.PROTOCOL} and update both constants here, in the same commit.
     */
    private static final String GOLDEN_PROTOCOL = "9";
    private static final String GOLDEN_HEX = "00000000000b4e4f545f56414d504952450000044e4f4e450000000000010100000000";

    @Test
    void theSnapshotWireFormatAndTheProtocolNumberChangeTogether() {
        HubSnapshotPayload s = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, new LinkedHashMap<>(), List.of(),
                new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(true, true));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        assertEquals(GOLDEN_HEX, hex.toString(), "the snapshot's bytes changed: raise LadderNet.PROTOCOL and update GOLDEN_HEX and GOLDEN_PROTOCOL");
        assertEquals(GOLDEN_PROTOCOL, LadderNet.PROTOCOL, "the protocol number does not go with the snapshot's wire format");
    }

    @Test
    void aSnapshotBuiltWithoutTheFlagsSaysNeither() {
        HubSnapshotPayload s = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, new LinkedHashMap<>());
        assertEquals(net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.NONE, s.world());
    }

    @Test
    void theTrialNoticeSurvivesTheWire() {
        for (int trial = 0; trial <= 2; trial++) {
            HubSnapshotPayload s = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, new LinkedHashMap<>(), List.of(),
                    new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(false, false, trial));
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
            assertEquals(trial, HubSnapshotPayload.STREAM_CODEC.decode(buf).world().trial());
        }
        assertEquals(2, new net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags(false, false, 99).trial(), "a bad value from the wire is clamped");
    }
}
