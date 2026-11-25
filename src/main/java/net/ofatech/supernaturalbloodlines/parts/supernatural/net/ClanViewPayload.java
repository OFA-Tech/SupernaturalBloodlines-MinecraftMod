package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanWire;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubClan;

/**
 * Server to client: the viewer's own clan card, whole (a client replaces its copy). Every number and name is bounded; a decode over a bound fails that
 * packet. It carries no id of any person.
 */
public record ClanViewPayload(ClanView view) implements CustomPacketPayload {
    public static final Type<ClanViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "clan_view"));

    static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(ClanWire.MAX_NAME);

    private static int bounded(ByteBuf buf, int max, String what) {
        int v = ByteBufCodecs.VAR_INT.decode(buf);
        if (v < 0 || v > max) {
            throw new DecoderException(what + " " + v + " is out of range");
        }
        return v;
    }

    private static byte small(ByteBuf buf, int max, String what) {
        return (byte) bounded(buf, max, what);
    }

    private static void put(ByteBuf buf, int v) {
        ByteBufCodecs.VAR_INT.encode(buf, v);
    }

    private static final StreamCodec<ByteBuf, ClanView> VIEW = new StreamCodec<>() {
        @Override
        public ClanView decode(ByteBuf buf) {
            boolean in = buf.readBoolean();
            int days = bounded(buf, ClanWire.MAX_DAYS, "days");
            int auto = bounded(buf, ClanWire.MAX_DAYS + 1, "auto") - 1;
            ClanView.Charter charter = new ClanView.Charter(small(buf, ClanView.CHARTER_INERT, "charter state"), NAME.decode(buf), buf.readBoolean(), buf.readBoolean(),
                    bounded(buf, ClanWire.MAX_SECONDS, "reforge seconds"));
            ClanView.Contest contest = new ClanView.Contest(small(buf, ClanView.CONTEST_RUNNING, "contest phase"), NAME.decode(buf), bounded(buf, ClanWire.MAX_SECONDS, "seconds"),
                    bounded(buf, ClanWire.MAX_DAYS, "cooldown"), buf.readBoolean(), bounded(buf, ContestRules.Refusal.values().length, "why"), buf.readBoolean());
            boolean feedOn = buf.readBoolean();
            return new ClanView(in, days, auto, charter, contest, feedOn, bounded(buf, ClanWire.MAX_PERCENT, "feed percent"), bounded(buf, ClanWire.MAX_PERCENT, "bonus percent"));
        }

        @Override
        public void encode(ByteBuf buf, ClanView v) {
            buf.writeBoolean(v.inClan());
            put(buf, v.daysInClan());
            put(buf, v.autoPromoteDaysLeft() + 1);
            put(buf, v.charter().state());
            NAME.encode(buf, v.charter().holderName());
            buf.writeBoolean(v.charter().mine());
            buf.writeBoolean(v.charter().forgeable());
            put(buf, v.charter().reforgeSecondsLeft());
            put(buf, v.contest().phase());
            NAME.encode(buf, v.contest().challengerName());
            put(buf, v.contest().secondsLeft());
            put(buf, v.contest().waitDaysLeft());
            buf.writeBoolean(v.contest().iMayChallenge());
            put(buf, v.contest().challengeWhy());
            buf.writeBoolean(v.contest().iMustAccept());
            buf.writeBoolean(v.feedOn());
            put(buf, v.feedPercent());
            put(buf, v.bonusPercent());
        }
    };

    public static final StreamCodec<ByteBuf, ClanViewPayload> STREAM_CODEC = VIEW.map(ClanViewPayload::new, ClanViewPayload::view);

    public static ClanViewPayload of(ServerPlayer p) {
        return new ClanViewPayload(HubClan.of(p));
    }

    @Override
    public Type<ClanViewPayload> type() {
        return TYPE;
    }
}
