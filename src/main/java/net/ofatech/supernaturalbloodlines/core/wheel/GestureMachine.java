package net.ofatech.supernaturalbloodlines.core.wheel;

/**
 * The G key of the wheel: held past {@code tapMs} it opens and stays open while held; letting go over a power uses it, letting go over nothing leaves the wheel open
 * until a click, G or Esc; a tap shorter than {@code tapMs} opens nothing and repeats the last power. Immutable; the caller feeds the inputs and does what the effect says.
 * {@link Input#TICK} is the poll of a held key (nothing else says time has passed).
 */
public final class GestureMachine {
    public enum Input { G_DOWN, G_UP, CLICK_LEFT, CLICK_RIGHT, ESC, SCREEN_OPENED, FOCUS_LOST, TICK }

    public enum Effect { NONE, OPEN, CLOSE, USE_HOVERED, REPEAT_LAST }

    private enum State { IDLE, PENDING, HELD, STICKY }

    public record Step(GestureMachine machine, Effect effect) {
    }

    public static final GestureMachine IDLE = new GestureMachine(State.IDLE, 0);
    /** An open wheel nothing is held for (the shape of one left open after a release over nothing). */
    public static final GestureMachine OPEN_STICKY = new GestureMachine(State.STICKY, 0);

    private final State state;
    private final long downAt;

    private GestureMachine(State state, long downAt) {
        this.state = state;
        this.downAt = downAt;
    }

    /** The wheel is open (held or left open). */
    public boolean open() {
        return state == State.HELD || state == State.STICKY;
    }

    /** G is down and the wheel is not open yet. */
    public boolean pending() {
        return state == State.PENDING;
    }

    /** The same wheel, left open after a use that does not close it (a toggle stays open). */
    public GestureMachine sticky() {
        return open() ? new GestureMachine(State.STICKY, downAt) : this;
    }

    public Step step(Input in, long nowMs, int tapMs) {
        return step(in, nowMs, tapMs, false);
    }

    /** @param overPower the pointer is on a power (for {@code G_UP} and the clicks) */
    public Step step(Input in, long nowMs, int tapMs, boolean overPower) {
        switch (in) {
            case SCREEN_OPENED:
                return new Step(IDLE, Effect.NONE);
            case FOCUS_LOST:
                return new Step(IDLE, open() ? Effect.CLOSE : Effect.NONE);
            case ESC:
                return new Step(IDLE, open() ? Effect.CLOSE : Effect.NONE);
            default:
                break;
        }
        switch (state) {
            case IDLE:
                return in == Input.G_DOWN ? new Step(new GestureMachine(State.PENDING, nowMs), Effect.NONE) : new Step(this, Effect.NONE);
            case PENDING:
                if (in == Input.TICK && nowMs - downAt >= tapMs) {
                    return new Step(new GestureMachine(State.HELD, downAt), Effect.OPEN);
                }
                if (in == Input.G_UP) {
                    return nowMs - downAt >= tapMs ? new Step(new GestureMachine(State.STICKY, downAt), Effect.OPEN) : new Step(IDLE, Effect.REPEAT_LAST);
                }
                return new Step(this, Effect.NONE);
            case HELD:
                if (in == Input.G_UP) {
                    return overPower ? new Step(IDLE, Effect.USE_HOVERED) : new Step(new GestureMachine(State.STICKY, downAt), Effect.NONE);
                }
                if (in == Input.CLICK_LEFT && overPower) {
                    return new Step(this, Effect.USE_HOVERED);
                }
                return new Step(this, Effect.NONE);
            case STICKY:
                if (in == Input.G_DOWN) {
                    return new Step(IDLE, Effect.CLOSE);
                }
                if (in == Input.CLICK_LEFT) {
                    return overPower ? new Step(this, Effect.USE_HOVERED) : new Step(IDLE, Effect.CLOSE);
                }
                return new Step(this, Effect.NONE);
            default:
                return new Step(this, Effect.NONE);
        }
    }
}
