package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Client to server: run one of our commands by its row id in {@code AdminCommands} (for example {@code P4} with the member's name). The server
 * rebuilds the typed command from the row and the checked arguments and runs it as the sender, so Brigadier's own permission and rules apply
 * and no rule is written twice.
 */
public record HubCommandPayload(String rowId, List<String> args) implements CustomPacketPayload {
    public static final int MAX_ARGS = 4;
    public static final Type<HubCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_command"));

    public static final StreamCodec<ByteBuf, HubCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(8), HubCommandPayload::rowId,
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(MAX_ARGS)), HubCommandPayload::args,
            HubCommandPayload::new);

    public HubCommandPayload {
        args = List.copyOf(args);
    }

    @Override
    public Type<HubCommandPayload> type() {
        return TYPE;
    }
}
