package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: the key of a Supernatural-mode slot went down or up. It carries only the slot number and what the crosshair was on; the server reads the
 * action from the player's own loadout, never from here, and checks the target itself. {@code targetEntityId} is -1 for nothing.
 */
public record TriggerActionPayload(int slot, int targetEntityId, boolean release) implements CustomPacketPayload {
    public static final Type<TriggerActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "trigger_action"));

    public static final StreamCodec<ByteBuf, TriggerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TriggerActionPayload::slot,
            ByteBufCodecs.VAR_INT, TriggerActionPayload::targetEntityId,
            ByteBufCodecs.BOOL, TriggerActionPayload::release,
            TriggerActionPayload::new);

    @Override
    public Type<TriggerActionPayload> type() {
        return TYPE;
    }
}
