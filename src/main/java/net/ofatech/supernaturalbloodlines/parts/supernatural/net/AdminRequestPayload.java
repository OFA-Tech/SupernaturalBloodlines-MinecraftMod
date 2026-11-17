package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the Admin page opened and wants an {@link AdminSnapshotPayload}. Answered only for permission level 2 or more. */
public record AdminRequestPayload() implements CustomPacketPayload {
    public static final AdminRequestPayload INSTANCE = new AdminRequestPayload();
    public static final Type<AdminRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "admin_request"));
    public static final StreamCodec<ByteBuf, AdminRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<AdminRequestPayload> type() {
        return TYPE;
    }
}
