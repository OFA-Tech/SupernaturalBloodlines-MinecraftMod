package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HudIconsTest {
    private static final String MOD = "supernaturalbloodlines";
    private static final String SPRITES = "/assets/supernaturalbloodlines/textures/gui/sprites/icons/";

    @Test
    void ourOwnHudTexturesHaveAKitIcon() {
        assertEquals("bite", HudIcons.kitIcon(MOD, "textures/gui/hud/bite.png", MOD));
        assertEquals("alpha_roar", HudIcons.kitIcon(MOD, "textures/gui/hud/alpha_roar.png", MOD));
        assertEquals("unknown", HudIcons.kitIcon(MOD, "textures/gui/hud/unknown.png", MOD), "the placeholder rune is dark too");
    }

    @Test
    void anotherModsTextureOrAnotherPlaceOfOurOwnKeepsItsPicture() {
        assertNull(HudIcons.kitIcon("tensura", "textures/gui/hud/bite.png", MOD), "another mod's bite");
        assertNull(HudIcons.kitIcon(MOD, "textures/gui/hud/blood_full.png", MOD), "the food bar's blood point is not a slot icon");
        assertNull(HudIcons.kitIcon(MOD, "textures/item/bite.png", MOD));
        assertNull(HudIcons.kitIcon(MOD, "textures/gui/hud/bite.jpg", MOD));
        assertNull(HudIcons.kitIcon("minecraft", "textures/item/diamond_sword.png", MOD));
    }

    @Test
    void everyIconNamedHasItsPicturesInTheKitAndItsOutlinedTwinForTheHud() throws Exception {
        for (String name : HudIcons.OURS) {
            for (String dir : new String[] {"16/", "32/", "hud/"}) {
                try (InputStream in = HudIconsTest.class.getResourceAsStream(SPRITES + dir + name + ".png")) {
                    assertNotNull(in, "icons/" + dir + name);
                }
            }
        }
    }

    @Test
    void everyPictureInTheHudFolderIsEitherASlotIconWithATwinOrNamedAsNotOne() throws Exception {
        File at = new File("").getAbsoluteFile();
        while (!new File(at, "src/main/resources").isDirectory()) {
            at = at.getParentFile();
        }
        File dir = new File(at, "src/main/resources/assets/supernaturalbloodlines/textures/gui/hud");
        Set<String> seen = new HashSet<>();
        for (File f : dir.listFiles((d, n) -> n.endsWith(".png"))) {
            String name = f.getName().substring(0, f.getName().length() - 4);
            seen.add(name);
            assertTrue(HudIcons.OURS.contains(name) ^ HudIcons.NOT_ICONS.contains(name), name + " is neither a slot icon with an outlined twin nor named as not one (a new dark icon would vanish on the dark slots)");
        }
        assertTrue(seen.containsAll(HudIcons.OURS) && seen.containsAll(HudIcons.NOT_ICONS), "and every name listed is a picture that exists");
    }
}
