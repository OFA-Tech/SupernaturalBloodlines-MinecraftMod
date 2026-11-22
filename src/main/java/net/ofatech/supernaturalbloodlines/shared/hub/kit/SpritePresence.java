package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Whether a sprite can be drawn, asked once and remembered until the resource packs reload. A pack may replace or remove a sprite, so a missing one is
 * an expected case: the caller draws flat theme colours instead and the screen stays usable. Pure; the probe is the client's sprite atlas.
 */
public final class SpritePresence {
    private final Predicate<String> probe;
    private final Map<String, Boolean> known = new HashMap<>();

    public SpritePresence(Predicate<String> probe) {
        this.probe = probe;
    }

    public boolean has(String sprite) {
        return known.computeIfAbsent(sprite, this::ask);
    }

    /** A probe that cannot take the name (an invalid resource path) says the sprite is not there, it never throws into a frame. */
    private boolean ask(String sprite) {
        try {
            return probe.test(sprite);
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    /** Forget every answer: the resource packs changed. */
    public void clear() {
        known.clear();
    }
}
