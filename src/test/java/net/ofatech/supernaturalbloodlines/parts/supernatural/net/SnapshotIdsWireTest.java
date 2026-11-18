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
                List.of(new HubSocial.Member("Derek", "beta", true, Optional.of(mate))), 0, genes, List.of(), List.of(new HubSocial.Person("Klaus", sire)));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
        HubSnapshotPayload back = HubSnapshotPayload.STREAM_CODEC.decode(buf);
        assertEquals(s.family(), back.family(), "family");
        assertEquals(s.pack(), back.pack(), "pack");
        assertEquals(s.people(), back.people(), "people");
        assertEquals(s.genes(), back.genes(), "genes");
        assertEquals(s.rites(), back.rites(), "rites");
        assertEquals(s, back);
    }
}

