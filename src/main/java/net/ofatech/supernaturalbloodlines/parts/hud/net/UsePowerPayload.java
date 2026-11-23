package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: use a power from the wheel (or end it, {@code release}). {@code ref} is a {@code PowerRef} key; the server looks the power up in what the
 * player really has, so the text is only a name. {@code mode} is the mode to run (the server clamps it); {@code targetEntityId} is -1 for nothing.
 */
public record UsePowerPayload(String ref, int mode, int targetEntityId, boolean release) implements CustomPacketPayload {
    public static final Type<UsePowerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "use_power"));

    public static final StreamCodec<ByteBuf, UsePowerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(WheelEntry.MAX_STRING), UsePowerPayload::ref,
            ByteBufCodecs.VAR_INT, UsePowerPayload::mode,
            ByteBufCodecs.VAR_INT, UsePowerPayload::targetEntityId,
            ByteBufCodecs.BOOL, UsePowerPayload::release,
            UsePowerPayload::new);

    @Override
    public Type<UsePowerPayload> type() {
        return TYPE;
    }
}
