package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * Turning the emblem's player model by dragging. Vanilla's {@code renderEntityInInventoryFollowsMouse} turns the body toward a point, so a
 * drag moves that point sideways from the model's centre (clamped), and it stays where it was left. Pure.
 */
public final class ModelDrag {
    public static final int MAX_OFFSET = 120;

    private float offset;

    public float offset() {
        return offset;
    }

    public void drag(double dx) {
        offset = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, offset + (float) dx));
    }

    public void reset() {
        offset = 0;
    }

    /** The x the model "looks at": dragging right turns it right, so the point moves left of centre. */
    public float lookX(float centreX) {
        return centreX - offset;
    }
}
