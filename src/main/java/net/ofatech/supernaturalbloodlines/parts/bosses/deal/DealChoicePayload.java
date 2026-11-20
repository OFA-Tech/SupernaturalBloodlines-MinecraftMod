package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: the player's answer to Elijah's offer: the position in the list the server sent, or -1 for no deal. The server believes nothing else. */
public record DealChoicePayload(int entityId, int index) implements CustomPacketPayload {
    public static final Type<DealChoicePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "elijah_choice"));
    public static final StreamCodec<ByteBuf, DealChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DealChoicePayload::entityId,
            ByteBufCodecs.VAR_INT, DealChoicePayload::index,
            DealChoicePayload::new);

    @Override
    public Type<DealChoicePayload> type() {
        return TYPE;
    }
}