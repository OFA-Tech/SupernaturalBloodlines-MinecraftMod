package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class LayerTableTest {
    @Test
    void defaultsMatchTheDesignTable() {
        LayerTable t = LayerTable.parse(LayerTable.DEFAULTS).table();
        LayerSpec original = t.specFor(Species.ORIGINAL, false);
        assertEquals(List.of("tensura:blood_mist", "tensura:drain", "tensura:charm", "tensura:self_regeneration"), original.skills());
        assertEquals(0.20, original.maxMagiculeBonus());
        assertTrue(original.treatAsUndead());
        assertEquals(0.25, t.specFor(Species.ORIGINAL_HYBRID, false).maxMagiculeBonus());
        assertTrue(t.specFor(Species.ORIGINAL_HYBRID, false).skills().contains("tensura:godwolf_sense"));
        LayerSpec witch = t.specFor(Species.WITCH, false);
        assertTrue(witch.skills().isEmpty());
        assertEquals(0.30, witch.maxMagiculeBonus());
        assertEquals(0.10, witch.maxAuraBonus());
        assertFalse(witch.treatAsUndead());
        assertEquals(0.10, t.specFor(Species.HUNTER_OF_THE_FIVE, false).maxAuraBonus());
        assertFalse(t.specFor(Species.SIPHONER, false).treatAsUndead());
        assertTrue(t.specFor(Species.HERETIC, false).treatAsUndead());
    }

    @Test
    void tribridIsHybridPlusCharm() {
        LayerTable t = LayerTable.parse(LayerTable.DEFAULTS).table();
        List<String> hybrid = t.specFor(Species.HYBRID, false).skills();
        List<String> tribrid = t.specFor(Species.TRIBRID, false).skills();
        assertTrue(tribrid.containsAll(hybrid));
        assertTrue(tribrid.contains("tensura:charm"));
    }

    @Test
    void plainVampireOnlyCountsWithoutASpecies() {
        LayerTable t = LayerTable.parse(LayerTable.DEFAULTS).table();
        LayerSpec vampire = t.specFor(Species.NONE, true);
        assertEquals(List.of("tensura:drain"), vampire.skills());
        assertTrue(vampire.treatAsUndead());
        assertEquals(0.0, vampire.maxMagiculeBonus());
        assertEquals(LayerSpec.NONE, t.specFor(Species.NONE, false));
        // a species wins over the vampire faction
        assertEquals(LayerSpec.NONE.skills(), t.specFor(Species.WITCH, true).skills());
        assertEquals(LayerSpec.NONE, t.specFor(Species.POTENTIAL, true));
    }

    @Test
    void configOverridesAndKeepsTheRestOfTheDefaults() {
        ParsedTable<LayerTable> p = LayerTable.parse(List.of("witch=tensura:charm, godwolf_sense|0.5|0.25|true"));
        LayerSpec witch = p.table().specFor(Species.WITCH, false);
        assertEquals(List.of("tensura:charm", "tensura:godwolf_sense"), witch.skills());
        assertEquals(0.5, witch.maxMagiculeBonus());
        assertEquals(0.25, witch.maxAuraBonus());
        assertTrue(witch.treatAsUndead());
        assertTrue(p.rejected().isEmpty());
        assertEquals(0.20, p.table().specFor(Species.ORIGINAL, false).maxMagiculeBonus());
    }

    @Test
    void emptySkillListIsAllowed() {
        LayerSpec s = LayerTable.parse(List.of("ORIGINAL=|0.1|0|false")).table().specFor(Species.ORIGINAL, false);
        assertTrue(s.skills().isEmpty());
        assertEquals(0.1, s.maxMagiculeBonus());
    }

    @Test
    void badEntriesAreRejectedAndTheDefaultStays() {
        ParsedTable<LayerTable> p = LayerTable.parse(List.of("DRAGON=drain|0|0|false", "ORIGINAL=drain|x|0|false",
                "ORIGINAL=drain|0.1|0", "ORIGINAL=drain|-0.5|0|false", "nonsense", "ORIGINAL=bad id|0|0|false"));
        assertEquals(6, p.rejected().size());
        assertEquals(0.20, p.table().specFor(Species.ORIGINAL, false).maxMagiculeBonus());
    }
}
