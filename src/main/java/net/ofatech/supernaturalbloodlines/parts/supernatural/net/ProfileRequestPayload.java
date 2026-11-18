package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Client to server: open the Profile of this person. Carries only who; what the sender may learn is decided by the server's own indexes. */
public record ProfileRequestPayload(UUID subject) implements CustomPacketPayload {
    public static final Type<ProfileRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "profile_request"));
    public static final StreamCodec<ByteBuf, ProfileRequestPayload> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(ProfileRequestPayload::new, ProfileRequestPayload::subject);

    @Override
    public Type<ProfileRequestPayload> type() {
        return TYPE;
    }
}

