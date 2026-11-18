package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the hub opened or closed. Only decides how long a compulsion choice waits for this player; it grants nothing. */
public record HubOpenPayload(boolean open) implements CustomPacketPayload {
    public static final Type<HubOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_open"));
    public static final StreamCodec<ByteBuf, HubOpenPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(HubOpenPayload::new, HubOpenPayload::open);

    @Override
    public Type<HubOpenPayload> type() {
        return TYPE;
    }
}
