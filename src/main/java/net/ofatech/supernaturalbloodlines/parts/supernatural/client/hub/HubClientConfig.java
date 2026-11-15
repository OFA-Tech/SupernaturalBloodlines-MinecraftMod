package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The hub's client settings (each player picks their own look). The theme is never sent to or read by the server. */
public final class HubClientConfig {
    public enum ThemeId { GRIMOIRE, BLOOD_MOON, TENSURA }

    public static final String FILE = SupernaturalBloodlines.MODID + "-hub-client.toml";
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<ThemeId> HUB_THEME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HUB_THEME = b.comment("The hub's look. GRIMOIRE (default): an open book. BLOOD_MOON and TENSURA are not built yet and show the Grimoire;",
                        "TENSURA also falls back to the Grimoire without Tensura.")
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

    /** The theme that is drawn for the one chosen: the Grimoire until the others are built (12f), and for Tensura's without Tensura. */
    public static ThemeId resolve(ThemeId chosen, boolean tensuraLoaded) {
        if (chosen == ThemeId.TENSURA && !tensuraLoaded) {
            return ThemeId.GRIMOIRE;
        }
        // shortcut: Blood Moon and Tensura are phase 12f; return `chosen` here once they exist
        return ThemeId.GRIMOIRE;
    }
}
