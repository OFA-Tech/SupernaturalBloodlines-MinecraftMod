package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodView;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.BrotherhoodWire;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubBrotherhood;

/** Server to client: the viewer's own brotherhood, whole (a client replaces its copy). Booleans and bounded numbers only; a decode over a bound fails that packet. */
public record BrotherhoodViewPayload(BrotherhoodView view) implements CustomPacketPayload {
    public static final Type<BrotherhoodViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "brotherhood_view"));

    private static int bounded(ByteBuf buf, int max, String what) {
        int v = ByteBufCodecs.VAR_INT.decode(buf);
        if (v < 0 || v > max) {
            throw new DecoderException(what + " " + v + " is out of range");
        }
        return v;
    }

    private static void put(ByteBuf buf, int v) {
        ByteBufCodecs.VAR_INT.encode(buf, v);
    }

    private static final StreamCodec<ByteBuf, BrotherhoodView> VIEW = new StreamCodec<>() {
        @Override
        public BrotherhoodView decode(ByteBuf buf) {
            boolean in = buf.readBoolean();
            boolean fits = buf.readBoolean();
            boolean perks = buf.readBoolean();
            int own = bounded(buf, BrotherhoodWire.MAX_MARK, "own mark");
            int leader = bounded(buf, BrotherhoodWire.MAX_MARK, "leader mark");
            boolean anyMax = buf.readBoolean();
            int brothers = bounded(buf, BrotherhoodWire.MAX_COUNT, "brothers");
            int percent = bounded(buf, BrotherhoodWire.MAX_PERCENT, "percent");
            boolean sight = buf.readBoolean();
            boolean code = buf.readBoolean();
            int strikes = bounded(buf, BrotherhoodWire.MAX_STRIKES, "strikes");
            int limit = bounded(buf, BrotherhoodWire.MAX_STRIKES, "strike limit");
            int seconds = bounded(buf, BrotherhoodWire.MAX_SECONDS, "breaker seconds");
            boolean lead = buf.readBoolean();
            return new BrotherhoodView(in, fits, perks, own, leader, anyMax, brothers, percent, sight, code, strikes, limit, seconds, lead);
        }

        @Override
        public void encode(ByteBuf buf, BrotherhoodView v) {
            buf.writeBoolean(v.inBrotherhood());
            buf.writeBoolean(v.fits());
            buf.writeBoolean(v.perksOn());
            put(buf, v.ownMark());
            put(buf, v.leaderMark());
            buf.writeBoolean(v.anyHasMax());
            put(buf, v.armsBrothers());
            put(buf, v.armsPercent());
            buf.writeBoolean(v.sightOn());
            buf.writeBoolean(v.codeOn());
            put(buf, v.strikes());
            put(buf, v.strikeLimit());
            put(buf, v.breakerSecondsLeft());
            buf.writeBoolean(v.leadByMark());
        }
    };

    public static final StreamCodec<ByteBuf, BrotherhoodViewPayload> STREAM_CODEC = VIEW.map(BrotherhoodViewPayload::new, BrotherhoodViewPayload::view);

    public static BrotherhoodViewPayload of(ServerPlayer p) {
        return new BrotherhoodViewPayload(HubBrotherhood.of(p));
    }

    @Override
    public Type<BrotherhoodViewPayload> type() {
        return TYPE;
    }
}