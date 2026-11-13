package net.ofatech.supernaturalbloodlines.parts.species.pack;

import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
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

    private static volatile BiConsumer<ServerPlayer, RoarEffectPayload> spy;

    @Override
    public Type<RoarEffectPayload> type() {
        return TYPE;
    }

    /**
     * To the roarer and to every player watching them, one by one: a client without the mod has no channel (sending would
     * throw), so each recipient is checked, not only the roarer.
     */
    public static void send(ServerPlayer roarer, RoarEffectPayload payload) {
        sendOne(roarer, payload);
        for (ServerPlayer tracker : ((ServerLevel) roarer.level()).getChunkSource().chunkMap.getPlayersWatching(roarer)) {
            if (tracker != roarer) {
                sendOne(tracker, payload);
            }
        }
    }

    private static void sendOne(ServerPlayer to, RoarEffectPayload payload) {
        BiConsumer<ServerPlayer, RoarEffectPayload> s = spy;
        if (s != null) {
            s.accept(to, payload);
        }
        if (!(to instanceof FakePlayer) && to.connection != null && to.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(to, payload);
        }
    }

    /** For tests: sees every send attempt, whether or not the player can receive it; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(BiConsumer<ServerPlayer, RoarEffectPayload> value) {
        spy = value;
    }
}
