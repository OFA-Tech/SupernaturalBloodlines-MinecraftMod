package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The powers the wheel pressed and has not released yet. A release is sent for every one of them whenever the wheel could no longer send it by itself (the button went up
 * anywhere, the screen closed or was replaced, focus was lost, the player died), exactly once each: nothing is left running on the server. Pure.
 */
public final class HeldPowers {
    public record Held(PowerRef ref, int mode) {
    }

    private final Map<String, Held> held = new LinkedHashMap<>();

    public void press(PowerRef ref, int mode) {
        held.put(ref.key(), new Held(ref, mode));
    }

    public boolean any() {
        return !held.isEmpty();
    }

    public boolean holds(PowerRef ref) {
        return held.containsKey(ref.key());
    }

    /** Everything held, now forgotten: the caller sends one release for each. */
    public List<Held> releaseAll() {
        List<Held> out = new ArrayList<>(held.values());
        held.clear();
        return out;
    }

    /** The one power's release, or null when it was not held (a second button-up sends nothing). */
    public Held release(PowerRef ref) {
        return held.remove(ref.key());
    }
}
