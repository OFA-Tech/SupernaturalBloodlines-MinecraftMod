package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;

/** The hub's client settings (each player picks their own look). The theme is never sent to or read by the server. */
public final class HubClientConfig {
    public enum ThemeId {
        GRIMOIRE, BLOOD_MOON, TENSURA;

        /** The id a part registers the theme under ({@code HubHooks.addTheme}). */
        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Which page the hub opens on: Home the first time in a session (and whenever a request waits), or always the page the player left. */
    public enum OpenOn { HOME, LAST_PAGE }

    public static final String FILE = SupernaturalBloodlines.MODID + "-hub-client.toml";
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<ThemeId> HUB_THEME;
    private static final ModConfigSpec.EnumValue<OpenOn> OPEN_ON;
    private static final ModConfigSpec.BooleanValue REDUCE_MOTION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HUB_THEME = b.comment("The hub's look: GRIMOIRE (default, an open book), BLOOD_MOON (a red moon that fills with your ladder) or TENSURA",
                        "(Tensura's menu style, wider; needs Tensura, else the Grimoire). It can be changed while the hub is open.")
                .defineEnum("hub_theme", ThemeId.GRIMOIRE);
        OPEN_ON = b.comment("Which page the hub opens on: HOME (the first time in a session, and whenever a request waits) or LAST_PAGE (the page you left;",
                        "only a waiting request opens Home).").defineEnum("open_on", OpenOn.HOME);
        REDUCE_MOTION = b.comment("Turn off the interface's own motion: page slides, press and unlock flourishes, glow pulses and our screen shake, in the hub, the HUD",
                "and the boss bars. A change still lands at once. Cooldown sweeps stay (they are information); world animations (boss shifts, particles) are not touched.")
                .define("reduce_motion", false);
        SPEC = b.build();
        // the HUD and the boss bars read it too, and may be built without the hub part
        ReduceMotion.install(HubClientConfig::reduceMotion);
    }

    /** True when the player turned the interface's motion off. */
    public static boolean reduceMotion() {
        try {
            return REDUCE_MOTION.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    private HubClientConfig() {
    }

    /** True for "Home first"; false means the page the player left (a waiting request still opens Home). */
    public static boolean openOnHome() {
        try {
            return OPEN_ON.get() == OpenOn.HOME;
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    public static OpenOn openOn() {
        try {
            return OPEN_ON.get();
        } catch (IllegalStateException notLoaded) {
            return OpenOn.HOME;
        }
    }

    public static void setOpenOn(OpenOn value) {
        try {
            OPEN_ON.set(value);
            OPEN_ON.save();
        } catch (IllegalStateException notLoaded) {
            // not loaded: no change
        }
    }

    public static ThemeId theme() {
        try {
            return HUB_THEME.get();
        } catch (IllegalStateException notLoaded) {
            return ThemeId.GRIMOIRE;
        }
    }

    /** The theme a press of the hub's theme button shows after {@code current}: the next one that can be drawn now (the Grimoire always can). */
    public static ThemeId next(ThemeId current, java.util.function.Predicate<String> available) {
        ThemeId[] all = ThemeId.values();
        for (int i = 1; i <= all.length; i++) {
            ThemeId c = all[(current.ordinal() + i) % all.length];
            if (c == ThemeId.GRIMOIRE || available.test(c.id())) {
                return c;
            }
        }
        return current;
    }

    /** Saves the look (the hub button and the config file are the same setting). */
    public static void setTheme(ThemeId theme) {
        try {
            HUB_THEME.set(theme);
            HUB_THEME.save();
        } catch (IllegalStateException notLoaded) {
            // the config is not loaded (a test, or too early): the screen keeps the look for this session only
        }
    }

    /** The theme that is drawn for the one chosen: itself when a part registered it and it can be drawn now (Tensura's needs Tensura), else the Grimoire. */
    public static ThemeId resolve(ThemeId chosen, java.util.function.Predicate<String> registered) {
        return registered.test(chosen.id()) ? chosen : ThemeId.GRIMOIRE;
    }
}
