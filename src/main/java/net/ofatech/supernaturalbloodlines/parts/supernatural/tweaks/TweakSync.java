package net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;

/**
 * The cleansing altar is clicked on the client first, so each client must know whether the server has it disabled. The server sends its own value
 * when a player joins and whenever the hub saves the key; the client uses it instead of its own file until it leaves the world. The payload is
 * registered on {@code LadderNet}'s optional channel: a client without it keeps its file.
 */
public final class TweakSync {
    public record Payload(boolean altarDisabled) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "tweak_sync"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Payload::altarDisabled, Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        /** The server's own file, never what another server pushed to it. */
        public static Payload ofLocal() {
            return new Payload(SupernaturalConfig.localTweak(Tweak.CLEANSING_ALTAR_DISABLED));
        }
    }

    private TweakSync() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                send(p);
            }
        });
        AdminSettings.onClientCopySaved(server -> server.execute(() -> server.getPlayerList().getPlayers().forEach(TweakSync::send)));
    }

    /** Sends the server's value to a player whose connection is open and has the channel; a fake player's has neither. Whether it was sent. */
    public static boolean send(ServerPlayer p) {
        if (p.connection != null && p.connection.getConnection().isConnected() && p.connection.hasChannel(Payload.TYPE)) {
            PacketDistributor.sendToPlayer(p, Payload.ofLocal());
            return true;
        }
        return false;
    }

    /** Client side. */
    public static void onClient(Payload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> SupernaturalConfig.applyPushedAltar(payload.altarDisabled()));
    }
}
