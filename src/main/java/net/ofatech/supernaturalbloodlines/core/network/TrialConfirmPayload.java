package net.ofatech.supernaturalbloodlines.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: the player pressed "take the spark" in the True Alpha warning of the Alpha Trial. It carries nothing: the server only believes it
 * if it sent the warning a moment ago ({@code TrialConfirmLedger}), and then lets the rite start within two minutes.
 */
public record TrialConfirmPayload() implements CustomPacketPayload {
    public static final TrialConfirmPayload INSTANCE = new TrialConfirmPayload();
    public static final Type<TrialConfirmPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "trial_confirm"));
    public static final StreamCodec<ByteBuf, TrialConfirmPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<TrialConfirmPayload> type() {
        return TYPE;
    }
}
