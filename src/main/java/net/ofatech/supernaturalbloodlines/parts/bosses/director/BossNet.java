package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The one place the bosses part sends a payload to a player. Both of its payloads are optional (a client without the bosses part can still join), and sending a payload
 * to a connection that did not register it is an error, so a player with no connection (a fake one) or without the channel is simply skipped.
 */
public final class BossNet {
    private BossNet() {
    }

    /** Whether the player's connection knows the payload's channel. Fake players have a stand-in connection that cannot say. */
    public static boolean canReceive(ServerPlayer p, CustomPacketPayload.Type<?> type) {
        return !(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(type);
    }

    /** Sends the payload to a player who can receive it; false (and nothing sent, nothing thrown) for one who cannot. */
    public static boolean send(ServerPlayer p, CustomPacketPayload payload) {
        if (!canReceive(p, payload.type())) {
            return false;
        }
        PacketDistributor.sendToPlayer(p, payload);
        return true;
    }
}
