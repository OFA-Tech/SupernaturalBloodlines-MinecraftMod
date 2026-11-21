package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/** Development only: six demo slots for the HUD screenshots, so every state (cooldown, active, locked, Bite's blood ring) can be photographed. Used only while {@link DevHud#on}. */
final class HudDemo implements HudSource {
    private static ResourceLocation icon(String item) {
        return ResourceLocation.withDefaultNamespace("textures/item/" + item + ".png");
    }

    @Override
    public SlotView[] views() {
        if (DevHud.human) {
            return humanViews();
        }
        return new SlotView[] {
                new SlotView(ClientLoadout.BITE_ICON, "R", 0.35f, 7, false, null, "Bite", "Vampire feed (human form), blood 62%"),
                new SlotView(icon("diamond_sword"), "F", 0f, 0, true, null, "Predator's Thirst", "toggled on"),
                new SlotView(icon("stick"), "V", 0.6f, 4, false, null, "Venom Fang", null),
                new SlotView(icon("golden_apple"), "Z", 0f, 0, false, null, "Regeneration", null),
                new SlotView(icon("ender_eye"), "X", 0f, 0, false, "supernaturalbloodlines.hud.slot.locked", "Moon Howl", null),
                new SlotView(ResourceLocation.fromNamespaceAndPath("supernaturalbloodlines", "textures/gui/hud/alpha_roar.png"), "C", 0.5f, 30, false, null, "Alpha Roar", "Your pack gets Strength and Speed")};
    }

    /** A plain human with Tensura: Tensura skills only; the third skill is cooling down, the seconds counting down from 30 as the game runs. */
    private static SlotView[] humanViews() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        long t = level == null ? 0 : level.getGameTime();
        int left = DevHud.cooldown ? 30 - (int) (t / 20 % 30) : 0;
        return new SlotView[] {
                new SlotView(icon("iron_sword"), "R", 0f, 0, false, null, "Predator", null).withMastery(Mastery.of(42, 100, 0, false)).withDescription("Eats what it kills"),
                new SlotView(icon("bow"), "F", 0f, 0, false, null, "Sharp Sight", null).withMastery(Mastery.of(100, 100, 0, true)).withDescription("Sees far and clear"),
                new SlotView(icon("amethyst_shard"), "V", left / 30f, left, false, null, "Water Blade", "Blade mode").withMastery(Mastery.of(7, 100, 0, false)).withDescription("Cuts with water"),
                new SlotView(icon("golden_apple"), "Z", 0f, 0, false, "supernaturalbloodlines.hud.slot.cannot_use", "Poison Resistance", null).withMastery(Mastery.of(-40, 100, 100, false)).withDescription("Resists poison"),
                new SlotView(icon("ender_eye"), "X", 0f, 0, true, null, "Great Sage", null).withMastery(Mastery.of(88, 100, 100, false)).withDescription("Thinks for you"),
                SlotView.EMPTY.withKey("C")};
    }

    @Override
    public SlotMode mode() {
        return DevHud.human ? SlotMode.TENSURA : SlotMode.SUPERNATURAL;
    }

    @Override
    public SlotMode other() {
        return DevHud.human ? null : SlotMode.TENSURA;
    }

    @Override
    public SlotView[] otherViews(SlotMode other) {
        return new SlotView[] {
                new SlotView(icon("iron_sword"), "R", 0f, 0, false, null, "", null),
                new SlotView(icon("bow"), "F", 0f, 0, false, null, "", null),
                new SlotView(icon("iron_helmet"), "V", 0f, 0, false, null, "", null),
                new SlotView(icon("amethyst_shard"), "Z", 0f, 0, false, null, "", null),
                new SlotView(icon("nether_star"), "X", 0f, 0, false, null, "", null),
                new SlotView(icon("heart_of_the_sea"), "C", 0f, 0, false, null, "", null)};
    }

    @Override
    public boolean alt() {
        return DevHud.alt;
    }

    @Override
    public long sinceSwitch() {
        return DevHud.switched ? 0 : -1;
    }

    @Override
    public boolean actionBar() {
        return false;
    }

    @Override
    public float blood() {
        return 0.62f;
    }

    @Override
    public long moonTicks() {
        return 2000;
    }

    @Override
    public Optional<TrackerAccess.Line> tracked() {
        return Optional.of(new TrackerAccess.Line("Endure thirst of 15 or more", 1, 3));
    }

    @Override
    public int bossBottom() {
        return 0;
    }

    @Override
    public boolean cacheable() {
        return false;
    }
}
