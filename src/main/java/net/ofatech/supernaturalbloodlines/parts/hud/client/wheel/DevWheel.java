package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.GestureMachine;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelModel;
import net.ofatech.supernaturalbloodlines.parts.hud.net.ClientCatalogue;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelCataloguePayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;

/**
 * Development only: the demo wheel of the screenshots ({@code -Dsupernaturalbloodlines.devshots=wheel:<look>:<state>}, states open, alt, page, picker, aim, closed). It fills
 * the client catalogue with sample powers (the mockups' data, our own 32 px icons) and opens the screen in that state. Inert unless the driver calls it.
 */
public final class DevWheel {
    static volatile boolean active;
    static volatile boolean alt;
    static volatile List<ResourceLocation> holds;

    private DevWheel() {
    }

    public static void install() {
        DevHud.wheelShow = DevWheel::show;
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/sprites/icons/32/" + name + ".png");
    }

    private static WheelEntry power(PowerSource source, String id, PowerGroup group, PowerKind kind, String name, String icon, PowerState state, String... modes) {
        List<Component> names = new ArrayList<>();
        for (String m : modes) {
            names.add(Component.literal(m));
        }
        return new WheelEntry(new PowerRef(source, ResourceLocation.fromNamespaceAndPath(source.modId(), id)), group, kind, Component.literal(name),
                modes.length == 0 ? Component.empty() : names.get(Math.min(state.mode(), names.size() - 1)), names, icon(icon),
                new PowerState(state.on(), state.cooldownEnd(), state.cooldownTotal(), state.lock(), state.mode(), Math.max(1, modes.length)), true, 0.4f);
    }

    private static PowerState st(boolean on, long end, int total, LockReason lock, int mode) {
        return new PowerState(on, end, total, lock, mode, 1);
    }

    static List<WheelEntry> demo(long now) {
        PowerState ready = PowerState.READY;
        List<WheelEntry> out = new ArrayList<>();
        PowerSource t = PowerSource.TENSURA;
        PowerGroup sk = PowerGroup.SKILLS;
        out.add(power(PowerSource.BLOODLINE, "bite", PowerGroup.MAIN, PowerKind.HELD, "Bite", "fang", ready));
        out.add(power(PowerSource.VAMPIRISM, "rage", PowerGroup.MAIN, PowerKind.TOGGLE, "Rage", "claw", ready));
        out.add(power(PowerSource.WEREWOLVES, "howl", PowerGroup.MAIN, PowerKind.INSTANT, "Howl", "wolf_spirits", ready));
        out.add(power(PowerSource.INFINITY, "soul", PowerGroup.MAIN, PowerKind.INSTANT, "Soul", "spark", ready));
        out.add(power(PowerSource.GREAT_SAGE, "scan", PowerGroup.MAIN, PowerKind.HELD, "Scan", "eye", ready));
        out.add(power(t, "predator", sk, PowerKind.INSTANT, "Predator", "fang", ready, "Devour", "Analyze", "Stomach"));
        out.add(power(t, "ultraspeed", sk, PowerKind.TOGGLE, "Ultraspeed", "boot", st(true, 0, 0, LockReason.NONE, 0)));
        out.add(power(t, "infinite_regeneration", sk, PowerKind.TOGGLE, "Infinite Regeneration", "heart", st(false, now + 240, 240, LockReason.NONE, 0)));
        out.add(power(t, "sharp_sight", sk, PowerKind.INSTANT, "Sharp Sight", "spark", ready));
        out.add(power(t, "black_lightning", sk, PowerKind.AIMED, "Black Lightning", "bolt", ready));
        out.add(power(t, "heat_wave", sk, PowerKind.TOGGLE, "Heat Wave", "sun", st(false, 0, 0, LockReason.NOT_UNLOCKED, 0)));
        out.add(power(t, "true_sight", sk, PowerKind.TOGGLE, "True Sight", "eye", ready));
        out.add(power(t, "beast_form", sk, PowerKind.INSTANT, "Beast Form", "wolf_spirits", ready));
        out.add(power(t, "healing_potion", sk, PowerKind.INSTANT, "Healing Potion", "vial", ready));
        out.add(power(t, "sticky_thread", sk, PowerKind.INSTANT, "Sticky Thread", "claw", ready, "Bind", "Pull", "Cut"));
        String[] icons = {"heart", "vial", "shield", "key", "spark", "wolf_spirits", "drop", "shield", "sun", "lock", "moon_full", "heart"};
        for (int i = 0; i < 27; i++) {
            // page 2 of 3 is spells 12 to 23: the moon is on, the vial cools down, Thunder Spear (hovered in the shot) is held, the last is locked
            String name = i == 12 ? "Full Moon" : i == 13 ? "Blood Drop" : i == 14 ? "Healing Mist" : i == 15 ? "Thunder Spear" : i == 23 ? "Ritual Seal" : "Spell " + (i + 1);
            String icon = i == 12 ? "moon_full" : i == 13 ? "drop" : i == 14 ? "vial" : i == 15 ? "bolt" : i == 23 ? "lock" : icons[i % icons.length];
            PowerState s = i == 12 ? st(true, 0, 0, LockReason.NONE, 0) : i == 14 ? st(false, now + 140, 140, LockReason.NONE, 0) : i == 23 ? st(false, 0, 0, LockReason.NOT_UNLOCKED, 0) : ready;
            out.add(power(t, "spell_" + i, PowerGroup.MAGIC, i == 15 ? PowerKind.HELD : PowerKind.INSTANT, name, icon, s));
        }        out.add(power(t, "heavy_slash", PowerGroup.BATTLEWILL, PowerKind.INSTANT, "Heavy Slash", "claw", ready));
        out.add(power(t, "air_flight", PowerGroup.BATTLEWILL, PowerKind.HELD, "Air Flight", "boot", ready));
        return out;
    }

