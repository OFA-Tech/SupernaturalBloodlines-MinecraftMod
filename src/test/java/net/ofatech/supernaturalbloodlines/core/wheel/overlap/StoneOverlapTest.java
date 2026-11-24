package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import org.junit.jupiter.api.Test;

class StoneOverlapTest {
    /**
     * Owner, 2026-11-22: a stone power exists only because the player holds the stone, so no production group has an Infinity member and the dedup groups run between the other
     * sides only (the research lists Infinity members of M01, M02, M05 and M08: they are modes inside a stone, not separate powers).
     */
    @Test
    void stoneEntryIsNeverHiddenByTheOverlapTable() {
        OverlapTable prod = OverlapTableTest.production();
        for (OverlapGroup group : prod.groups()) {
            for (var member : group.members()) {
                assertFalse(member.source() == PowerSource.INFINITY, group.id() + " has an Infinity member: " + member.key());
            }
        }
        for (var chain : prod.chains()) {
            for (var member : chain) {
                assertFalse(member.source() == PowerSource.INFINITY, member.key());
            }
        }
    }
}
