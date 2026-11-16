package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/** Development only: six demo slots for the HUD screenshots, so every state (cooldown, active, locked, Bite's blood ring) can be photographed. Inert unless {@link DevHud#on}. */
final class HudDemo {
    private HudDemo() {
    }

    private static ResourceLocation icon(String item) {
        return ResourceLocation.withDefaultNamespace("textures/item/" + item + ".png");
    }

    static net.ofatech.supernaturalbloodlines.core.hud.SlotMode mode() {
        return net.ofatech.supernaturalbloodlines.core.hud.SlotMode.SUPERNATURAL;
    }

    static boolean on() {
        return DevHud.on;
    }

    static boolean alt() {
        return DevHud.on && DevHud.alt;
    }

    static SlotView[] views() {
        return new SlotView[] {
                new SlotView(ClientLoadout.BITE_ICON, "R", 0.35f, 7, false, null, "Bite", "Vampire feed (human form), blood 62%"),
                new SlotView(icon("diamond_sword"), "F", 0f, 0, true, null, "Predator's Thirst", "toggled on"),
                new SlotView(icon("stick"), "V", 0.6f, 4, false, null, "Venom Fang", null),
                new SlotView(icon("golden_apple"), "Z", 0f, 0, false, null, "Regeneration", null),
                new SlotView(icon("ender_eye"), "X", 0f, 0, false, "supernaturalbloodlines.hud.slot.locked", "Moon Howl", null),
                new SlotView(icon("blaze_powder"), "C", 0f, 0, false, null, "Bat Form", null)};
    }

    static SlotView[] otherViews() {
        return new SlotView[] {
                new SlotView(icon("iron_sword"), "R", 0f, 0, false, null, "", null),
                new SlotView(icon("bow"), "F", 0f, 0, false, null, "", null),
                new SlotView(icon("iron_helmet"), "V", 0f, 0, false, null, "", null),
                new SlotView(icon("amethyst_shard"), "Z", 0f, 0, false, null, "", null),
                new SlotView(icon("nether_star"), "X", 0f, 0, false, null, "", null),
                new SlotView(icon("heart_of_the_sea"), "C", 0f, 0, false, null, "", null)};
    }

    static float blood() {
        return 0.62f;
    }

    static long moonTicks() {
        return 2000;
    }

    static TrackerAccess.Line tracked() {
        return new TrackerAccess.Line("Endure thirst of 15 or more", 1, 3);
    }
}
