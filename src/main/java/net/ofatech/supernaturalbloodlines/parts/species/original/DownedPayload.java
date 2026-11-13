package net.ofatech.supernaturalbloodlines.parts.species.original;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Server to client: the player is now downed (open the Give up / Wait screen) or is not any more (close it). */
public record DownedPayload(boolean downed) implements CustomPacketPayload {
    public static final Type<DownedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "downed"));
    public static final StreamCodec<ByteBuf, DownedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DownedPayload::downed,
            DownedPayload::new);

    @Override
    public Type<DownedPayload> type() {
        return TYPE;
    }
}
