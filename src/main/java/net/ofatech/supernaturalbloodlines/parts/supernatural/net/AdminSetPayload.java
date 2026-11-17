package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingValue;

/** Client to server: change one setting. The id is looked up in the allow-list and the value checked against the spec; nothing here is trusted. */
public record AdminSetPayload(String id, SettingValue value) implements CustomPacketPayload {
    public static final Type<AdminSetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "admin_set"));

    public static final StreamCodec<ByteBuf, AdminSetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), AdminSetPayload::id,
            SettingValue.STREAM_CODEC, AdminSetPayload::value,
            AdminSetPayload::new);

    @Override
    public Type<AdminSetPayload> type() {
        return TYPE;
    }
}
