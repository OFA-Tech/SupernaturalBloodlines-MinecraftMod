package net.ofatech.supernaturalbloodlines.core.evolution;

import java.util.Locale;
import java.util.Optional;

/** How the skill is learned: {@code DEFAULT} is Tensura's own call with its own price and vetoes. 19b adds FREE, 19c PERMANENT. */
public enum GrantForm {
    DEFAULT;

    public static Optional<GrantForm> parse(String id) {
        for (GrantForm f : values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(id)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }
}
