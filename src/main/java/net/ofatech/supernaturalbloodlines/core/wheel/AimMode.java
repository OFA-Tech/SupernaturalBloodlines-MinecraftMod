package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Optional;

/**
 * Aim mode: the wheel is closed, the power is chosen and waits for a click. A click confirms (one use, aimed at the crosshair's target), a right click, Esc or the G tap cancel.
 * The use itself is sent by the {@code Sender} the caller gives, so this is testable. A screen opening cancels it (the player went somewhere else).
 */
public final class AimMode {
    public interface Sender {
        void use(PowerRef ref, int mode, int targetEntityId);
    }

    public record Aim(PowerRef ref, int mode) {
    }

    private final Sender sender;
    private Aim current;

    public AimMode(Sender sender) {
        this.sender = sender;
    }

    /** Why an aim that is waiting must end by itself. */
    public enum Cancel { NONE, GUI_HIDDEN, TIMED_OUT }

    /** An aim waits for a click for this long (20 s) at most: a power left aimed behind a forgotten wheel would fire at the next click. */
    public static final long TIMEOUT_TICKS = 400;

    private long startedAt;

    public void start(PowerRef ref, int mode) {
        start(ref, mode, 0);
    }

    public void start(PowerRef ref, int mode, long nowTick) {
        current = new Aim(ref, mode);
        startedAt = nowTick;
    }

    /** Pure rule: the HUD is hidden (F1: the player cannot see what is aimed) or the aim has waited too long. */
    public static Cancel cancelReason(boolean hideGui, long startedAtTick, long nowTick) {
        if (hideGui) {
            return Cancel.GUI_HIDDEN;
        }
        return nowTick - startedAtTick >= TIMEOUT_TICKS ? Cancel.TIMED_OUT : Cancel.NONE;
    }

    /** What to do now, or {@link Cancel#NONE}; the caller cancels and says so on the action bar. */
    public Cancel check(boolean hideGui, long nowTick) {
        return current == null ? Cancel.NONE : cancelReason(hideGui, startedAt, nowTick);
    }

    public boolean active() {
        return current != null;
    }

    public Optional<Aim> aim() {
        return Optional.ofNullable(current);
    }

    /** Sends one use at the target ({@code -1} for none) and ends the aim; nothing when not aiming. */
    public boolean confirm(int targetEntityId) {
        Aim a = current;
        current = null;
        if (a == null) {
            return false;
        }
        sender.use(a.ref(), a.mode(), targetEntityId);
        return true;
    }

    /** Ends the aim without sending; true when there was one. */
    public boolean cancel() {
        boolean was = current != null;
        current = null;
        return was;
    }
}
