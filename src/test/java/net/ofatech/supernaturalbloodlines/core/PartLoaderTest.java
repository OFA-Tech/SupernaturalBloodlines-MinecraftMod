package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartLoaderTest {
    @Test
    void noModsMeansNoParts() {
        assertTrue(PartLoader.enabledParts(id -> false, p -> true).isEmpty());
    }

    @Test
    void allModsAllParts() {
        assertEquals(EnumSet.allOf(Part.class), PartLoader.enabledParts(id -> true, p -> true));
    }

    @Test
    void configCanDisablePart() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.VAMPIRISM_TENSURA);
        assertFalse(parts.contains(Part.VAMPIRISM_TENSURA));
        assertTrue(parts.contains(Part.ENCHANTED_TENSURA));
    }

    @Test
    void speciesTensuraNeedsSpecies() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.SPECIES);
        assertFalse(parts.contains(Part.SPECIES));
        assertFalse(parts.contains(Part.SPECIES_TENSURA));
    }

    @Test
    void missingWerewolvesKeepsSpecies() {
        Set<Part> parts = PartLoader.enabledParts(id -> !id.equals("werewolves"), p -> true);
        assertTrue(parts.contains(Part.SPECIES));
        assertTrue(parts.contains(Part.ARTIFACTS));
    }

    @Test
    void statusNamesMissingMods() {
        assertEquals("disabled (missing: tensura)",
                PartLoader.status(Part.VAMPIRISM_TENSURA, id -> !id.equals("tensura"), p -> true));
        assertEquals("disabled (config)", PartLoader.status(Part.SPECIES, id -> true, p -> false));
        assertEquals("enabled", PartLoader.status(Part.SPECIES, id -> true, p -> true));
    }
}
