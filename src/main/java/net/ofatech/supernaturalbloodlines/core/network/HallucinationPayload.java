package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.VisibleForTesting;

/** One hallucination for the receiving player only (the Hunter's Curse, werewolf venom). The client plays it; see {@code HallucinationClient}. */
public record HallucinationPayload(Kind kind) implements CustomPacketPayload {
    public enum Kind {
        /** A zombie only this player sees, for about 5 seconds. */
        PHANTOM,
        /** A sound from nowhere. */
        WHISPER,
        /** The screen goes dark for a moment. */
        DARKNESS;

        public static Kind random(RandomSource random) {
            return values()[random.nextInt(values().length)];
        }
    }

    public static final Type<HallucinationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hallucination"));

    public static final StreamCodec<ByteBuf, HallucinationPayload> STREAM_CODEC = ByteBufCodecs
            .idMapper((int id) -> Kind.values()[id], Kind::ordinal)
            .map(HallucinationPayload::new, HallucinationPayload::kind);

    private static volatile BiConsumer<ServerPlayer, Kind> spy;

    @Override
    public Type<HallucinationPayload> type() {
        return TYPE;
    }

    /**
     * Sends {@code kind} to {@code p}. Mock players and clients without the mod have no channel (sending would throw),
     * so it returns false and the caller may use a server-side stand-in.
     */
    public static boolean send(ServerPlayer p, Kind kind) {
        BiConsumer<ServerPlayer, Kind> s = spy;
        if (s != null) {
            s.accept(p, kind);
        }
        if (p instanceof FakePlayer || p.connection == null || !p.connection.hasChannel(TYPE)) {
            return false;
        }
        PacketDistributor.sendToPlayer(p, new HallucinationPayload(kind));
        return true;
    }

    /** For tests: sees every send attempt, whether or not the player can receive it; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(BiConsumer<ServerPlayer, Kind> value) {
        spy = value;
    }
}
