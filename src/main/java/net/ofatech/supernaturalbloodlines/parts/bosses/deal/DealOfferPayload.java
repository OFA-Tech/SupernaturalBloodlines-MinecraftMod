package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Server to client: Elijah offers this player a deal. {@code originals} are the ids of the Originals whose blood he may give, in order: the server
 * has already left out the one whose line the player is in when they have not discovered it, so nothing here is hidden knowledge. {@code days} is the oath.
 */
public record DealOfferPayload(int entityId, int days, List<String> originals) implements CustomPacketPayload {
    public static final Type<DealOfferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "elijah_offer"));
    public static final StreamCodec<ByteBuf, DealOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DealOfferPayload::entityId,
            ByteBufCodecs.VAR_INT, DealOfferPayload::days,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), DealOfferPayload::originals,
            DealOfferPayload::new);

    @Override
    public Type<DealOfferPayload> type() {
        return TYPE;
    }
}