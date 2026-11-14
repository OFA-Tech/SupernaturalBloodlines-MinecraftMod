package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;

/** The supernatural state of a player, sent to that player only (see {@code SupernaturalState#clientView}). */
public record SupernaturalSyncPayload(SupernaturalState state) implements CustomPacketPayload {
    public static final Type<SupernaturalSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "supernatural_sync"));

    public static final StreamCodec<ByteBuf, SupernaturalSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(SupernaturalState.CODEC).map(SupernaturalSyncPayload::new, SupernaturalSyncPayload::state);

    @Override
    public Type<SupernaturalSyncPayload> type() {
        return TYPE;
    }
}
