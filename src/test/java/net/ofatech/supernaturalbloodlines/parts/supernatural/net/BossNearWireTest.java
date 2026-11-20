package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;
import org.junit.jupiter.api.Test;

/** Plan 17: which boss is near (and its name, when the player may know it) rides in the Home snapshot, and is cleaned on the way in. */
class BossNearWireTest {
    private static HubSnapshotPayload snapshot(WorldNowHook.Flags flags) {
        return new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(), List.of(), flags, List.of());
    }

    @Test
    void theKindAndTheNameSurviveTheCodec() {
        var snap = snapshot(new WorldNowHook.Flags(true, false, 1, WorldNowHook.BossNear.FALLEN, "mikael"));
        var buf = Unpooled.buffer();
        HubSnapshotPayload.STREAM_CODEC.encode(buf, snap);
        var back = HubSnapshotPayload.STREAM_CODEC.decode(buf);
        assertEquals(WorldNowHook.BossNear.FALLEN, back.world().bossNear());
        assertEquals("mikael", back.world().bossName());
    }

    @Test
    void aNameForNobodyIsDropped() {
        assertEquals("", new WorldNowHook.Flags(false, false, 0, WorldNowHook.BossNear.NONE_KIND, "mikael").bossName());
    }

    @Test
    void aKindOutsideTheThreeIsClamped() {
        assertEquals(WorldNowHook.BossNear.FALLEN, new WorldNowHook.Flags(false, false, 0, 9, "x").bossNear());
        assertEquals(WorldNowHook.BossNear.NONE_KIND, new WorldNowHook.Flags(false, false, 0, -3, "x").bossNear());
    }

    @Test
    void anOldCallerWithoutABossSeesNone() {
        assertEquals(WorldNowHook.BossNear.NONE_KIND, new WorldNowHook.Flags(true, true, 2).bossNear());
    }
}
