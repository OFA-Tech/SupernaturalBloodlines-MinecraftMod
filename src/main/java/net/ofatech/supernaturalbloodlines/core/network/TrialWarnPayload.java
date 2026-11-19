package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Server to client: the Alpha Trial was refused once because the player's path to True Alpha is still open and winning would end it. The hub opens
 * on the Rites page with the True Alpha warning; the answer is a {@link TrialConfirmPayload}. It carries nothing.
 */
public record TrialWarnPayload() implements CustomPacketPayload {
    public static final TrialWarnPayload INSTANCE = new TrialWarnPayload();
    public static final Type<TrialWarnPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "trial_warn"));
    public static final StreamCodec<ByteBuf, TrialWarnPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<TrialWarnPayload> type() {
        return TYPE;
    }
}
