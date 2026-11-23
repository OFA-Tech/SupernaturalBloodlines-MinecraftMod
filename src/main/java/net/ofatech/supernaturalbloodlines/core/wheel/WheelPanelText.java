package net.ofatech.supernaturalbloodlines.core.wheel;

/** How much room the side panel has for the power's description: what is left between the kind line and the slot rows after the mode line and the lock box. Pure. */
public final class WheelPanelText {
    public static final int LINE = 10;
    public static final int LOCK_BOX = 42;
    public static final int MAX_LINES = 4;

    private WheelPanelText() {
    }

    /**
     * @param top       y where the description would start
     * @param bottom    y of the first row that must stay free (the slot row)
     * @param modeLines how many lines the mode text takes (0 for a power without modes)
     * @param locked    the lock box is drawn
     */
    public static int descriptionLines(int top, int bottom, int modeLines, boolean locked) {
        int free = bottom - top - modeLines * LINE - (locked ? LOCK_BOX : 0) - 2;
        return Math.max(0, Math.min(MAX_LINES, free / LINE));
    }
}
