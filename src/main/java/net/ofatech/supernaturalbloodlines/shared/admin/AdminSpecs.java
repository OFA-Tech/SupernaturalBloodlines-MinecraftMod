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
    /** The COMMON specs: the only files an operator's edit may reach. */
    private static final Map<String, ModConfigSpec> SPECS = new ConcurrentHashMap<>();
    /** CLIENT specs, read for the player's own Settings page; the admin path never writes them (they are the host's own file, not the server's). */
    private static final Map<String, ModConfigSpec> CLIENT_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, Predicate<String>> LINE_CHECKS = new ConcurrentHashMap<>();

    private static final Map<String, List<String>> CHOICES = new ConcurrentHashMap<>();

    private AdminSpecs() {}

    public static void register(String file, ModConfigSpec spec) {
        SPECS.put(file, spec);
    }

    /** A CLIENT config: its values can be read here (the Settings page), but {@link #spec} and so the admin write path do not see it. */
    public static void registerClient(String file, ModConfigSpec spec) {
        CLIENT_SPECS.put(file, spec);
    }

    /** Whether {@code file} is a CLIENT config the admin path must refuse. */
    public static boolean isClient(String file) {
        return CLIENT_SPECS.containsKey(file) && !SPECS.containsKey(file);
    }

    private static ModConfigSpec any(String file) {
        ModConfigSpec common = SPECS.get(file);
        return common != null ? common : CLIENT_SPECS.get(file);
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
        ModConfigSpec spec = any(entry.file());
        if (spec == null || !spec.isLoaded()) {
            return Optional.empty();
        }
        return spec.getValues().get(entry.path()) instanceof ConfigValue<?> v ? Optional.of(v) : Optional.empty();
    }

    /** The value spec of an entry whether or not the config is loaded (the range, default and choices). */
    public static Optional<ModConfigSpec.ValueSpec> valueSpec(SettingEntry entry) {
        ModConfigSpec spec = any(entry.file());
        if (spec == null) {
            return Optional.empty();
        }
        return spec.getValues().get(entry.path()) instanceof ConfigValue<?> v ? Optional.of(v.getSpec()) : Optional.empty();
    }

    /** For tests. */
    public static void forget(String file) {
        SPECS.remove(file);
        CLIENT_SPECS.remove(file);
    }
}
