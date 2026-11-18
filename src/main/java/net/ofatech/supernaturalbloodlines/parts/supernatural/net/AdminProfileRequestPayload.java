package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server, from the Admin page only: the OP view of a Profile. Answered only when the server's own op list gives the sender level 2. */
public record AdminProfileRequestPayload(UUID subject) implements CustomPacketPayload {
    public static final Type<AdminProfileRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "admin_profile_request"));
    public static final StreamCodec<ByteBuf, AdminProfileRequestPayload> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(AdminProfileRequestPayload::new, AdminProfileRequestPayload::subject);

    @Override
    public Type<AdminProfileRequestPayload> type() {
        return TYPE;
    }
}

