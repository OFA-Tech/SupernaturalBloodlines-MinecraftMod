package net.ofatech.supernaturalbloodlines.parts.species.original;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the downed player chose "Give up". "Wait" sends nothing. */
public record DownedGiveUpPayload() implements CustomPacketPayload {
    public static final DownedGiveUpPayload INSTANCE = new DownedGiveUpPayload();
    public static final Type<DownedGiveUpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "give_up"));
    public static final StreamCodec<ByteBuf, DownedGiveUpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<DownedGiveUpPayload> type() {
        return TYPE;
    }
}
