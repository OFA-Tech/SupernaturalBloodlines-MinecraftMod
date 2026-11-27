package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** Where a player's face, its paper and its square frame go: the frame is 2 px outside the face on each side (1 px border, 1 px paper). Pure. */
public final class PortraitGeometry {
    public record Boxes(Rect frame, Rect paper, Rect face) {
    }

    private PortraitGeometry() {
    }

    /** The boxes of a face of {@code size} centred on {@code (cx, cy)}. */
    public static Boxes of(int cx, int cy, int size) {
        int x = cx - size / 2;
        int y = cy - size / 2;
        return new Boxes(new Rect(x - 2, y - 2, size + 4, size + 4), new Rect(x - 1, y - 1, size + 2, size + 2), new Rect(x, y, size, size));
    }

    /** The centre x that puts the frame's left edge on {@code left}. */
    public static int cxAt(int left, int size) {
        return left + size / 2 + 2;
    }

    /** Where text beside a portrait whose frame starts at {@code left} begins: 4 px clear of the frame. */
    public static int besideX(int left, int size) {
        return left + size + 8;
    }
}
