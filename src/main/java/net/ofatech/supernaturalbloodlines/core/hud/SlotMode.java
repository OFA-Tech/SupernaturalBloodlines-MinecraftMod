package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Where the six slots come from. Which modes exist depends on the installed mods; with neither there is no HUD. */
public enum SlotMode {
    TENSURA,
    SUPERNATURAL;

    public static List<SlotMode> available(boolean tensuraLoaded, boolean vampirismLoaded) {
        List<SlotMode> modes = new ArrayList<>(2);
        if (tensuraLoaded) {
            modes.add(TENSURA);
        }
        if (vampirismLoaded) {
            modes.add(SUPERNATURAL);
        }
        return List.copyOf(modes);
    }

    /** The mode after a toggle; a mode that is not available falls back to the first one that is. Empty when there is none. */
    public static Optional<SlotMode> next(SlotMode current, List<SlotMode> available) {
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int at = available.indexOf(current);
        return Optional.of(available.get(at < 0 ? 0 : (at + 1) % available.size()));
    }

    /** The mode to start in from the client setting ({@code AUTO} is Tensura when it is installed). */
    public static Optional<SlotMode> start(String setting, List<SlotMode> available) {
        if (available.isEmpty()) {
            return Optional.empty();
        }
        SlotMode wanted = "SUPERNATURAL".equalsIgnoreCase(setting) ? SUPERNATURAL : TENSURA;
        return Optional.of(available.contains(wanted) ? wanted : available.get(0));
    }
}
