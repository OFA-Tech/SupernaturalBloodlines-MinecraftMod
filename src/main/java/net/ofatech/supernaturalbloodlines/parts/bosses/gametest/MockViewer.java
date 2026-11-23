package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

/**
 * A real (not fake) player whose connection records what is sent to it and says whether it knows the payload channels: what a client with and without the mod
 * looks like to the server. It never fires the login event ({@code makeMockServerPlayerInLevel} does, and other mods then send packets to its mock connection).
 */
final class MockViewer {
    final ServerPlayer player;
    final List<Packet<?>> sent = new ArrayList<>();

    private MockViewer(ServerPlayer player) {
        this.player = player;
    }

    static MockViewer of(GameTestHelper helper, boolean hasChannels) {
        var server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "viewer-" + id.toString().substring(0, 8));
        ServerPlayer p = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        MockViewer v = new MockViewer(p);
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), p, new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false)) {
            @Override
            public void send(Packet<?> packet) {
                v.sent.add(packet);
            }

            @Override
            public boolean hasChannel(CustomPacketPayload.Type<?> type) {
                return hasChannels;
            }
        };
        p.setPos(helper.absoluteVec(new Vec3(3.5, 1, 3.5)));
        return v;
    }

    /** The payloads of the given kind that were sent, in order. */
    <T extends CustomPacketPayload> List<T> payloads(Class<T> kind) {
        List<T> out = new ArrayList<>();
        for (Packet<?> packet : sent) {
            if (packet instanceof ClientboundCustomPayloadPacket c && kind.isInstance(c.payload())) {
                out.add(kind.cast(c.payload()));
            }
        }
        return out;
    }

    /** Where in the list of everything sent the first packet of that class is, or -1. */
    int indexOf(Class<?> packetClass) {
        for (int i = 0; i < sent.size(); i++) {
            if (packetClass.isInstance(sent.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
