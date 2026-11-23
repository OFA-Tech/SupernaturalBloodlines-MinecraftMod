package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.Locale;
import java.util.Optional;

/**
 * How the skill is learned: {@code DEFAULT} is Tensura's own call with its own price and vetoes. {@code FREE} is the way Great Sage and FoxAblaze grant their
 * own Raphael: an instance that carries {@code NoMagiculeCost} and {@code GiftedInstance}, so Tensura charges nothing. 19c adds PERMANENT.
 */
public enum GrantForm {
    DEFAULT, FREE;

    public static Optional<GrantForm> parse(String id) {
        for (GrantForm f : values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(id)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }
}
