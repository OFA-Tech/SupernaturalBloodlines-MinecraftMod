package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: put an action in a slot of the Supernatural-mode loadout. An empty {@code actionId} clears the slot. The server decides what may be assigned. */
public record SetLoadoutPayload(int slot, String actionId) implements CustomPacketPayload {
    public static final Type<SetLoadoutPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "set_loadout"));

    public static final StreamCodec<ByteBuf, SetLoadoutPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetLoadoutPayload::slot,
            ByteBufCodecs.stringUtf8(128), SetLoadoutPayload::actionId,
            SetLoadoutPayload::new);

    @Override
    public Type<SetLoadoutPayload> type() {
        return TYPE;
    }
}
