package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenView;
import net.ofatech.supernaturalbloodlines.core.group.coven.CovenWire;
import net.ofatech.supernaturalbloodlines.core.group.coven.VoteKind;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubCoven;

/**
 * Server to client: the viewer's own place in a coven, whole (a client replaces its copy). Pushed with the group view and whenever a circle, a call, a
 * consent, a meter or a vote changes for that player. Every list and string is bounded; a decode over a bound fails that packet.
 */
public record CovenViewPayload(CovenView view) implements CustomPacketPayload {
    public static final Type<CovenViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "coven_view"));

    static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(CovenWire.MAX_NAME);
    static final StreamCodec<ByteBuf, java.util.List<String>> NAMES = NAME.apply(ByteBufCodecs.list(CovenWire.MAX_CIRCLE));

    private static final StreamCodec<ByteBuf, CovenView.Circle> CIRCLE = ByteBufCodecs.VAR_INT.map(i -> at(CovenView.Circle.values(), i, "circle"), Enum::ordinal);
    private static final StreamCodec<ByteBuf, CovenView.Ballot> BALLOT = ByteBufCodecs.VAR_INT.map(i -> at(CovenView.Ballot.values(), i, "ballot"), Enum::ordinal);
    private static final StreamCodec<ByteBuf, Consent> CONSENT = ByteBufCodecs.VAR_INT.map(i -> at(Consent.values(), i, "consent"), Enum::ordinal);
    private static final StreamCodec<ByteBuf, VoteKind> KIND = ByteBufCodecs.VAR_INT.map(i -> at(VoteKind.values(), i, "vote kind"), Enum::ordinal);

    private static <T> T at(T[] values, int i, String what) {
        if (i < 0 || i >= values.length) {
            throw new DecoderException("unknown " + what + " " + i);
        }
        return values[i];
    }

    private static final StreamCodec<ByteBuf, CovenView.Vote> VOTE = new StreamCodec<>() {
        @Override
        public CovenView.Vote decode(ByteBuf buf) {
            VoteKind kind = KIND.decode(buf);
            String subject = NAME.decode(buf);
            int yes = ByteBufCodecs.VAR_INT.decode(buf);
            int no = ByteBufCodecs.VAR_INT.decode(buf);
            int electors = ByteBufCodecs.VAR_INT.decode(buf);
            CovenView.Ballot mine = BALLOT.decode(buf);
            return new CovenView.Vote(kind, subject, yes, no, electors, mine, ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, CovenView.Vote v) {
            KIND.encode(buf, v.kind());
            NAME.encode(buf, v.subjectName());
            ByteBufCodecs.VAR_INT.encode(buf, v.yes());
            ByteBufCodecs.VAR_INT.encode(buf, v.no());
            ByteBufCodecs.VAR_INT.encode(buf, v.electors());
            BALLOT.encode(buf, v.mine());
            ByteBufCodecs.VAR_INT.encode(buf, v.hoursLeft());
        }
    };

    private static final StreamCodec<ByteBuf, CovenView> VIEW = new StreamCodec<>() {
        @Override
        public CovenView decode(ByteBuf buf) {
            int tier = ByteBufCodecs.VAR_INT.decode(buf);
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            CovenView.Circle circle = CIRCLE.decode(buf);
            String anchor = NAME.decode(buf);
            java.util.List<String> names = NAMES.decode(buf);
            int meter = ByteBufCodecs.VAR_INT.decode(buf);
            int cap = ByteBufCodecs.VAR_INT.decode(buf);
            int regen = ByteBufCodecs.VAR_INT.decode(buf);
            Consent consent = CONSENT.decode(buf);
            int power = ByteBufCodecs.VAR_INT.decode(buf);
            long ep = ByteBufCodecs.VAR_LONG.decode(buf);
            boolean known = ByteBufCodecs.BOOL.decode(buf);
            int shun = ByteBufCodecs.VAR_INT.decode(buf);
            java.util.Optional<CovenView.Vote> vote = ByteBufCodecs.optional(VOTE).decode(buf);
            return new CovenView(tier, size, circle, anchor, names, meter, cap, regen, consent, power, ep, known, shun, vote);
        }

        @Override
        public void encode(ByteBuf buf, CovenView v) {
            ByteBufCodecs.VAR_INT.encode(buf, v.tier());
            ByteBufCodecs.VAR_INT.encode(buf, v.size());
            CIRCLE.encode(buf, v.circle());
            NAME.encode(buf, v.anchorName());
            NAMES.encode(buf, v.circleNames());
            ByteBufCodecs.VAR_INT.encode(buf, v.meter());
            ByteBufCodecs.VAR_INT.encode(buf, v.meterCap());
            ByteBufCodecs.VAR_INT.encode(buf, v.regenSeconds());
            CONSENT.encode(buf, v.consent());
            ByteBufCodecs.VAR_INT.encode(buf, v.sharePower());
            ByteBufCodecs.VAR_LONG.encode(buf, v.shareEp());
            ByteBufCodecs.BOOL.encode(buf, v.shareKnown());
            ByteBufCodecs.VAR_INT.encode(buf, v.shunnedDaysLeft());
            ByteBufCodecs.optional(VOTE).encode(buf, v.vote());
        }
    };

    public static final StreamCodec<ByteBuf, CovenViewPayload> STREAM_CODEC = VIEW.map(CovenViewPayload::new, CovenViewPayload::view);

    public static CovenViewPayload of(ServerPlayer p) {
        return new CovenViewPayload(HubCoven.of(p));
    }

    @Override
    public Type<CovenViewPayload> type() {
        return TYPE;
    }
}