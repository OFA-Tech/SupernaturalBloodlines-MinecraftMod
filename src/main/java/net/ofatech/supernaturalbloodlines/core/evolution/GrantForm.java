package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * How the skill is learned: {@code DEFAULT} is Tensura's own call with its own price and vetoes; {@code PERMANENT} is the same learn with the remove time written
 * out as {@code -1}, as Ascension's altar learns its Ultimates. (Tensura 2.0.1.5's two-argument learn already passes {@code -1}; the form states the intent in the
 * data and survives a change of that default.) 19b adds FREE.
 */
public enum GrantForm {
    DEFAULT,
    PERMANENT;

    public static Optional<GrantForm> parse(String id) {
        for (GrantForm f : values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(id)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /** The remove time to learn with, in ticks ({@code -1} is permanent); empty for the form that uses Tensura's two-argument learn. */
    public OptionalInt removeTime() {
        return this == PERMANENT ? OptionalInt.of(-1) : OptionalInt.empty();
    }
}
