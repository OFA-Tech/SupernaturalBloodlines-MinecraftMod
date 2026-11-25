package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/**
 * Server to client: everything that waits for this player's answer, whole (a client replaces its list). Pushed when a request opens or is
 * answered, and when the hub opens; Home reads it, and so does the corner toast. Only what the chat line already said.
 */
public record HubRequestsPayload(List<HubRequests.Request> requests) implements CustomPacketPayload {
    public static final int MAX = 16;
    public static final Type<HubRequestsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hub_requests"));

    private static final StreamCodec<ByteBuf, HubRequests.Kind> KIND = ByteBufCodecs.stringUtf8(16).map(HubRequests.Kind::valueOf, HubRequests.Kind::name);

    private static final StreamCodec<ByteBuf, HubRequests.Request> REQUEST = StreamCodec.composite(
            KIND, HubRequests.Request::kind,
            net.minecraft.core.UUIDUtil.STREAM_CODEC, HubRequests.Request::from,
            ByteBufCodecs.stringUtf8(64), HubRequests.Request::fromName,
            ByteBufCodecs.stringUtf8(64), HubRequests.Request::token,
            ByteBufCodecs.VAR_INT, HubRequests.Request::ticksLeft,
            HubRequests.Request::new);

    public static final StreamCodec<ByteBuf, HubRequestsPayload> STREAM_CODEC = REQUEST.apply(ByteBufCodecs.list(MAX)).map(HubRequestsPayload::new, HubRequestsPayload::requests);

    public HubRequestsPayload {
        requests = List.copyOf(requests.subList(0, Math.min(requests.size(), MAX)));
    }

    public static HubRequestsPayload of(ServerPlayer p) {
        return new HubRequestsPayload(HubRequests.of(p));
    }

    @Override
    public Type<HubRequestsPayload> type() {
        return TYPE;
    }
}
