package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Every client to server send of the HUD goes through here: nothing is sent without a connection (a key released while the world closes) or to a server
 * that has no channel for the payload (a server without this mod, or with the hud part off). Client side only.
 */
public final class HudSend {
    private HudSend() {
    }

    /** True while there is a connection; the channel of Tensura's and ManasCore's own packets is theirs to check. */
    public static boolean connected() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(type);
    }

    public static void send(CustomPacketPayload payload) {
        if (canSend(payload.type())) {
            PacketDistributor.sendToServer(payload);
        }
    }
}
