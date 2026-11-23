package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Server to client: this player has just damaged the boss whose bar has the UUID {@code bar}. The client makes that boss the focus of the boss row (the full card); it is
 * the one fact the client cannot know by itself. Sent only to the player who dealt the damage, and not more than once in {@link HitThrottle#MIN_GAP} ticks per boss.
 */
public record BossHitPayload(UUID bar) implements CustomPacketPayload {
    public static final Type<BossHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "boss_hit"));
    public static final StreamCodec<ByteBuf, BossHitPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, BossHitPayload::bar, BossHitPayload::new);

    @Override
    public Type<BossHitPayload> type() {
        return TYPE;
    }
}
