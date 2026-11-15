package net.ofatech.supernaturalbloodlines.core.hud;

/** Tensura mode shows the active preset (left column) and the next one (right column). Tensura has 9 presets, and preset 9 pairs with preset 1. */
public final class PresetPair {
    public static final int TENSURA_PRESETS = 9;

    private PresetPair() {
    }

    public static int left(int active) {
        return active;
    }

    /** The preset after {@code active}, wrapping; an out of range value is wrapped, never a crash. */
    public static int right(int active, int presetCount) {
        return presetCount <= 0 ? 0 : Math.floorMod(active + 1, presetCount);
    }
}
