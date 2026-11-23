package net.ofatech.supernaturalbloodlines.core.wheel;

/**
 * When a held power that the wheel started is let go: a held power started by the mouse stays on while the button is down, by Enter or Space while the key is down, and one
 * started by letting go of G (or by the tap that repeats the last power) has nothing to hold, so it is pressed and released at once. A power that is not held is never held. Pure.
 */
public final class HoldPlan {
    public enum Trigger { MOUSE_CLICK, KEY_PRESS, G_RELEASE, TAP_REPEAT }

    private HoldPlan() {
    }

    /** True when the release must be sent right after the press. */
    public static boolean releaseAtOnce(PowerKind kind, Trigger trigger) {
        return kind == PowerKind.HELD && (trigger == Trigger.G_RELEASE || trigger == Trigger.TAP_REPEAT);
    }

    /** True when the power stays down until the input that started it goes up. */
    public static boolean heldUntilInputUp(PowerKind kind, Trigger trigger) {
        return kind == PowerKind.HELD && !releaseAtOnce(kind, trigger);
    }
}
