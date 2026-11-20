package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: the player pressed the Alpha Roar key. It carries nothing: the server decides from its own state
 * whether this player may roar and whether the cooldown is over, never from anything the client says.
 */
public record RoarPayload() implements CustomPacketPayload {
    public static final RoarPayload INSTANCE = new RoarPayload();
    public static final Type<RoarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "alpha_roar"));
    public static final StreamCodec<ByteBuf, RoarPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<RoarPayload> type() {
        return TYPE;
    }
}
