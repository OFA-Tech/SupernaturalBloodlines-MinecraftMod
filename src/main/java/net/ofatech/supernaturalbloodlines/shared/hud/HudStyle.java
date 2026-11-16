package net.ofatech.supernaturalbloodlines.shared.hud;

import java.util.function.Supplier;

/**
 * The look of the ability HUD's slots, which follows the hub's theme setting: inked tiles with wax key badges (Grimoire), round slots with gold key
 * letters (Blood Moon), hexagons with key tabs (Tensura). The hub (supernatural part) installs the source; the HUD part reads it, so neither imports the
 * other. Without a source the HUD is Grimoire-shaped.
 */
public final class HudStyle {
    public enum Style { GRIMOIRE, BLOOD_MOON, TENSURA }

    private static volatile Supplier<Style> source = () -> Style.GRIMOIRE;
    private static volatile Style forced;

    private HudStyle() {
    }

    public static void install(Supplier<Style> s) {
        source = s;
    }

    /** Development only (the HUD screenshots): a style regardless of the setting; null to follow the setting again. */
    public static void force(Style style) {
        forced = style;
    }

    public static Style current() {
        Style f = forced;
        return f != null ? f : source.get();
    }
}
