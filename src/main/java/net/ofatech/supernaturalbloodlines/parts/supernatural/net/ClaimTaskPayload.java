package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: claim the reward of a done task. The server decides everything; the id is only a name. */
public record ClaimTaskPayload(String taskId) implements CustomPacketPayload {
    public static final Type<ClaimTaskPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "claim_task"));

    public static final StreamCodec<ByteBuf, ClaimTaskPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(64).map(ClaimTaskPayload::new, ClaimTaskPayload::taskId);

    @Override
    public Type<ClaimTaskPayload> type() {
        return TYPE;
    }
}
