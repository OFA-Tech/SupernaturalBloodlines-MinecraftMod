package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** A power as the wire names it: its source and id. */
public record PowerRef(PowerSource source, ResourceLocation id) {
    public static final int MAX_KEY = 256;

    public PowerRef {
        java.util.Objects.requireNonNull(source);
        java.util.Objects.requireNonNull(id);
    }

    /** e.g. {@code tensura/tensura:coercion} */
    public String key() {
        return source.name().toLowerCase(java.util.Locale.ROOT) + "/" + id;
    }

    public static Optional<PowerRef> parse(String key) {
        if (key == null || key.length() > MAX_KEY) {
            return Optional.empty();
        }
        int slash = key.indexOf('/');
        if (slash <= 0 || slash == key.length() - 1) {
            return Optional.empty();
        }
        PowerSource source = null;
        String name = key.substring(0, slash);
        for (PowerSource s : PowerSource.values()) {
            if (s.name().toLowerCase(java.util.Locale.ROOT).equals(name)) {
                source = s;
            }
        }
        ResourceLocation id = source == null ? null : ResourceLocation.tryParse(key.substring(slash + 1));
        return id == null ? Optional.empty() : Optional.of(new PowerRef(source, id));
    }
}
