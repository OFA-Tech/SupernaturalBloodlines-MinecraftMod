package net.ofatech.supernaturalbloodlines.core;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** The parts the mod constructor decided to enable (mods present and switch on). Read-only for everyone else. */
public final class EnabledParts {
    private static volatile Set<Part> enabled = EnumSet.noneOf(Part.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private EnabledParts() {}

    /** Called once from the mod constructor. */
    public static void set(Collection<Part> parts) {
        Set<Part> copy = EnumSet.noneOf(Part.class);
        copy.addAll(parts);
        enabled = copy;
    }

    public static Set<Part> snapshot() {
        return enabled;
    }

    /** True when the part with this config key is enabled. An unknown key is false and warns once. */
    public static boolean isEnabled(String configKey) {
        for (Part p : Part.values()) {
            if (p.configKey().equals(configKey)) {
                return enabled.contains(p);
            }
        }
        if (WARNED.add(configKey)) {
            SupernaturalBloodlines.LOGGER.warn("part_enabled condition names unknown part '{}', treating it as disabled", configKey);
        }
        return false;
    }
}
