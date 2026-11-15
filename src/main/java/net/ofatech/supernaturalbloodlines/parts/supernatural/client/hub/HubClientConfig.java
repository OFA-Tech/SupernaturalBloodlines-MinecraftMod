package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The hub's client settings (each player picks their own look). The theme is never sent to or read by the server. */
public final class HubClientConfig {
    public enum ThemeId {
        GRIMOIRE, BLOOD_MOON, TENSURA;

        /** The id a part registers the theme under ({@code HubHooks.addTheme}). */
        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final String FILE = SupernaturalBloodlines.MODID + "-hub-client.toml";
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<ThemeId> HUB_THEME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HUB_THEME = b.comment("The hub's look: GRIMOIRE (default, an open book), BLOOD_MOON (a red moon that fills with your ladder) or TENSURA",
                        "(Tensura's menu style, wider; needs Tensura, else the Grimoire). It can be changed while the hub is open.")
                .defineEnum("hub_theme", ThemeId.GRIMOIRE);
        SPEC = b.build();
    }

    private HubClientConfig() {
    }

    public static ThemeId theme() {
        try {
            return HUB_THEME.get();
        } catch (IllegalStateException notLoaded) {
            return ThemeId.GRIMOIRE;
        }
    }

    /** The theme that is drawn for the one chosen: itself when a part registered it and it can be drawn now (Tensura's needs Tensura), else the Grimoire. */
    public static ThemeId resolve(ThemeId chosen, java.util.function.Predicate<String> registered) {
        return registered.test(chosen.id()) ? chosen : ThemeId.GRIMOIRE;
    }
}
