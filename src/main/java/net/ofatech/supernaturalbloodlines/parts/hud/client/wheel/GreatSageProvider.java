package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/**
 * Great Sage's powers on the wheel. They live on the client (the mod runs them there: toggles, a held scan, a request to its own server code), so this reaches them by
 * reflection with one probe at startup; a method that is missing is logged once and the whole mod's entries disappear, never a crash. The wheel lists nothing unless
 * Great Sage's own rule says the player may use it. Verified against Great Sage 0.0.7.1: {@code MagicSense.toggle()}, {@code AssessMode.toggle() / press() / release() /
 * active()}, {@code GreatSageKeys.menu()}, {@code GreatSageRules.allows(LocalPlayer)} and {@code ThoughtClient.request()} are public static.
 */
public final class GreatSageProvider {
    public static final String MOD = "greatsage";
    static final ResourceLocation MAGIC_SENSE = id("magic_sense");
    static final ResourceLocation ASSESS = id("assess");
    static final ResourceLocation SCAN = id("scan");
    static final ResourceLocation THOUGHT = id("thought_acceleration");
    static final ResourceLocation MENU = id("menu");

    /** The reflective handles of one run; a null field is a method this Great Sage does not have. */
    private record Handles(Method magicToggle, Method magicActive, Method assessToggle, Method assessPress, Method assessRelease, Method assessActive, Method menu, Method allows,
                           Method thought) {
        boolean usable() {
            return magicToggle != null && assessToggle != null && assessPress != null && assessRelease != null && menu != null && allows != null;
        }
    }

    private static boolean probed;
    private static Handles handles;

    private GreatSageProvider() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD, path);
    }

    private static Method find(String cls, String name, Class<?>... args) {
        try {
            Class<?> c = Class.forName("dev.dedek.greatsage.client." + cls);
            Method m = c.getDeclaredMethod(name, args);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError missing) {
            SupernaturalBloodlines.LOGGER.warn("Great Sage has no {}.{}: its wheel entries stay off ({})", cls, name, missing.toString());
            return null;
        }
    }

    private static synchronized Handles handles() {
        if (!probed) {
            probed = true;
            if (ModList.get().isLoaded(MOD)) {
                handles = new Handles(find("MagicSense", "toggle"), find("MagicSense", "active"), find("AssessMode", "toggle"), find("AssessMode", "press"),
                        find("AssessMode", "release"), find("AssessMode", "active"), find("GreatSageKeys", "menu"), find("GreatSageRules", "allows", LocalPlayer.class),
                        find("ThoughtClient", "request"));
                if (!handles.usable()) {
                    handles = null;
                }
            }
        }
        return handles;
    }

    private static boolean call(Method m) {
        try {
            m.invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SupernaturalBloodlines.LOGGER.warn("Great Sage call {} failed: {}", m.getName(), e.toString());
            return false;
        }
    }

    private static boolean flag(Method m) {
        if (m == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(m.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** True when the mod is there and every method the wheel needs was found. */
    public static boolean available() {
        return handles() != null;
    }

    private static boolean allowed(LocalPlayer p) {
        try {
            return Boolean.TRUE.equals(handles().allows().invoke(null, p));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static WheelEntry entry(ResourceLocation id, PowerKind kind, boolean on, String icon) {
        return new WheelEntry(new PowerRef(PowerSource.GREAT_SAGE, id), PowerGroup.MAIN, kind, Component.translatable("supernaturalbloodlines.wheel.greatsage." + id.getPath()),
                Component.empty(), List.of(), ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/sprites/icons/32/" + icon + ".png"),
                on ? new PowerState(true, 0L, 0, net.ofatech.supernaturalbloodlines.core.wheel.LockReason.NONE, 0, 1) : PowerState.READY, false, -1f);
    }

    /** The entries to add to the server's list (none unless the mod works and lets this player use it). Thought Acceleration is listed only when its call was found. */
    public static List<WheelEntry> entries(LocalPlayer p) {
        Handles h = handles();
        if (h == null || p == null || !allowed(p)) {
            return List.of();
        }
        List<WheelEntry> out = new ArrayList<>();
        out.add(entry(MAGIC_SENSE, PowerKind.TOGGLE, flag(h.magicActive()), "eye"));
        out.add(entry(ASSESS, PowerKind.TOGGLE, flag(h.assessActive()), "info"));
        out.add(entry(SCAN, PowerKind.HELD, false, "spark"));
        if (h.thought() != null) {
            out.add(entry(THOUGHT, PowerKind.INSTANT, false, "bolt"));
        } else {
            SupernaturalBloodlines.LOGGER.warn("Great Sage has no thought acceleration request: it is not on the wheel");
        }
        out.add(entry(MENU, PowerKind.INSTANT, false, "guide"));
        return out;
    }

    /** Runs one of Great Sage's powers: a press, or {@code release} for the held scan. False when it could not. */
    public static boolean run(ResourceLocation id, boolean release) {
        Handles h = handles();
        if (h == null) {
            return false;
        }
        if (id.equals(MAGIC_SENSE)) {
            return !release && call(h.magicToggle());
        }
        if (id.equals(ASSESS)) {
            return !release && call(h.assessToggle());
        }
        if (id.equals(SCAN)) {
            return call(release ? h.assessRelease() : h.assessPress());
        }
        if (id.equals(THOUGHT)) {
            return !release && h.thought() != null && call(h.thought());
        }
        if (id.equals(MENU)) {
            if (release) {
                return false;
            }
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                Object screen = h.menu().invoke(null);
                if (screen instanceof net.minecraft.client.gui.screens.Screen s) {
                    mc.setScreen(s);
                    return true;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                SupernaturalBloodlines.LOGGER.warn("Great Sage menu failed: {}", e.toString());
            }
        }
        return false;
    }
}
