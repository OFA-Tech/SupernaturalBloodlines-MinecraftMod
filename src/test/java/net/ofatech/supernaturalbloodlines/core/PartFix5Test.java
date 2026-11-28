package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for Plan 22 Fix 5: SUPERNATURAL needs no required mod, SUPERNATURAL_TENSURA freed from Vampirism.
 */
class PartFix5Test {

    @Test
    void supernaturalNeedsNoRequiredMod() {
        // SUPERNATURAL has no required mods, only anyOfMods
        assertTrue(Part.SUPERNATURAL.requiredMods().isEmpty(), "SUPERNATURAL should have no required mods");
        assertEquals(Set.of("vampirism", "tensura", "werewolves"), Set.copyOf(Part.SUPERNATURAL.anyOfMods()));
    }

    @Test
    void supernaturalEnabledWithVampirismOnly() {
        Set<Part> parts = PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
        assertTrue(parts.contains(Part.HUD));
    }

    @Test
    void supernaturalEnabledWithTensuraOnly() {
        Set<Part> parts = PartLoader.enabledParts(Set.of("tensura")::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
    }

    @Test
    void supernaturalEnabledWithWerewolvesOnly() {
        Set<Part> parts = PartLoader.enabledParts(Set.of("werewolves")::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
    }

    @Test
    void supernaturalDisabledWithNoContentMods() {
        Set<Part> parts = PartLoader.enabledParts(id -> false, p -> true);
        assertFalse(parts.contains(Part.SUPERNATURAL));
    }

    @Test
    void supernaturalStatusReportsMissingAnyOf() {
        assertEquals("disabled (needs one of: vampirism, tensura, werewolves)",
                PartLoader.status(Part.SUPERNATURAL, id -> false, p -> true));
        assertEquals("enabled",
                PartLoader.status(Part.SUPERNATURAL, Set.of("vampirism")::contains, p -> true));
    }

    @Test
    void supernaturalTensuraNeedsOnlyTensura() {
        // SUPERNATURAL_TENSURA requires only tensura, not vampirism
        assertEquals(List.of("tensura"), Part.SUPERNATURAL_TENSURA.requiredMods());
        assertFalse(Part.SUPERNATURAL_TENSURA.requiredMods().contains("vampirism"));
    }

    @Test
    void supernaturalTensuraEnabledWithTensuraAndSupernatural() {
        // With tensura loaded and SUPERNATURAL enabled (via any content mod), SUPERNATURAL_TENSURA should be on
        Set<String> loaded = Set.of("tensura", "vampirism"); // vampirism enables SUPERNATURAL via anyOf
        Set<Part> parts = PartLoader.enabledParts(loaded::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
        assertTrue(parts.contains(Part.SUPERNATURAL_TENSURA));
    }

    @Test
    void supernaturalTensuraEnabledWithTensuraAndWerewolves() {
        // werewolves enables SUPERNATURAL via anyOf, tensura enables SUPERNATURAL_TENSURA
        Set<String> loaded = Set.of("tensura", "werewolves");
        Set<Part> parts = PartLoader.enabledParts(loaded::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
        assertTrue(parts.contains(Part.SUPERNATURAL_TENSURA));
    }

    @Test
    void supernaturalTensuraDisabledWithoutSupernatural() {
        Set<Part> off = PartLoader.enabledParts(id -> true, p -> p != Part.SUPERNATURAL);
        assertFalse(off.contains(Part.SUPERNATURAL_TENSURA));
    }

    @Test
    void supernaturalTensuraDisabledWithoutTensura() {
        // Even with vampirism (which enables SUPERNATURAL), without tensura SUPERNATURAL_TENSURA is off
        Set<String> loaded = Set.of("vampirism");
        Set<Part> parts = PartLoader.enabledParts(loaded::contains, p -> true);
        assertTrue(parts.contains(Part.SUPERNATURAL));
        assertFalse(parts.contains(Part.SUPERNATURAL_TENSURA));
    }

    @Test
    void fullComboUnchanged() {
        // With all mods, all parts should still be enabled (no regression)
        assertEquals(EnumSet.allOf(Part.class), PartLoader.enabledParts(id -> true, p -> true));
    }
}