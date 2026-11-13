package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
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

/**
 * Entity ids to outline for {@link #TICKS} ticks, on the receiving client only (the Hunter's vampire sense). The server
 * never marks the entities as glowing, so nobody else sees it; see {@code HunterSenseClient}.
 */
public record GlowPayload(List<Integer> ids) implements CustomPacketPayload {
    public static final int TICKS = 40;
    private static final int MAX_IDS = 256;

    public static final Type<GlowPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "glow"));

    public static final StreamCodec<ByteBuf, GlowPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(MAX_IDS))
            .map(GlowPayload::new, GlowPayload::ids);

    private static volatile BiConsumer<ServerPlayer, List<Integer>> spy;

    @Override
    public Type<GlowPayload> type() {
        return TYPE;
    }

    /** Sends the first {@value #MAX_IDS} of {@code ids} to {@code p}; false (nothing sent) for mock players and clients without the mod. */
    public static boolean send(ServerPlayer p, List<Integer> ids) {
        BiConsumer<ServerPlayer, List<Integer>> s = spy;
        if (s != null) {
            s.accept(p, ids);
        }
        if (p instanceof FakePlayer || p.connection == null || !p.connection.hasChannel(TYPE)) {
            return false;
        }
        PacketDistributor.sendToPlayer(p, new GlowPayload(ids.size() > MAX_IDS ? ids.subList(0, MAX_IDS) : ids));
        return true;
    }

    /** For tests: sees every send attempt, whether or not the player can receive it; null clears it. */
    @VisibleForTesting
    public static void overrideSpy(BiConsumer<ServerPlayer, List<Integer>> value) {
        spy = value;
    }
}
