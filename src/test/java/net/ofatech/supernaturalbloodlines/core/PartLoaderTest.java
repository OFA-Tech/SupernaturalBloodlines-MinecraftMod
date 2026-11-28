package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.junit.jupiter.api.Test;

class PartLoaderTest {
    @Test
    void noModsMeansNoPartsAtAll() {
        assertEquals(EnumSet.noneOf(Part.class), PartLoader.enabledParts(id -> false, p -> true));
    }

    @Test
    void hudNeedsVampirismOrTensura() {
        assertTrue(Part.HUD.requiredMods().isEmpty());
        assertEquals(Set.of("vampirism", "tensura"), Set.copyOf(Part.HUD.anyOfMods()));
        assertTrue(PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true).contains(Part.HUD));
        assertTrue(PartLoader.enabledParts(Set.of("tensura")::contains, p -> true).contains(Part.HUD));
        assertFalse(PartLoader.enabledParts(id -> false, p -> true).contains(Part.HUD));
    }

    @Test
    void celestialsNeedsEnhancedCelestials() {
        assertTrue(Part.CELESTIALS.requiredMods().isEmpty());
        assertEquals(Set.of("enhancedcelestials2core"), Set.copyOf(Part.CELESTIALS.anyOfMods()));
        assertTrue(PartLoader.enabledParts(Set.of("enhancedcelestials2core")::contains, p -> true).contains(Part.CELESTIALS));
        assertFalse(PartLoader.enabledParts(id -> false, p -> true).contains(Part.CELESTIALS));
    }

    @Test
    void anyOfStatusReportsMissingMods() {
        assertEquals("disabled (needs one of: vampirism, tensura)",
                PartLoader.status(Part.HUD, id -> false, p -> true));
        assertEquals("enabled",
                PartLoader.status(Part.HUD, Set.of("vampirism")::contains, p -> true));
    }

    @Test
    void celestialsSwitchesOff() {
        assertTrue(Part.CELESTIALS.requiredMods().isEmpty());
        assertTrue(Part.CELESTIALS.requiredParts().isEmpty());
        assertEquals("celestials", Part.CELESTIALS.configKey());
        assertFalse(PartLoader.enabledParts(id -> true, p -> p != Part.CELESTIALS).contains(Part.CELESTIALS));
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
    void speciesNeedsVampirismAndEnchanted() {
        // SPECIES requires both vampirism AND enchanted (requiredMods, not anyOf)
        Set<Part> noEnchanted = PartLoader.enabledParts(id -> !id.equals("enchanted"), p -> true);
        assertFalse(noEnchanted.contains(Part.SPECIES));
        Set<Part> noVamp = PartLoader.enabledParts(id -> !id.equals("vampirism"), p -> true);
        assertFalse(noVamp.contains(Part.SPECIES));
        // With both present, SPECIES is enabled
        Set<Part> both = PartLoader.enabledParts(Set.of("vampirism", "enchanted")::contains, p -> true);
        assertTrue(both.contains(Part.SPECIES));
    }

    @Test
    void artifactsNeedOnlyVampirism() {
        assertTrue(Part.ARTIFACTS.requiredParts().isEmpty());
        assertEquals(List.of("vampirism"), Part.ARTIFACTS.requiredMods());
        Set<Part> vampOnly = PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true);
        assertTrue(vampOnly.contains(Part.ARTIFACTS));
        Set<Part> noVamp = PartLoader.enabledParts(id -> !id.equals("vampirism"), p -> true);
        assertFalse(noVamp.contains(Part.ARTIFACTS));
    }

    @Test
    void missingWerewolvesKeepsSpecies() {
        Set<Part> parts = PartLoader.enabledParts(id -> !id.equals("werewolves"), p -> true);
        assertTrue(parts.contains(Part.SPECIES));
        assertTrue(parts.contains(Part.ARTIFACTS));
    }

    @Test
    void vampirismAndEnchantedOnly() {
        Set<String> loaded = Set.of("vampirism", "enchanted");
        assertEquals(EnumSet.of(Part.VAMPIRISM_ENCHANTED, Part.SPECIES, Part.ARTIFACTS, Part.SUPERNATURAL, Part.HUD),
                PartLoader.enabledParts(loaded::contains, p -> true));
    }

    @Test
    void supernaturalUsesAnyOfMods() {
        // SUPERNATURAL uses anyOfMods: enabled if ANY of vampirism/tensura/werewolves is present
        Set<Part> vampOnly = PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true);
        assertTrue(vampOnly.contains(Part.SUPERNATURAL));
        assertTrue(vampOnly.contains(Part.HUD));
        assertTrue(vampOnly.contains(Part.ARTIFACTS));
        // Still enabled with only tensura (anyOf)
        Set<Part> tensuraOnly = PartLoader.enabledParts(Set.of("tensura")::contains, p -> true);
        assertTrue(tensuraOnly.contains(Part.SUPERNATURAL));
        // Disabled when NONE of the anyOf mods are present
        Set<Part> none = PartLoader.enabledParts(id -> false, p -> true);
        assertFalse(none.contains(Part.SUPERNATURAL));
    }

    @Test
    void supernaturalTensuraNeedsTensuraAndSupernatural() {
        // SUPERNATURAL_TENSURA requires tensura mod + SUPERNATURAL part
        Set<String> loaded = Set.of("vampirism", "tensura");
        assertTrue(PartLoader.enabledParts(loaded::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        // Disabled without tensura mod
        assertFalse(PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        // Enabled with only tensura (SUPERNATURAL uses anyOf so it's also enabled)
        assertTrue(PartLoader.enabledParts(Set.of("tensura")::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        // Disabled when SUPERNATURAL part is off
        Set<Part> off = PartLoader.enabledParts(id -> true, p -> p != Part.SUPERNATURAL);
        assertFalse(off.contains(Part.SUPERNATURAL_TENSURA));
        assertEquals("disabled (requires [SUPERNATURAL])",
                PartLoader.status(Part.SUPERNATURAL_TENSURA, id -> true, p -> p != Part.SUPERNATURAL));
    }

    @Test
    void supernaturalOffKeepsTheOtherParts() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.SUPERNATURAL);
        assertFalse(parts.contains(Part.SUPERNATURAL));
        assertFalse(parts.contains(Part.SUPERNATURAL_TENSURA));
        for (Part p : Part.values()) {
            if (p != Part.SUPERNATURAL && p != Part.SUPERNATURAL_TENSURA) {
                assertTrue(parts.contains(p), p + " should still be enabled");
            }
        }
    }

    @Test
    void hudConfigKeyAndNoRequiredMods() {
        assertTrue(Part.HUD.requiredMods().isEmpty());
        assertTrue(Part.HUD.requiredParts().isEmpty());
        assertEquals("hud", Part.HUD.configKey());
    }

    @Test
    void hudOffBySwitch() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.HUD);
        assertFalse(parts.contains(Part.HUD));
        assertEquals("disabled (config)", PartLoader.status(Part.HUD, id -> true, p -> p != Part.HUD));
    }

    @Test
    void everyInitClassResolvesWithInitMethod() throws Exception {
        for (Part p : Part.values()) {
            Method m = Class.forName(p.initClass()).getMethod("init", IEventBus.class, ModContainer.class);
            assertTrue(Modifier.isStatic(m.getModifiers()), p + " init must be static");
            assertEquals(void.class, m.getReturnType(), p + " init must return void");
        }
    }

    @Test
    void statusNamesMissingMods() {
        assertEquals("disabled (missing: tensura)",
                PartLoader.status(Part.VAMPIRISM_TENSURA, id -> !id.equals("tensura"), p -> true));
        assertEquals("disabled (config)", PartLoader.status(Part.SPECIES, id -> true, p -> false));
        assertEquals("enabled", PartLoader.status(Part.SPECIES, id -> true, p -> true));
    }
}