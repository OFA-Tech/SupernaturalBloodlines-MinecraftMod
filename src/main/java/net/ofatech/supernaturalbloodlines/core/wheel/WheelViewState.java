package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Who needs the server's catalogue of powers right now: the wheel while it is open, the hub while it is open. The server is told only when the first one needs it and when
 * the last one stops, so closing one of them never cuts off the other, and a screen that is set up again (a resize) sends nothing twice. Pure.
 */
public final class WheelViewState {
    public enum Viewer { WHEEL, HUB }

    private final Set<Viewer> open = EnumSet.noneOf(Viewer.class);

    /** What to tell the server after {@code viewer} opened or closed: true (start), false (stop), or nothing when nothing changed for the server. */
    public Optional<Boolean> set(Viewer viewer, boolean isOpen) {
        boolean before = !open.isEmpty();
        if (isOpen) {
            open.add(viewer);
        } else {
            open.remove(viewer);
        }
        boolean after = !open.isEmpty();
        return before == after ? Optional.empty() : Optional.of(after);
    }

    public boolean any() {
        return !open.isEmpty();
    }

    /** A new world or a lost connection: nobody is watching. */
    public void clear() {
        open.clear();
    }
}
