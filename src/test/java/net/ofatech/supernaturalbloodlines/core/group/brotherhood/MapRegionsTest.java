package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.MapRegions.Region;
import org.junit.jupiter.api.Test;

class MapRegionsTest {
    @Test
    void thresholds() {
        assertEquals(Set.of(), MapRegions.revealed(0));
        assertEquals(EnumSet.of(Region.SIGIL), MapRegions.revealed(1));
        assertEquals(EnumSet.of(Region.SIGIL), MapRegions.revealed(3));
        assertEquals(EnumSet.of(Region.SIGIL, Region.TWINS), MapRegions.revealed(4));
        assertEquals(EnumSet.of(Region.SIGIL, Region.TWINS, Region.RING_AND_STAR), MapRegions.revealed(8));
        assertEquals(EnumSet.of(Region.SIGIL, Region.TWINS, Region.RING_AND_STAR, Region.SLEEPER), MapRegions.revealed(12));
        assertEquals(EnumSet.of(Region.SIGIL, Region.TWINS, Region.RING_AND_STAR, Region.SLEEPER, Region.SWORDS), MapRegions.revealed(16));
        assertEquals(EnumSet.allOf(Region.class), MapRegions.revealed(20));
    }

    @Test
    void neverAboveTheCap() {
        assertEquals(EnumSet.allOf(Region.class), MapRegions.revealed(99));
    }

    @Test
    void theMapIsCompleteForEveryoneWhenAnyBrotherHasTwenty() {
        assertEquals(EnumSet.allOf(Region.class), MapRegions.shown(0, true));
        assertEquals(EnumSet.allOf(Region.class), MapRegions.shown(5, true));
    }

    @Test
    void withoutAnyTwentyOnlyTheOwnKillsShow() {
        assertEquals(Set.of(), MapRegions.shown(0, false));
        assertEquals(EnumSet.of(Region.SIGIL, Region.TWINS), MapRegions.shown(5, false));
    }
}