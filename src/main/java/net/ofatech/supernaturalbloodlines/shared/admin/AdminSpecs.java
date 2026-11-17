package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * The COMMON specs of the parts that are on. Each part registers its own next to its {@code registerConfig}, so a disabled part adds
 * nothing and its spec class is never loaded. Also the per-key line checks of the list settings (the parser the config itself uses).
 */
public final class AdminSpecs {
    private static final Map<String, ModConfigSpec> SPECS = new ConcurrentHashMap<>();
    private static final Map<String, Predicate<String>> LINE_CHECKS = new ConcurrentHashMap<>();

    private static final Map<String, List<String>> CHOICES = new ConcurrentHashMap<>();

    private AdminSpecs() {}

    public static void register(String file, ModConfigSpec spec) {
        SPECS.put(file, spec);
    }

    /** Registers the parser that says whether one line of the list {@code id} is usable; {@code id} is {@code file:path}. */
    public static void lineCheck(String id, Predicate<String> check) {
        LINE_CHECKS.put(id, check);
    }

    /** The line check of the list {@code id}, or null when it has none. */
    public static Predicate<String> lineCheck(String id) {
        return LINE_CHECKS.get(id);
    }

    /** The values a CHOICE key accepts (a spec cannot list them): registered next to its spec by the part that owns the key. */
    public static void choices(String id, List<String> values) {
        CHOICES.put(id, List.copyOf(values));
    }

    public static List<String> choices(String id) {
        return CHOICES.getOrDefault(id, List.of());
    }

    public static Optional<ModConfigSpec> spec(String file) {
        return Optional.ofNullable(SPECS.get(file));
    }

    /** The live value of an entry; empty when its part is off or its config has not loaded. */
    public static Optional<ConfigValue<?>> value(SettingEntry entry) {
        ModConfigSpec spec = SPECS.get(entry.file());
        if (spec == null || !spec.isLoaded()) {
            return Optional.empty();
        }
        return spec.getValues().get(entry.path()) instanceof ConfigValue<?> v ? Optional.of(v) : Optional.empty();
    }

    /** The value spec of an entry whether or not the config is loaded (the range, default and choices). */
    public static Optional<ModConfigSpec.ValueSpec> valueSpec(SettingEntry entry) {
        ModConfigSpec spec = SPECS.get(entry.file());
        if (spec == null) {
            return Optional.empty();
        }
        return spec.getValues().get(entry.path()) instanceof ConfigValue<?> v ? Optional.of(v.getSpec()) : Optional.empty();
    }

    /** For tests. */
    public static void forget(String file) {
        SPECS.remove(file);
    }
}
