package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: move a power's mode to the next ({@code delta} +1) or previous (-1) one it may use. Anything else is clamped to its sign. */
public record SetPowerModePayload(String ref, int delta) implements CustomPacketPayload {
    public static final Type<SetPowerModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "set_power_mode"));

    public static final StreamCodec<ByteBuf, SetPowerModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(WheelEntry.MAX_STRING), SetPowerModePayload::ref,
            ByteBufCodecs.VAR_INT, SetPowerModePayload::delta,
            SetPowerModePayload::new);

    @Override
    public Type<SetPowerModePayload> type() {
        return TYPE;
    }
}
