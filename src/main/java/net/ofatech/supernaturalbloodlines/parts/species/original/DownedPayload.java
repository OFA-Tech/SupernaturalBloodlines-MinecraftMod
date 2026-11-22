package net.ofatech.supernaturalbloodlines.parts.species.original;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Server to client: the player is now downed (open the Give up / Wait screen) or is not any more (close it). {@code mode} is {@code Downed.MODE_DYING} or
 * {@code Downed.MODE_TURNING} and picks the screen's text; {@code ticksLeft} is how long the dying still lasts (0 when closing), for the countdown it shows.
 */
public record DownedPayload(boolean downed, int mode, int ticksLeft) implements CustomPacketPayload {
    public static final Type<DownedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "downed"));
    public static final StreamCodec<ByteBuf, DownedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DownedPayload::downed,
            ByteBufCodecs.VAR_INT, DownedPayload::mode,
            ByteBufCodecs.VAR_INT, DownedPayload::ticksLeft,
            DownedPayload::new);

    @Override
    public Type<DownedPayload> type() {
        return TYPE;
    }
}
