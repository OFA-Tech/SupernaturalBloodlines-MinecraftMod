package net.ofatech.supernaturalbloodlines.core.group.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CirclePlanTest {
    private static final double SHARE = 0.70;
    private static final double HUGE_EP = 1e12;

    private static UUID id(int i) {
        return new UUID(0, i);
    }

    private static CirclePlan.Member member(int i, int meter, int cap, Consent consent, double base) {
        return new CirclePlan.Member(id(i), meter, cap, consent, base, i == 0);
    }

    private static List<CirclePlan.Member> circle(int n, int meter, int cap, Consent consent) {
        List<CirclePlan.Member> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(member(i, meter, cap, consent, HUGE_EP));
        }
        return out;
    }

    private static int powerSum(CirclePlan.Result r) {
        return r.shares().values().stream().mapToInt(CirclePlan.Share::power).sum();
    }

    @Test
    void aSoloCastChangesNothing() {
        CirclePlan.Result r = CirclePlan.plan(4500, 0, circle(1, 3000, 3000, Consent.NONE), SHARE, 10);
        assertEquals(4500, r.altarPays());
        assertTrue(r.shares().isEmpty());
    }

    @Test
    void threeShareAFortyFiveHundredBillClean() {
        CirclePlan.Result r = CirclePlan.plan(4500, 0, circle(3, 3000, 3000, Consent.NONE), SHARE, 10);
        assertEquals(1350, r.altarPays());
        assertEquals(3, r.shares().size());
        r.shares().values().forEach(s -> {
            assertEquals(1050, s.power());
            assertEquals(ChannelMeter.Zone.CLEAN, s.zone());
        });
    }

    @Test
    void twentyThousandRiteAtThreeNeedsConsent() {
        CirclePlan.Result ok = CirclePlan.plan(15000, 0, circle(3, 3000, 3000, Consent.NOSEBLEED), SHARE, 10);
        assertEquals(4500, ok.altarPays());
        ok.shares().values().forEach(s -> {
            assertEquals(3500, s.power());
            assertEquals(ChannelMeter.Zone.NOSEBLEED, s.zone());
            assertEquals(500, s.overdraw());
        });
        CirclePlan.Result none = CirclePlan.plan(15000, 0, circle(3, 3000, 3000, Consent.NONE), SHARE, 10);
        assertTrue(none.shares().isEmpty());
        assertEquals(15000, none.altarPays());
        assertEquals(2, none.dropped().size());
    }

    @Test
    void twentyThousandRiteAtSevenIsClean() {
        CirclePlan.Result r = CirclePlan.plan(15000, 0, circle(7, 3000, 3000, Consent.NONE), SHARE, 10);
        assertEquals(4500, r.altarPays());
        r.shares().values().forEach(s -> {
            assertEquals(1500, s.power());
            assertEquals(ChannelMeter.Zone.CLEAN, s.zone());
        });
    }

    @Test
    void thirtySixThousandAtThirteenSplitsExactly() {
        CirclePlan.Result r = CirclePlan.plan(27000, 0, circle(13, 3000, 3000, Consent.NONE), SHARE, 10);
        assertEquals(8100, r.altarPays());
        assertEquals(18900, powerSum(r));
        assertEquals(11, r.shares().values().stream().filter(s -> s.power() == 1454).count());
        assertEquals(2, r.shares().values().stream().filter(s -> s.power() == 1453).count());
    }

    @Test
    void sharesAlwaysAddUpToThePool() {
        for (int n = 2; n <= 13; n++) {
            for (int bill = 0; bill <= 50000; bill += 777) {
                CirclePlan.Result r = CirclePlan.plan(bill, 0, circle(n, 100000, 100000, Consent.DAMAGE), SHARE, 10);
                int pool = (int) Math.floor(bill * SHARE + 1e-9);
                assertEquals(pool, powerSum(r), "n=" + n + " bill=" + bill);
                assertEquals(bill - pool, r.altarPays(), "n=" + n + " bill=" + bill);
            }
        }
    }

    @Test
    void theAnchorPaysTheExtraUnit() {
        CirclePlan.Result r = CirclePlan.plan(10, 0, circle(3, 100, 100, Consent.NONE), SHARE, 10);
        assertEquals(3, r.shares().get(id(0)).power());
        assertEquals(2, r.shares().get(id(1)).power());
        assertEquals(2, r.shares().get(id(2)).power());
    }

    @Test
    void aMemberOverTheHardCapIsDroppedAndTheRestRecomputed() {
        List<CirclePlan.Member> list = new ArrayList<>(circle(3, 20000, 20000, Consent.DAMAGE));
        list.set(0, member(0, 0, 100, Consent.DAMAGE, HUGE_EP));
        CirclePlan.Result r = CirclePlan.plan(30000, 0, list, SHARE, 10);
        assertEquals(List.of(new CirclePlan.Dropped(id(0), CirclePlan.DropReason.TOO_HEAVY)), r.dropped());
        assertEquals(2, r.shares().size());
        r.shares().values().forEach(s -> assertEquals(10500, s.power()));
    }

    @Test
    void consentNoneDropsANosebleedMember() {
        List<CirclePlan.Member> list = circle(3, 3000, 10000, Consent.NOSEBLEED);
        list.set(1, member(1, 3000, 10000, Consent.NONE, HUGE_EP));
        CirclePlan.Result r = CirclePlan.plan(15000, 0, list, SHARE, 10);
        assertEquals(List.of(new CirclePlan.Dropped(id(1), CirclePlan.DropReason.NEEDS_CONSENT)), r.dropped());
        assertEquals(2, r.shares().size());
        r.shares().values().forEach(s -> assertEquals(5250, s.power()));
    }

    @Test
    void consentNosebleedDropsADamageMember() {
        List<CirclePlan.Member> list = circle(4, 0, 5000, Consent.DAMAGE);
        list.set(0, member(0, 0, 5000, Consent.NOSEBLEED, HUGE_EP));
        CirclePlan.Result r = CirclePlan.plan(20000, 0, list, SHARE, 10);
        assertEquals(List.of(new CirclePlan.Dropped(id(0), CirclePlan.DropReason.NEEDS_CONSENT)), r.dropped());
        assertEquals(3, r.shares().size());
    }

    @Test
    void damageConsentKeepsADamageMember() {
        CirclePlan.Result r = CirclePlan.plan(20000, 0, circle(4, 0, 5000, Consent.DAMAGE), SHARE, 10);
        assertTrue(r.dropped().isEmpty());
        assertEquals(4, r.shares().size());
        r.shares().values().forEach(s -> assertEquals(ChannelMeter.Zone.DAMAGE, s.zone()));
    }

    @Test
    void epIsSplitTheSameWay() {
        CirclePlan.Result r = CirclePlan.plan(0, 2_000_000, circle(7, 3000, 3000, Consent.NONE), SHARE, 10);
        assertEquals(2_000_000, r.shares().values().stream().mapToDouble(CirclePlan.Share::ep).sum(), 1e-6);
        assertEquals(285_715, r.shares().get(id(0)).ep(), 1e-6);
        assertEquals(285_715, r.shares().get(id(1)).ep(), 1e-6);
        for (int i = 2; i < 7; i++) {
            assertEquals(285_714, r.shares().get(id(i)).ep(), 1e-6);
        }
    }

    @Test
    void aMemberBelowTheEpFloorIsDropped() {
        List<CirclePlan.Member> list = circle(3, 3000, 3000, Consent.NONE);
        list.set(1, member(1, 3000, 3000, Consent.NONE, 350_000));
        CirclePlan.Result r = CirclePlan.plan(0, 1_000_000, list, SHARE, 10);
        assertEquals(List.of(new CirclePlan.Dropped(id(1), CirclePlan.DropReason.EP_FLOOR)), r.dropped());
        assertEquals(2, r.shares().size());
        r.shares().values().forEach(s -> assertEquals(500_000, s.ep(), 1e-6));
    }

    @Test
    void recomputeConvergesInAtMostThirteenPasses() {
        List<CirclePlan.Member> list = List.of(member(0, 199, 1000, Consent.DAMAGE, HUGE_EP), member(1, 599, 1000, Consent.DAMAGE, HUGE_EP),
                member(2, 999, 1000, Consent.DAMAGE, HUGE_EP), member(3, 1000, 1000, Consent.DAMAGE, HUGE_EP));
        CirclePlan.Result r = CirclePlan.plan(4800, 0, list, 1.0, 10);
        assertEquals(List.of(id(0), id(1), id(2)), r.dropped().stream().map(CirclePlan.Dropped::id).toList());
        assertEquals(4, r.passes());
        assertTrue(r.passes() <= 13);
        assertTrue(r.shares().isEmpty());
    }

    @Test
    void fewerThanTwoLeftIsASoloCast() {
        List<CirclePlan.Member> list = List.of(member(0, 0, 100, Consent.DAMAGE, HUGE_EP), member(1, 0, 100, Consent.DAMAGE, HUGE_EP));
        CirclePlan.Result r = CirclePlan.plan(10000, 0, list, SHARE, 10);
        assertTrue(r.shares().isEmpty());
        assertEquals(10000, r.altarPays());
    }

    @Test
    void theMemberShareZeroGivesAnAllAltarBill() {
        CirclePlan.Result r = CirclePlan.plan(4500, 0, circle(3, 3000, 3000, Consent.NONE), 0.0, 10);
        assertEquals(4500, r.altarPays());
        assertEquals(3, r.shares().size());
        r.shares().values().forEach(s -> assertEquals(0, s.power()));
    }
}