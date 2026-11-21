package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

/** The facts of a rite the hub draws (its chalk, what it sacrifices, the full moon, the tick power) reach the client intact. */
class RiteWireTest {
    @Test
    void aRiteKeepsItsChalkSacrificesMoonAndTickPower() {
        var rite = new HubSocial.Rite("supernaturalbloodlines:rite_of_the_moon", 15000, List.of("1 supernaturalbloodlines:moonstone"),
                List.of("enchanted:medium_circle=enchanted:nether_chalk"), "supernaturalbloodlines:faction_rite", "vampire_or_werewolf",
                List.of("minecraft:wolf", "minecraft:villager"), true, 3);
        var snap = new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, Map.of(), List.of(rite));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HubSnapshotPayload.STREAM_CODEC.encode(buf, snap);
        HubSocial.Rite back = HubSnapshotPayload.STREAM_CODEC.decode(buf).rites().get(0);
        assertEquals(rite, back);
        assertEquals("nether_chalk", back.circles().get(0).chalk());
        assertEquals(List.of("minecraft:wolf", "minecraft:villager"), back.sacrifices());
        assertEquals(true, back.fullMoon());
        assertEquals(3, back.tickPower());
    }
}
