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

    public void start(PowerRef ref, int mode) {
        current = new Aim(ref, mode);
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