    /** {@code state}: open, alt, page, picker, aim or closed. Called by the screenshot driver on the client thread. */
    public static void show(String state) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        mc.setScreen(null); // an open wheel is removed first: its removal clears these flags
        active = true;
        alt = "alt".equals(state);
        holds = null;
        WheelActions.reset();
        WheelDescriptions.demo = Map.of("heat_wave", "Scorching air burns everything near you.", "predator", "Devour a target to copy its body and skills. The mode picks what it does.",
                "black_lightning", "Call down black lightning on the spot you aim at.", "spell_15", "Hold to charge, then let go to throw a spear of lightning.");
        ClientCatalogue.frozen = true;
        ClientCatalogue.set(new WheelCataloguePayload(1, demo(now)));
        WheelList.reset();
        if ("closed".equals(state)) {
            mc.setScreen(null);
            return;
        }
        if ("aim".equals(state)) {
            DevHud.on = true;
            WheelEntry bolt = demo(now).get(9 + 5);
            WheelActions.aim().start(bolt.ref(), 0);
            WheelActions.devAimEntry(bolt);
            mc.setScreen(null);
            return;
        }
        WheelScreen screen = new WheelScreen(GestureMachine.OPEN_STICKY);
        mc.setScreen(screen);
        switch (state) {
            case "alt" -> {
                screen.devOpen(new WheelModel.Open(3, 0, 0));
                screen.devHover(Ring.OUTER, 0);
            }
            case "page" -> {
                screen.devOpen(new WheelModel.Open(3, 1, 1));
                screen.devHover(Ring.OUTER, 3);
            }
            case "picker" -> {
                screen.devOpen(new WheelModel.Open(3, 0, 0));
                screen.devHover(Ring.OUTER, 4);
                screen.devPicker(4);
                holds = java.util.Arrays.asList(id("predator"), id("ultraspeed"), id("sharp_sight"), null, id("true_sight"), null);
            }
            default -> {
                screen.devOpen(new WheelModel.Open(3, 0, 0));
                screen.devHover(Ring.OUTER, 5);
            }
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("tensura", path);
    }

    /** The slots the picker shows in a screenshot: the demo's, not the real player's. */
    static List<ResourceLocation> devHolds() {
        return holds;
    }
}
