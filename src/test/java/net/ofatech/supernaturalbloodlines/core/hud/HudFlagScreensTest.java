package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudFlagScreensTest {
    private static final String SCREENS = "io.github.manasmods.tensura.client.screen.";

    @Test
    void onlyTensurasHudSettingsScreenGetsItsFlagsBack() {
        assertTrue(HudFlagScreens.handsBack(SCREENS + "SettingsScreen"));
        assertTrue(HudFlagScreens.handsBack(SCREENS + "SettingsScreen$1"));
    }

    @Test
    void theStatusMenuAndEveryOtherTensuraScreenKeepTheSkillHudHidden() {
        for (String s : new String[] {"MainScreen", "MainScreen$1", "HumanoidMainScreen", "AbilitySelectionScreen", "AbilityCategoriesScreen", "EvolutionScreen",
                "SkillCreationScreen", "KilnScreen", "SpatialBagScreen", "NamingScreen", "SettingsScreenHelper"}) {
            assertFalse(HudFlagScreens.handsBack(SCREENS + s), s);
        }
        assertFalse(HudFlagScreens.handsBack("io.github.manasmods.manascore.config.SomeScreen"));
        assertFalse(HudFlagScreens.handsBack("net.minecraft.client.gui.screens.inventory.InventoryScreen"));
    }
}
