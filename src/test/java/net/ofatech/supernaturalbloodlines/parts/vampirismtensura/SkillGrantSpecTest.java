package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkillGrantSpecTest {
    private static final int MAX = 10;

    @Test
    void defaultIsGodwolfSenseAtMax() {
        SkillGrantSpec.Parsed p = SkillGrantSpec.parse(SkillGrantSpec.DEFAULTS);
        assertEquals(List.of(new SkillGrantSpec(-1, "tensura:godwolf_sense")), p.grants());
        assertTrue(p.rejected().isEmpty());
    }

    @Test
    void minusOneMeansMaxLevel() {
        SkillGrantSpec g = new SkillGrantSpec(-1, "tensura:godwolf_sense");
        assertTrue(g.reachedAt(MAX, MAX));
        assertFalse(g.reachedAt(MAX - 1, MAX));
    }

    @Test
    void levelAboveMaxIsClampedToMax() {
        SkillGrantSpec g = new SkillGrantSpec(99, "tensura:godwolf_sense");
        assertTrue(g.reachedAt(MAX, MAX));
        assertFalse(g.reachedAt(1, MAX));
    }

    @Test
    void plainLevelIsAFloor() {
        SkillGrantSpec g = new SkillGrantSpec(3, "tensura:godwolf_sense");
        assertFalse(g.reachedAt(2, MAX));
        assertTrue(g.reachedAt(3, MAX));
        assertTrue(g.reachedAt(7, MAX));
    }

    @Test
    void notInFactionNeverReaches() {
        assertFalse(new SkillGrantSpec(1, "tensura:godwolf_sense").reachedAt(0, MAX));
    }

    @Test
    void malformedEntriesAreRejectedAndTheRestKept() {
        SkillGrantSpec.Parsed p = SkillGrantSpec.parse(List.of("2=tensura:a", "nolevel", "0=tensura:b", "-2=tensura:c", "x=tensura:d", "3=", "4=nocolon"));
        assertEquals(List.of(new SkillGrantSpec(2, "tensura:a")), p.grants());
        assertEquals(List.of("nolevel", "0=tensura:b", "-2=tensura:c", "x=tensura:d", "3=", "4=nocolon"), p.rejected());
    }
}
