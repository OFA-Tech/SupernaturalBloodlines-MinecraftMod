package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the hub opened and wants a {@link HubSnapshotPayload}. It carries nothing; the server answers for the sender only. */
public record HubSnapshotRequestPayload() implements CustomPacketPayload {
    public static final HubSnapshotRequestPayload INSTANCE = new HubSnapshotRequestPayload();
    public static final Type<HubSnapshotRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_snapshot_request"));
    public static final StreamCodec<ByteBuf, HubSnapshotRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<HubSnapshotRequestPayload> type() {
        return TYPE;
    }
}
