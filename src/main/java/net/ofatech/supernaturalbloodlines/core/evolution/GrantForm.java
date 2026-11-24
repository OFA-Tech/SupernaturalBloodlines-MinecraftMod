package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * How the skill is learned: {@code DEFAULT} is Tensura's own call with its own price and vetoes. {@code FREE} is the way Great Sage and FoxAblaze grant their
 * own Raphael: an instance that carries {@code NoMagiculeCost} and {@code GiftedInstance}, so Tensura charges nothing. {@code PERMANENT} is the default learn
 * with the remove time written out as {@code -1}, as Ascension's altar learns its Ultimates (Tensura 2.0.1.5's two-argument learn already passes {@code -1};
 * the form states the intent in the data and survives a change of that default).
 */
public enum GrantForm {
    DEFAULT, FREE, PERMANENT;

    public static Optional<GrantForm> parse(String id) {
        for (GrantForm f : values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(id)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /** The remove time to learn with, in ticks ({@code -1} is permanent); empty for the forms that use Tensura's two-argument learn. */
    public OptionalInt removeTime() {
        return this == PERMANENT ? OptionalInt.of(-1) : OptionalInt.empty();
    }
}
