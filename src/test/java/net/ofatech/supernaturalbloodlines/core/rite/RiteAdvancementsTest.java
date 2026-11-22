package net.ofatech.supernaturalbloodlines.core.rite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RiteAdvancementsTest {
    @Test
    void theNamesFollowTheDataFiles() {
        assertEquals("supernaturalbloodlines:rites/known/immortality", RiteAdvancements.known("immortality"));
        assertEquals("supernaturalbloodlines:rites/sealed/immortality", RiteAdvancements.sealed("immortality"));
        assertEquals("supernaturalbloodlines/rites/immortality", RiteAdvancements.entry("immortality"));
        assertEquals("supernaturalbloodlines/sealed/immortality", RiteAdvancements.sealedEntry("immortality"));
    }

    @Test
    void theRiteOfAnAdvancement() {
        assertEquals("silver_moon", RiteAdvancements.riteOf(RiteAdvancements.known("silver_moon")));
        assertEquals("silver_moon", RiteAdvancements.riteOf(RiteAdvancements.sealed("silver_moon")));
        assertNull(RiteAdvancements.riteOf("minecraft:story/root"));
        assertNull(RiteAdvancements.riteOf("supernaturalbloodlines:something/else"));
    }
}
