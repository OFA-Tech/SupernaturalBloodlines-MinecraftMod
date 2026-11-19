package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.junit.jupiter.api.Test;

class PartLoaderTest {
    @Test
    void noModsMeansOnlyTheModFreeParts() {
        assertEquals(EnumSet.of(Part.HUD, Part.CELESTIALS), PartLoader.enabledParts(id -> false, p -> true));
    }

    @Test
    void celestialsNeedsNoModAndSwitchesOff() {
        assertTrue(Part.CELESTIALS.requiredMods().isEmpty());
        assertTrue(Part.CELESTIALS.requiredParts().isEmpty());
        assertEquals("celestials", Part.CELESTIALS.configKey());
        assertFalse(PartLoader.enabledParts(id -> true, p -> p != Part.CELESTIALS).contains(Part.CELESTIALS));
        assertEquals(EnumSet.complementOf(EnumSet.of(Part.CELESTIALS)), PartLoader.enabledParts(id -> true, p -> p != Part.CELESTIALS));
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
    void speciesNeedsVampirismEnchanted() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.VAMPIRISM_ENCHANTED);
        assertFalse(parts.contains(Part.SPECIES));
        assertFalse(parts.contains(Part.SPECIES_TENSURA));
    }

    @Test
    void artifactsNeedVampirismEnchanted() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.VAMPIRISM_ENCHANTED);
        assertFalse(parts.contains(Part.ARTIFACTS));
        assertTrue(Part.ARTIFACTS.requiredParts().contains(Part.VAMPIRISM_ENCHANTED));
        assertEquals("disabled (requires [VAMPIRISM_ENCHANTED])",
                PartLoader.status(Part.ARTIFACTS, id -> true, p -> p != Part.VAMPIRISM_ENCHANTED));
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
        assertEquals(EnumSet.of(Part.VAMPIRISM_ENCHANTED, Part.SPECIES, Part.ARTIFACTS, Part.SUPERNATURAL, Part.HUD, Part.CELESTIALS),
                PartLoader.enabledParts(loaded::contains, p -> true));
    }

    @Test
    void supernaturalNeedsOnlyVampirism() {
        assertEquals(EnumSet.of(Part.SUPERNATURAL, Part.HUD, Part.CELESTIALS), PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true));
        assertFalse(PartLoader.enabledParts(id -> !id.equals("vampirism"), p -> true).contains(Part.SUPERNATURAL));
    }

    @Test
    void supernaturalTensuraNeedsVampirismTensuraAndSupernatural() {
        Set<String> loaded = Set.of("vampirism", "tensura");
        assertTrue(PartLoader.enabledParts(loaded::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        assertFalse(PartLoader.enabledParts(Set.of("vampirism")::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        assertFalse(PartLoader.enabledParts(Set.of("tensura")::contains, p -> true).contains(Part.SUPERNATURAL_TENSURA));
        Set<Part> off = PartLoader.enabledParts(id -> true, p -> p != Part.SUPERNATURAL);
        assertFalse(off.contains(Part.SUPERNATURAL_TENSURA));
        assertEquals("disabled (requires [SUPERNATURAL])",
                PartLoader.status(Part.SUPERNATURAL_TENSURA, id -> true, p -> p != Part.SUPERNATURAL));
    }

    @Test
    void supernaturalOffKeepsTheOtherParts() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.SUPERNATURAL);
        assertTrue(parts.containsAll(EnumSet.complementOf(EnumSet.of(Part.SUPERNATURAL, Part.SUPERNATURAL_TENSURA))));
    }

    @Test
    void hudNeedsNoMod() {
        assertTrue(Part.HUD.requiredMods().isEmpty());
        assertTrue(Part.HUD.requiredParts().isEmpty());
        assertEquals("hud", Part.HUD.configKey());
        assertTrue(PartLoader.enabledParts(id -> false, p -> true).contains(Part.HUD));
        assertEquals("enabled", PartLoader.status(Part.HUD, id -> false, p -> true));
    }

    @Test
    void hudOffBySwitch() {
        Set<Part> parts = PartLoader.enabledParts(id -> true, p -> p != Part.HUD);
        assertFalse(parts.contains(Part.HUD));
        assertEquals("disabled (config)", PartLoader.status(Part.HUD, id -> true, p -> p != Part.HUD));
        assertEquals(EnumSet.complementOf(EnumSet.of(Part.HUD)), parts);
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
