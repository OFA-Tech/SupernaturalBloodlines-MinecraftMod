package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.Locale;

/**
 * Our own 16 x 16 pixel sprites (Minecraft's font has no emoji): one atlas, {@link #COLUMNS} cells a row, white on transparent so a
 * theme tints it. The order is the cell order and must match {@code icons.txt}, which {@code tools/gen_icons.py} writes with the atlas
 * (a test compares them). Pure.
 */
public enum Icons {
    PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK, GUIDE, LOADOUT,
    CHECK, LOCK, CROWN, DOT, CLOSE, THEME, LAUNCH, DROP,
    MOON_NEW, MOON_WAXING_CRESCENT, MOON_FIRST_QUARTER, MOON_WAXING_GIBBOUS, MOON_FULL, MOON_WANING_GIBBOUS, MOON_LAST_QUARTER,
    MOON_WANING_CRESCENT,
    SUN, SHIELD, FANG, VIAL, QUESTION, INFO, PLUS, ARROW_LEFT, ARROW_RIGHT, CLAW, HEART, KEY,
    BOOT, EYE, VOICE, SPARK, BOLT;

    public static final int CELL = 16;
    public static final int COLUMNS = 16;
    public static final int ROWS = 4;
    public static final String TEXTURE = "textures/gui/icons.png";

    public int column() {
        return ordinal() % COLUMNS;
    }

    public int row() {
        return ordinal() / COLUMNS;
    }

    /** The name in {@code icons.txt}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The sprite of a hub page (its tab, its rail button, its list row). */
    public static Icons of(net.ofatech.supernaturalbloodlines.shared.hub.HubPage page) {
        return switch (page) {
            case PATH -> PATH;
            case SKILLS -> SKILLS;
            case QUESTS -> QUESTS;
            case RITES -> RITES;
            case BLOODLINE -> BLOODLINE;
            case PACK -> Icons.PACK;
            case GUIDE -> GUIDE;
            case LOADOUT -> LOADOUT;
        };
    }

    /** The moon of a vanilla moon phase (0 is the full moon, 4 the new moon). */
    public static Icons moon(int vanillaPhase) {
        return switch (Math.floorMod(vanillaPhase, 8)) {
            case 0 -> MOON_FULL;
            case 1 -> MOON_WANING_GIBBOUS;
            case 2 -> MOON_LAST_QUARTER;
            case 3 -> MOON_WANING_CRESCENT;
            case 4 -> MOON_NEW;
            case 5 -> MOON_WAXING_CRESCENT;
            case 6 -> MOON_FIRST_QUARTER;
            default -> MOON_WAXING_GIBBOUS;
        };
    }
}
