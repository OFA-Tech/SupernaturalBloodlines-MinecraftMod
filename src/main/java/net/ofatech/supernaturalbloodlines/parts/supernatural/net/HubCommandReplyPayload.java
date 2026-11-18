package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Server to client: what the command a {@link HubCommandPayload} ran said (the same lines also went to chat), and whether it worked. */
public record HubCommandReplyPayload(String rowId, boolean success, List<Component> lines) implements CustomPacketPayload {
    public static final int MAX_LINES = 16;
    public static final Type<HubCommandReplyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_command_reply"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HubCommandReplyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(8), HubCommandReplyPayload::rowId,
            ByteBufCodecs.BOOL, HubCommandReplyPayload::success,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)), HubCommandReplyPayload::lines,
            HubCommandReplyPayload::new);

    public HubCommandReplyPayload {
        lines = List.copyOf(lines.subList(0, Math.min(lines.size(), MAX_LINES)));
    }

    @Override
    public Type<HubCommandReplyPayload> type() {
        return TYPE;
    }
}
