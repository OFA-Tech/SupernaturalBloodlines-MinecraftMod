package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.List;
import java.util.Locale;

/**
 * The names of the art kit's GUI sprites (the ids {@code blitSprite} takes, without the namespace) and the one rule of how an icon is fitted to the box
 * it is drawn in. The pictures are in {@code textures/gui/sprites/}; a sprite that scales has a {@code .mcmeta} next to it. Pure.
 */
public final class KitSprites {
    /** The three theme kits: {@code HubTheme#kit()} is one of these. */
    public static final List<String> KITS = List.of("grimoire", "bloodmoon", "tensura");
    public static final String SHARED = "kit/shared/";

    /** What a part is drawn as, in the theme's own colours, when its sprite is missing. */
    public enum Flat { NONE, SURFACE, ACCENT, TRACK, LINE, RING }

    /**
     * Every theme kit sprite the code draws. The sprite name is the constant in lower case, the same in each of the {@link #KITS}; {@code UiKitSlicesTest}
     * checks that each exists in all three with a valid slice, so a part cannot be added here without its art.
     */
    public enum Part {
        COVER(Flat.SURFACE), COVER_ART(Flat.NONE), PANEL(Flat.SURFACE), INSET(Flat.SURFACE), TOOLTIP(Flat.SURFACE), DIVIDER(Flat.LINE),
        SCROLL_TRACK(Flat.TRACK), SCROLL_THUMB(Flat.ACCENT), SCROLL_THUMB_HOVER(Flat.ACCENT),
        TAB(Flat.SURFACE), TAB_HOVER(Flat.SURFACE), TAB_SELECTED(Flat.ACCENT), TAB_DISABLED(Flat.TRACK),
        BUTTON_NORMAL(Flat.SURFACE, 2), BUTTON_HOVER(Flat.SURFACE, 2), BUTTON_PRESSED(Flat.TRACK, 2), BUTTON_DISABLED(Flat.TRACK, 2), BUTTON_FOCUS(Flat.SURFACE, 2),
        BUTTON_PRIMARY_NORMAL(Flat.ACCENT, 2), BUTTON_PRIMARY_HOVER(Flat.ACCENT, 2), BUTTON_PRIMARY_PRESSED(Flat.ACCENT, 2), BUTTON_PRIMARY_DISABLED(Flat.TRACK, 2),
        BUTTON_PRIMARY_FOCUS(Flat.ACCENT, 2),
        FOCUS(Flat.RING), BAR_TRACK(Flat.TRACK), BAR_FILL(Flat.ACCENT), PLATE(Flat.SURFACE), CHIP(Flat.SURFACE, 1), CHIP_ON(Flat.ACCENT, 1),
        SEAL(Flat.ACCENT), SEAL_HOVER(Flat.ACCENT), SEAL_PRESSED(Flat.TRACK), STAMP(Flat.SURFACE), STAMP_LIT(Flat.ACCENT),
        RUNG_REACHED(Flat.ACCENT), RUNG_CURRENT(Flat.SURFACE), RUNG_LOCKED(Flat.TRACK),
        TASK_CLAIMABLE(Flat.ACCENT), TASK_OPEN(Flat.SURFACE), TASK_CLAIMED(Flat.TRACK), TASK_FROZEN(Flat.TRACK), TASK_UNAVAILABLE(Flat.TRACK),
        PIP_ON(Flat.ACCENT), PIP_OFF(Flat.TRACK), GEM_GOLD(Flat.ACCENT);

        private final Flat flat;
        private final int margin;

        Part(Flat flat) {
            this(flat, 0);
        }

        /** {@code margin}: the transparent border of the picture (room for a glow), so the body of the control is the rect it is drawn for. */
        Part(Flat flat, int margin) {
            this.flat = flat;
            this.margin = margin;
        }

        public int margin() {
            return margin;
        }

        public Flat flat() {
            return flat;
        }

        public String sprite() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private KitSprites() {
    }

    public static String of(String kit, String name) {
        return "kit/" + kit + "/" + name;
    }

    public static String icon(int size, String name) {
        return "icons/" + size + "/" + name.toLowerCase(Locale.ROOT);
    }

    /** The icon of a skill: its own picture (the sprite is named by the skill id, any case), the "unknown" rune when the skill has none. */
    public static String skillIcon(String skillId, java.util.function.Predicate<String> has16) {
        String name = skillId.toLowerCase(Locale.ROOT);
        return has16.test(icon(16, name)) ? name : "unknown";
    }

    /** Which icon sprite, and at what size, fills a box: pixel art is only ever drawn at a whole multiple (or half) of its own size. */
    public record IconPlan(int sprite, int draw) {
    }

    /** A box of 64 or more takes the 32 px icon doubled, 32 or more the 32 px icon, 12 or more the 16 px icon as it is, less the 16 px icon halved. */
    public static IconPlan iconPlan(int box) {
        if (box >= 64) {
            return new IconPlan(32, 64);
        }
        if (box >= 32) {
            return new IconPlan(32, 32);
        }
        if (box >= 12) {
            return new IconPlan(16, 16);
        }
        return new IconPlan(16, 8);
    }
}
