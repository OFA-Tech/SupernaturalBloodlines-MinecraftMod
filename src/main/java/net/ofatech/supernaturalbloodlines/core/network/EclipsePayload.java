package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** Whether a lunar eclipse is running, for the client's red sky and fog; see {@code EclipseClient}. */
public record EclipsePayload(boolean active) implements CustomPacketPayload {
    public static final Type<EclipsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "eclipse"));

    public static final StreamCodec<ByteBuf, EclipsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EclipsePayload::active,
            EclipsePayload::new);

    private static volatile BiConsumer<ServerPlayer, EclipsePayload> spy;

    @Override
    public Type<EclipsePayload> type() {
        return TYPE;
    }

    /** Mock players and clients without the mod have no channel (sending would throw), so they are skipped. */
    public static void send(ServerPlayer to, EclipsePayload payload) {
        BiConsumer<ServerPlayer, EclipsePayload> s = spy;
        if (s != null) {
            s.accept(to, payload);
        }
        if (!(to instanceof FakePlayer) && to.connection != null && to.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(to, payload);
        }
    }

    /** For tests: sees every send attempt, whether or not the player can receive it; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(BiConsumer<ServerPlayer, EclipsePayload> value) {
        spy = value;
    }
}
