package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;

/** The numbers behind the kit's small widgets (bars, scroll bars, marks): where the picture's inside begins, how long a fill or a thumb is, which picture a state wears. Pure. */
public final class KitMetrics {
    /** The height of the bar track picture; a bar is drawn at this height, centred on the box it was asked for. */
    public static final int BAR_H = 8;
    /** The height of the bar fill picture, and the track's inset above and below it. */
    public static final int BAR_FILL_H = 4;
    /** The width of the scroll bar pictures. */
    public static final int SCROLL_W = 7;
    /** The height of the divider picture; a rule is its middle line. */
    public static final int DIVIDER_H = 7;
    /** A thumb shorter than its two ends would not look like one. */
    public static final int MIN_THUMB = 10;

    private KitMetrics() {
    }

    /** How many pixels of the track picture are edge on the left and right: the fill starts inside them. {@code UiKitSlicesTest} compares these with the {@code .mcmeta}. */
    public static int barInset(String kit) {
        return switch (kit) {
            case "bloodmoon" -> 4;
            case "tensura" -> 2;
            default -> 3;
        };
    }

    /** The length of a fill in a track {@code inner} pixels long: a part that is not empty is at least one pixel, a full one fills the track. */
    public static int barFill(int inner, float fraction) {
        float f = Float.isNaN(fraction) ? 0f : Math.max(0f, Math.min(1f, fraction));
        int len = Math.round(inner * f);
        return f > 0f ? Math.max(1, len) : 0;
    }

    /** The scroll bar thumb of a region {@code h} tall that scrolled {@code offset} of {@code max}: its distance from the top and its length. */
    public record Thumb(int top, int length) {
    }

    public static Thumb thumb(int h, int offset, int max) {
        int length = Math.min(h, Math.max(MIN_THUMB, h * h / (h + Math.max(0, max))));
        int top = (h - length) * Math.max(0, Math.min(max, offset)) / Math.max(1, max);
        return new Thumb(top, length);
    }

    public static Part rung(RungStatus status) {
        return switch (status) {
            case REACHED -> Part.RUNG_REACHED;
            case CURRENT -> Part.RUNG_CURRENT;
            case LOCKED -> Part.RUNG_LOCKED;
        };
    }

    public static Part task(TaskState state) {
        return switch (state) {
            case CLAIMABLE -> Part.TASK_CLAIMABLE;
            case OPEN -> Part.TASK_OPEN;
            case CLAIMED -> Part.TASK_CLAIMED;
            case FROZEN -> Part.TASK_FROZEN;
            case UNAVAILABLE -> Part.TASK_UNAVAILABLE;
        };
    }
}
