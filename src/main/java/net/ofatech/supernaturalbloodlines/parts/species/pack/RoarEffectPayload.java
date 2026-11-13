package net.ofatech.supernaturalbloodlines.parts.species.pack;

import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** Server to client: this player is roaring for {@code ticks}, so their eyes glow ({@code RoarClient}). */
public record RoarEffectPayload(int playerId, int ticks) implements CustomPacketPayload {
    public static final Type<RoarEffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "roar_effect"));
    public static final StreamCodec<ByteBuf, RoarEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RoarEffectPayload::playerId,
            ByteBufCodecs.VAR_INT, RoarEffectPayload::ticks,
            RoarEffectPayload::new);

    private static volatile Consumer<RoarEffectPayload> spy;

    @Override
    public Type<RoarEffectPayload> type() {
        return TYPE;
    }

    /** To the roarer and everyone who sees them. Mock players and clients without the mod have no channel. */
    public static void send(ServerPlayer roarer, RoarEffectPayload payload) {
        Consumer<RoarEffectPayload> s = spy;
        if (s != null) {
            s.accept(payload);
        }
        if (!(roarer instanceof FakePlayer) && roarer.connection != null && roarer.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(roarer, payload);
        }
    }

    /** For tests: sees every send attempt; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(Consumer<RoarEffectPayload> value) {
        spy = value;
    }
}
