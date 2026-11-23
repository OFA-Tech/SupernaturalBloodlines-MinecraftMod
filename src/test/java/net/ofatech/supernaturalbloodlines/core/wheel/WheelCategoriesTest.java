package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WheelCategoriesTest {
    private static final java.util.function.Predicate<PowerSource> ALL = s -> true;

    @Test
    void emptyCategoryIsHidden() {
        // Enchanted-like: a source with no entries is simply absent
        assertEquals(List.of(PowerSource.BLOODLINE, PowerSource.TENSURA), WheelCategories.visible(Set.of(PowerSource.TENSURA, PowerSource.BLOODLINE), ALL));
        assertTrue(WheelCategories.visible(Set.of(), ALL).isEmpty());
    }

    @Test
    void orderIsFixedWhateverTheInput() {
        List<PowerSource> reversed = new java.util.ArrayList<>(PowerSource.displayOrder());
        java.util.Collections.reverse(reversed);
        assertEquals(PowerSource.displayOrder(), WheelCategories.visible(reversed, ALL));
    }

    @Test
    void uninstalledSourceIsHiddenEvenWithEntries() {
        assertEquals(List.of(PowerSource.BLOODLINE),
                WheelCategories.visible(Set.of(PowerSource.BLOODLINE, PowerSource.VAMPIRISM), s -> s != PowerSource.VAMPIRISM));
    }

    @Test
    void tensuraWithThreeGroupsHasAMiddleRing() {
        assertEquals(List.of(PowerGroup.SKILLS, PowerGroup.MAGIC, PowerGroup.BATTLEWILL),
                WheelCategories.groups(PowerSource.TENSURA, List.of(PowerGroup.BATTLEWILL, PowerGroup.SKILLS, PowerGroup.MAGIC)));
    }

    @Test
    void tensuraWithOnlySkillsHasNoMiddleRing() {
        assertTrue(WheelCategories.groups(PowerSource.TENSURA, List.of(PowerGroup.SKILLS)).isEmpty());
    }

    @Test
    void tensuraWithSkillsAndSpellsHasTwoGroups() {
        assertEquals(List.of(PowerGroup.SKILLS, PowerGroup.MAGIC), WheelCategories.groups(PowerSource.TENSURA, List.of(PowerGroup.MAGIC, PowerGroup.SKILLS)));
    }

    @Test
    void otherSourcesNeverHaveAMiddleRing() {
        for (PowerSource s : PowerSource.values()) {
            if (s != PowerSource.TENSURA) {
                assertTrue(WheelCategories.groups(s, List.of(PowerGroup.MAIN)).isEmpty(), s.name());
                assertTrue(WheelCategories.groups(s, List.of(PowerGroup.MAIN, PowerGroup.MAGIC)).isEmpty(), s.name() + " even with a stray second group");
            }
        }
    }

    @Test
    void groupOrderIsSkillsMagicBattlewill() {
        assertEquals(List.of(PowerGroup.SKILLS, PowerGroup.MAGIC, PowerGroup.BATTLEWILL), PowerGroup.displayOrder());
        assertFalse(PowerGroup.displayOrder().contains(PowerGroup.MAIN));
    }
}
