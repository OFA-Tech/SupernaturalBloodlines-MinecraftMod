package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

class HudIconsTest {
    private static final String MOD = "supernaturalbloodlines";

    @Test
    void ourOwnHudTexturesHaveAKitIcon() {
        assertEquals("bite", HudIcons.kitIcon(MOD, "textures/gui/hud/bite.png", MOD));
        assertEquals("alpha_roar", HudIcons.kitIcon(MOD, "textures/gui/hud/alpha_roar.png", MOD));
    }

    @Test
    void anotherModsTextureOrAnotherPlaceOfOurOwnKeepsItsPicture() {
        assertNull(HudIcons.kitIcon("tensura", "textures/gui/hud/bite.png", MOD), "another mod's bite");
        assertNull(HudIcons.kitIcon(MOD, "textures/gui/hud/unknown.png", MOD), "the unknown rune has no kit icon");
        assertNull(HudIcons.kitIcon(MOD, "textures/gui/hud/blood_full.png", MOD), "the food bar's blood point is not a slot icon");
        assertNull(HudIcons.kitIcon(MOD, "textures/item/bite.png", MOD));
        assertNull(HudIcons.kitIcon(MOD, "textures/gui/hud/bite.jpg", MOD));
        assertNull(HudIcons.kitIcon("minecraft", "textures/item/diamond_sword.png", MOD));
    }

    @Test
    void everyIconNamedHasItsPictureInTheKit() throws Exception {
        for (String name : HudIcons.OURS) {
            for (int size : new int[] {16, 32}) {
                try (InputStream in = HudIconsTest.class.getResourceAsStream("/assets/supernaturalbloodlines/textures/gui/sprites/icons/" + size + "/" + name + ".png")) {
                    assertNotNull(in, "icons/" + size + "/" + name);
                }
            }
        }
    }
}