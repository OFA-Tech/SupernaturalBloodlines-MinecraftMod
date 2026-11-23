package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;

/**
 * The probe of a Unique that has no road of its own in its addon (it is only a reincarnation roll there): the one thing the addon says is its own
 * on/off switch for the skill, a public static boolean config value. Id: {@code unique.road:<class>#<FIELD>}.
 */
public final class UniqueRoadProbe implements AddonProbe {
    public static final String PREFIX = "unique.road:";

    private final String className;
    private final String field;

    public UniqueRoadProbe(String className, String field) {
        this.className = className;
        this.field = field;
    }

    /** Parses {@code unique.road:<class>#<FIELD>}. */
    public static Optional<UniqueRoadProbe> parse(String id) {
        if (!id.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String rest = id.substring(PREFIX.length());
        int hash = rest.indexOf('#');
        if (hash <= 0 || hash == rest.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(new UniqueRoadProbe(rest.substring(0, hash), rest.substring(hash + 1)));
    }

    @Override
    public Optional<String> missingHook() {
        Hooks.FieldHit hit = Hooks.staticField(className, field);
        if (hit.missing().isPresent()) {
            return hit.missing();
        }
        return hit.value().filter(v -> v instanceof Supplier<?>).isPresent() ? Optional.empty() : Optional.of(className + "#" + field + " (not a config value)");
    }

    @Override
    public boolean roadOpen() {
        Hooks.FieldHit hit = Hooks.staticField(className, field);
        if (hit.value().orElse(null) instanceof Supplier<?> value) {
            try {
                return !Boolean.FALSE.equals(value.get());
            } catch (IllegalStateException notLoaded) {
                return defaultOf(hit.value().get());
            }
        }
        return false;
    }

    /** A config that is not loaded yet answers with its default (what a ModConfigSpec value does). */
    private static boolean defaultOf(Object value) {
        try {
            Object d = value.getClass().getMethod("getDefault").invoke(value);
            return !Boolean.FALSE.equals(d);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    @Override
    public boolean satisfied(ServerPlayer target, EvolutionRecipe recipe) {
        return true;
    }
}
