package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.List;
import java.util.Locale;

/** The pure reading of the development screenshot flags that is not the hub's own: today {@code boss:<look>:<bar>}. A bad flag is an {@link IllegalArgumentException} that says what is wanted. */
final class DevShotsSpec {
    static final List<String> LOOKS = List.of("grimoire", "blood_moon", "tensura");

    record Boss(String look, String bar) {
    }

    private DevShotsSpec() {
    }

    /** {@code parts}: the flag split at colons, for example {@code [boss, grimoire, klaus_shifting]}. */
    static Boss boss(String[] parts) {
        String wanted = "boss:<look>:<bar>, with <look> one of " + LOOKS + " and <bar> like klaus_shifting, beast_spirit, mikael or stack3";
        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("cannot read the flag '" + String.join(":", parts) + "': " + wanted);
        }
        String look = parts[1].toLowerCase(Locale.ROOT);
        if (!LOOKS.contains(look)) {
            throw new IllegalArgumentException("no look '" + parts[1] + "' in the flag '" + String.join(":", parts) + "': " + wanted);
        }
        return new Boss(look, parts[2].toLowerCase(Locale.ROOT));
    }
}
