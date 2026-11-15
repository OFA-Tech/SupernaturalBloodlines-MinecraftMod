package net.ofatech.supernaturalbloodlines.parts.hud.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;

/** Server to client: the player's own Supernatural-mode loadout. */
public record LoadoutSyncPayload(Loadout loadout) implements CustomPacketPayload {
    public static final Type<LoadoutSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "loadout_sync"));

    public static final StreamCodec<ByteBuf, LoadoutSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodec(Loadout.CODEC).map(LoadoutSyncPayload::new, LoadoutSyncPayload::loadout);

    @Override
    public Type<LoadoutSyncPayload> type() {
        return TYPE;
    }
}
