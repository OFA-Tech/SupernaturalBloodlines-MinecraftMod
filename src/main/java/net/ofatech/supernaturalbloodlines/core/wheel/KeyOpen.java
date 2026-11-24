package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Optional;

/**
 * Something a key press asked for that must wait for the key's release: opening one of Infinity's screens from Enter. His destination screen confirms on Enter, and a key
 * held down repeats, so a screen opened on the press would be confirmed by the press that opened it. The press is kept, its repeats change nothing, and the release hands
 * it back once. Pure.
 */
public final class KeyOpen<T> {
    private int key = Integer.MIN_VALUE;
    private T what;

    /** A press of {@code key}. Ignored while a press is already waiting (a repeat, or a second key). */
    public boolean press(int pressed, T wanted) {
        if (what != null) {
            return false;
        }
        key = pressed;
        what = wanted;
        return true;
    }

    /** Whether this key is the one being waited on (its repeats are swallowed). */
    public boolean waiting(int pressed) {
        return what != null && key == pressed;
    }

    /** The release of {@code released}: what was asked, once, when it is the key waited on. */
    public Optional<T> release(int released) {
        if (what == null || key != released) {
            return Optional.empty();
        }
        T out = what;
        what = null;
        return Optional.of(out);
    }

    /** The screen went away or lost focus: no release will come. */
    public void clear() {
        what = null;
    }
}
