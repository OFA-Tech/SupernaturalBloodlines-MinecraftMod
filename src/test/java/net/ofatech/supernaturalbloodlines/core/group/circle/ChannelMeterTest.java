package net.ofatech.supernaturalbloodlines.core.group.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChannelMeterTest {
    private static final long MIN = 60_000L;

    @Test
    void refillsLazilyUpToTheCap() {
        assertEquals(300, ChannelMeter.valueAt(0, 0, MIN, 3000, 10));
        assertEquals(1500, ChannelMeter.valueAt(0, 0, 5 * MIN, 3000, 10));
        assertEquals(3000, ChannelMeter.valueAt(0, 0, 10 * MIN, 3000, 10));
        assertEquals(3000, ChannelMeter.valueAt(0, 0, 500 * MIN, 3000, 10));
    }

    @Test
    void aFullMeterStaysFull() {
        assertEquals(3000, ChannelMeter.valueAt(3000, 0, 7 * MIN, 3000, 10));
    }

    @Test
    void aClockThatGoesBackGivesNoTime() {
        assertEquals(100, ChannelMeter.valueAt(100, 10 * MIN, 5 * MIN, 3000, 10));
        assertEquals(0, ChannelMeter.valueAt(-50, 10 * MIN, 0, 3000, 10));
    }

    @Test
    void otherKindsHaveHalfTheCap() {
        assertEquals(3000, ChannelMeter.capacity(true, 3000, 0.5));
        assertEquals(1500, ChannelMeter.capacity(false, 3000, 0.5));
    }

    @Test
    void zoneBoundariesAreInclusiveOnTheLowerZone() {
        int cap = 3000;
        assertEquals(ChannelMeter.Zone.CLEAN, ChannelMeter.zone(0, cap));
        assertEquals(ChannelMeter.Zone.NOSEBLEED, ChannelMeter.zone(1, cap));
        assertEquals(ChannelMeter.Zone.NOSEBLEED, ChannelMeter.zone(cap / 2, cap));
        assertEquals(ChannelMeter.Zone.DAMAGE, ChannelMeter.zone(cap / 2 + 1, cap));
        assertEquals(ChannelMeter.Zone.DAMAGE, ChannelMeter.zone(cap, cap));
        assertEquals(ChannelMeter.Zone.REFUSED, ChannelMeter.zone(cap + 1, cap));
    }

    @Test
    void damageGrowsFromZeroToTen() {
        int cap = 3000;
        assertEquals(0, ChannelMeter.damageHp(cap / 2, cap));
        assertEquals(5, ChannelMeter.damageHp(cap / 2 + cap / 4, cap));
        assertEquals(10, ChannelMeter.damageHp(cap, cap));
        assertEquals(10, ChannelMeter.damageHp(cap * 3, cap));
    }
}