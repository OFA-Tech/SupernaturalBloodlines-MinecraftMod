package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;

/** The bloodline state of one player, sent to that player and to everyone tracking them. */
public record BloodlineSyncPayload(int entityId, BloodlineState state) implements CustomPacketPayload {
    public static final Type<BloodlineSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bloodline_sync"));

    public static final StreamCodec<ByteBuf, BloodlineSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BloodlineSyncPayload::entityId,
            ByteBufCodecs.fromCodec(BloodlineState.CODEC), BloodlineSyncPayload::state,
            BloodlineSyncPayload::new);

    @Override
    public Type<BloodlineSyncPayload> type() {
        return TYPE;
    }
}
