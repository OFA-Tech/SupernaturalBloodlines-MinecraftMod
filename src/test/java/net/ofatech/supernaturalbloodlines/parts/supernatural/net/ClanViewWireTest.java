package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanWire;
import org.junit.jupiter.api.Test;

class ClanViewWireTest {
    private static ClanView roundTrip(ClanView v) {
        ByteBuf buf = Unpooled.buffer();
        ClanViewPayload.STREAM_CODEC.encode(buf, new ClanViewPayload(v));
        ClanView back = ClanViewPayload.STREAM_CODEC.decode(buf).view();
        assertEquals(0, buf.readableBytes(), "bytes were left over");
        return back;
    }

    private static ClanView sample() {
        return new ClanView(true, 20, -1, new ClanView.Charter(ClanView.CHARTER_HELD, "Esther", false, false, 0),
                new ClanView.Contest(ClanView.CONTEST_PENDING, "Rebekah", 540, 6, false, 0, true), true, 10, 9);
    }

    @Test
    void roundTrip() {
        assertEquals(sample(), roundTrip(sample()));
        ClanView lost = new ClanView(true, 3, 11, new ClanView.Charter(ClanView.CHARTER_LOST, "", true, true, 3600),
                new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 2, false, 3, false), false, 0, 0);
        assertEquals(lost, roundTrip(lost));
    }

    @Test
    void anEmptyViewRoundTrips() {
        assertEquals(ClanView.NONE, roundTrip(ClanView.NONE));
        assertTrue(!ClanView.NONE.inClan() && ClanView.NONE.autoPromoteDaysLeft() == -1);
    }

    @Test
    void aLongNameIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ClanViewPayload.NAME.encode(Unpooled.buffer(), "ok");
        assertThrows(io.netty.handler.codec.EncoderException.class, () -> ClanViewPayload.NAME.encode(buf, "x".repeat(ClanWire.MAX_NAME + 1)));
        ByteBuf wire = Unpooled.buffer();
        net.minecraft.network.codec.ByteBufCodecs.stringUtf8(200).encode(wire, "y".repeat(200));
        assertThrows(DecoderException.class, () -> ClanViewPayload.NAME.decode(wire));
        assertEquals(ClanWire.MAX_NAME, new ClanView.Charter(ClanView.CHARTER_HELD, "z".repeat(200), false, false, 0).holderName().length(), "the record cuts a long name instead of failing the packet");
    }

    @Test
    void theRefusalNumberIsBoundedByTheEnumAndNotByAMagicNumber() throws Exception {
        String src = java.nio.file.Files.readString(root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/net/ClanViewPayload.java"));
        assertTrue(src.contains("ContestRules.Refusal.values().length"), "the payload bounds the refusal by the enum");
        assertTrue(!src.contains("bounded(buf, 16,"), "a magic bound is back");
        ClanView atEnd = new ClanView(true, 1, -1, ClanView.Charter.NONE, new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 0, false, net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules.Refusal.values().length, false), true, 10, 5);
        assertEquals(atEnd, roundTrip(atEnd));
        ClanView past = new ClanView(true, 1, -1, ClanView.Charter.NONE, new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 0, false, net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules.Refusal.values().length + 1, false), true, 10, 5);
        ByteBuf buf = Unpooled.buffer();
        ClanViewPayload.STREAM_CODEC.encode(buf, new ClanViewPayload(past));
        assertThrows(DecoderException.class, () -> ClanViewPayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void aCountOverTheCapIsRejected() {
        ClanView big = new ClanView(true, ClanWire.MAX_DAYS + 1, -1, ClanView.Charter.NONE, ClanView.Contest.NONE, true, 10, 5);
        ByteBuf buf = Unpooled.buffer();
        ClanViewPayload.STREAM_CODEC.encode(buf, new ClanViewPayload(big));
        assertThrows(DecoderException.class, () -> ClanViewPayload.STREAM_CODEC.decode(buf));
        ClanView percent = new ClanView(true, 1, -1, ClanView.Charter.NONE, ClanView.Contest.NONE, true, ClanWire.MAX_PERCENT + 1, 5);
        ByteBuf second = Unpooled.buffer();
        ClanViewPayload.STREAM_CODEC.encode(second, new ClanViewPayload(percent));
        assertThrows(DecoderException.class, () -> ClanViewPayload.STREAM_CODEC.decode(second));
        ClanView state = new ClanView(true, 1, -1, new ClanView.Charter((byte) 9, "", false, false, 0), ClanView.Contest.NONE, true, 10, 5);
        ByteBuf third = Unpooled.buffer();
        ClanViewPayload.STREAM_CODEC.encode(third, new ClanViewPayload(state));
        assertThrows(DecoderException.class, () -> ClanViewPayload.STREAM_CODEC.decode(third));
    }

    /** No person id, no species, no sireline root: counts, percents, seconds and the names of members of this clan. */
    @Test
    void theViewNeverCarriesAnotherClansDataOrAnyUuidOrSpecies() {
        List<String> seen = new ArrayList<>();
        for (Class<?> type : new Class<?>[] {ClanView.class, ClanView.Charter.class, ClanView.Contest.class}) {
            for (RecordComponent c : type.getRecordComponents()) {
                String t = c.getType().getName();
                seen.add(type.getSimpleName() + "." + c.getName() + ":" + c.getType().getSimpleName());
                assertTrue(!t.contains("UUID") && !t.contains("Optional") && !t.contains("Species") && !java.util.Map.class.isAssignableFrom(c.getType()) && !java.util.Collection.class.isAssignableFrom(c.getType()),
                        c.getName() + " is a " + t);
                assertTrue(!c.getName().toLowerCase().contains("species") && !c.getName().toLowerCase().contains("root") && !c.getName().toLowerCase().contains("original"), c.getName());
            }
        }
        assertEquals(List.of("ClanView.inClan:boolean", "ClanView.daysInClan:int", "ClanView.autoPromoteDaysLeft:int", "ClanView.charter:Charter", "ClanView.contest:Contest",
                "ClanView.feedOn:boolean", "ClanView.feedPercent:int", "ClanView.bonusPercent:int",
                "Charter.state:byte", "Charter.holderName:String", "Charter.mine:boolean", "Charter.forgeable:boolean", "Charter.reforgeSecondsLeft:int",
                "Contest.phase:byte", "Contest.challengerName:String", "Contest.secondsLeft:int", "Contest.cooldownDaysLeft:int", "Contest.iMayChallenge:boolean",
                "Contest.challengeWhy:int", "Contest.iMustAccept:boolean"), seen);
    }

    @Test
    void onlyTheViewersOwnClanIsCarried() throws Exception {
        String src = java.nio.file.Files.readString(root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/species/group/clan/ClanViews.java"));
        assertTrue(src.contains("ClanScope.of(level, p.getUUID())"), "the view must start from the viewer's own clan");
        assertTrue(!src.contains("charters()") && !src.contains("contests()"), "the view walks every clan's rows");
        assertTrue(src.contains("if (clan == null) {\n            return ClanView.NONE;") || src.contains("if (clan == null) {\r\n            return ClanView.NONE;"), "a viewer in no clan must get nothing");
    }

    @Test
    void theContestRequestKindIsAppendedLast() {
        var kinds = net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.values();
        assertEquals("CHARTER_CONTEST", kinds[kinds.length - 1].name());
        assertEquals("GROUP_VOTE", kinds[kinds.length - 2].name(), "the new kind goes after the last one, so an old client's names still mean the same");
    }

    @Test
    void aNonClanViewerGetsNone() {
        assertEquals(ClanView.NONE, new ClanView(false, 0, -1, ClanView.Charter.NONE, ClanView.Contest.NONE, false, 0, 0));
    }

    @Test
    void theHolderNameIsAMembersNameOrEmpty() throws Exception {
        String src = java.nio.file.Files.readString(root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/species/group/clan/ClanViews.java"));
        assertTrue(src.contains("Charters.isMember(viewer.server, id, group)"), "a name is shown only for a member of the viewer's clan");
        assertTrue(src.indexOf("nameIfMember(p, clan.group(), r.holder().orElse(null))") > 0 && src.indexOf("nameIfMember(p, clan.group(), cr.challenger())") > 0, "both names go through the member check");
    }

    private static java.nio.file.Path root() {
        java.nio.file.Path at = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return at;
    }
}
