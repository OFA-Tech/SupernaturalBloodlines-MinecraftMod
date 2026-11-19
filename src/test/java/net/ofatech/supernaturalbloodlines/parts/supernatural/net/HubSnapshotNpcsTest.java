package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

/** The pack's wolves ride in the hub snapshot (protocol 4): they survive the wire and are cut at the cap. */
class HubSnapshotNpcsTest {
    private static HubSocial.Npc npc(int token) {
        return new HubSocial.Npc(token, "Ash " + token, "werewolves:wolf", "brother", "guard", true, false, true, 12, 64, 3, "Derek", true, 50);
    }

    @Test
    void theWolvesSurviveTheCodec() {
        var snap = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(), List.of(), net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.NONE,
                List.of(npc(1), npc(2)));
        var buf = Unpooled.buffer();
        HubSnapshotPayload.STREAM_CODEC.encode(buf, snap);
        assertEquals(snap, HubSnapshotPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void aSnapshotKeepsAtMostSixtyFourWolves() {
        List<HubSocial.Npc> many = new java.util.ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            many.add(npc(i));
        }
        var snap = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(), List.of(), net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.NONE, many);
        assertEquals(HubSocial.MAX_NPCS, snap.npcs().size());
    }
}
