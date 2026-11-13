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
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.jetbrains.annotations.VisibleForTesting;

/** The eye colour of one werewolf player (R91), for the client's glowing eyes; see {@code EyeColorClient}. */
public record EyeColorPayload(int playerId, Color color) implements CustomPacketPayload {
    public enum Color {
        GOLD, BLUE, RED;

        /** Alphas have red eyes; anyone else's turn blue for good after an innocent kill, else gold. */
        public static Color of(WolfState w) {
            if (w.rank() == WolfState.Rank.ALPHA || w.rank() == WolfState.Rank.TRUE_ALPHA) {
                return RED;
            }
            return w.killedInnocent() ? BLUE : GOLD;
        }
    }

    public static final Type<EyeColorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "eye_color"));

    public static final StreamCodec<ByteBuf, EyeColorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EyeColorPayload::playerId,
            ByteBufCodecs.idMapper((int id) -> Color.values()[id], Color::ordinal), EyeColorPayload::color,
            EyeColorPayload::new);

    private static volatile BiConsumer<ServerPlayer, EyeColorPayload> spy;

    @Override
    public Type<EyeColorPayload> type() {
        return TYPE;
    }

    /** Mock players and clients without the mod have no channel (sending would throw), so they are skipped. */
    public static void send(ServerPlayer to, EyeColorPayload payload) {
        BiConsumer<ServerPlayer, EyeColorPayload> s = spy;
        if (s != null) {
            s.accept(to, payload);
        }
        if (!(to instanceof FakePlayer) && to.connection != null && to.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(to, payload);
        }
    }

    /** For tests: sees every send attempt, whether or not the player can receive it; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(BiConsumer<ServerPlayer, EyeColorPayload> value) {
        spy = value;
    }
}
