package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.GroupWire;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubGroups;

/**
 * Server to client: the group the player is in, whole (a client replaces its copy). Pushed with the hub snapshot and when a request
 * changes. It carries nothing about people outside the viewer's group; every list and string is bounded and a decode over a bound
 * fails that packet.
 */
public record GroupViewPayload(GroupView view) implements CustomPacketPayload {
    public static final Type<GroupViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "group_view"));

    private static final StreamCodec<ByteBuf, GroupType> GROUP_TYPE = ByteBufCodecs.stringUtf8(GroupWire.MAX_RANK)
            .map(id -> GroupType.parse(id).orElseThrow(() -> new DecoderException("unknown group type " + id)), GroupType::id);
    private static final StreamCodec<ByteBuf, GroupRank> GROUP_RANK = ByteBufCodecs.stringUtf8(GroupWire.MAX_RANK).map(GroupRank::parse, GroupRank::id);

    private static final StreamCodec<ByteBuf, GroupView.Row> ROW = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(GroupWire.MAX_NAME), GroupView.Row::name,
            ByteBufCodecs.stringUtf8(GroupWire.MAX_RANK), GroupView.Row::rank,
            GROUP_RANK, GroupView.Row::groupRank,
            ByteBufCodecs.BOOL, GroupView.Row::online,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), GroupView.Row::id,
            GroupView.Row::new);

    private static final StreamCodec<ByteBuf, GroupView> VIEW = StreamCodec.composite(
            GROUP_TYPE, GroupView::type,
            ByteBufCodecs.BOOL, GroupView::hasGroup,
            ByteBufCodecs.BYTE, GroupView::viewerRank,
            ByteBufCodecs.stringUtf8(GroupWire.MAX_NAME), GroupView::leaderName,
            ByteBufCodecs.VAR_INT, GroupView::sinceDays,
            ROW.apply(ByteBufCodecs.list(GroupWire.MAX_MEMBERS)), GroupView::members,
            GroupView::new);

    public static final StreamCodec<ByteBuf, GroupViewPayload> STREAM_CODEC = VIEW.map(GroupViewPayload::new, GroupViewPayload::view);

    public static GroupViewPayload of(ServerPlayer p) {
        return new GroupViewPayload(HubGroups.of(p));
    }

    @Override
    public Type<GroupViewPayload> type() {
        return TYPE;
    }
}
