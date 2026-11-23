package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the wheel screen opened ({@code open}) or closed. Only a viewer is sent the catalogue and its changes. */
public record WheelViewPayload(boolean open) implements CustomPacketPayload {
    public static final Type<WheelViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "wheel_view"));

    public static final StreamCodec<ByteBuf, WheelViewPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(WheelViewPayload::new, WheelViewPayload::open);

    @Override
    public Type<WheelViewPayload> type() {
        return TYPE;
    }
}
