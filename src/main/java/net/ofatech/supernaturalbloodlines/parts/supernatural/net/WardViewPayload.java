package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;

/**
 * Server to client: the wards the player owns and the names of the owners who invited them, whole (a client replaces what it had). Pushed when the hub
 * opens and when a ward or an invitation changes. Nothing about anybody else's ward.
 */
public record WardViewPayload(WardHub.View view) implements CustomPacketPayload {
    public static final Type<WardViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ward_view"));

    private static final StreamCodec<ByteBuf, WardHub.Member> MEMBER = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WardHub.Member::id,
            ByteBufCodecs.stringUtf8(WardHub.MAX_NAME * 4), WardHub.Member::name,
            WardHub.Member::new);

    private static final StreamCodec<ByteBuf, WardHub.Own> OWN = new StreamCodec<>() {
        @Override
        public WardHub.Own decode(ByteBuf buf) {
            String id = ByteBufCodecs.stringUtf8(8).decode(buf);
            boolean door = ByteBufCodecs.BOOL.decode(buf);
            boolean fallen = ByteBufCodecs.BOOL.decode(buf);
            String dimension = ByteBufCodecs.stringUtf8(WardHub.MAX_DIMENSION).decode(buf);
            int x = ByteBufCodecs.VAR_INT.decode(buf);
            int y = ByteBufCodecs.VAR_INT.decode(buf);
            int z = ByteBufCodecs.VAR_INT.decode(buf);
            List<WardHub.Member> invited = MEMBER.apply(ByteBufCodecs.list(WardHub.MAX_INVITED)).decode(buf);
            return new WardHub.Own(id, door, fallen, dimension, x, y, z, invited);
        }

        @Override
        public void encode(ByteBuf buf, WardHub.Own own) {
            ByteBufCodecs.stringUtf8(8).encode(buf, own.id());
            ByteBufCodecs.BOOL.encode(buf, own.door());
            ByteBufCodecs.BOOL.encode(buf, own.fallen());
            ByteBufCodecs.stringUtf8(WardHub.MAX_DIMENSION).encode(buf, own.dimension());
            ByteBufCodecs.VAR_INT.encode(buf, own.x());
            ByteBufCodecs.VAR_INT.encode(buf, own.y());
            ByteBufCodecs.VAR_INT.encode(buf, own.z());
            MEMBER.apply(ByteBufCodecs.list(WardHub.MAX_INVITED)).encode(buf, own.invited());
        }
    };

    public static final StreamCodec<ByteBuf, WardViewPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public WardViewPayload decode(ByteBuf buf) {
            List<WardHub.Own> own = new ArrayList<>(OWN.apply(ByteBufCodecs.list(WardHub.MAX_OWN)).decode(buf));
            List<String> welcome = ByteBufCodecs.stringUtf8(WardHub.MAX_NAME * 4).apply(ByteBufCodecs.list(WardHub.MAX_WELCOME)).decode(buf);
            return new WardViewPayload(new WardHub.View(own, welcome));
        }

        @Override
        public void encode(ByteBuf buf, WardViewPayload payload) {
            OWN.apply(ByteBufCodecs.list(WardHub.MAX_OWN)).encode(buf, payload.view().own());
            ByteBufCodecs.stringUtf8(WardHub.MAX_NAME * 4).apply(ByteBufCodecs.list(WardHub.MAX_WELCOME)).encode(buf, payload.view().welcomedBy());
        }
    };

    public static WardViewPayload of(ServerPlayer p) {
        return new WardViewPayload(WardHub.of(p));
    }

    @Override
    public Type<WardViewPayload> type() {
        return TYPE;
    }
}
