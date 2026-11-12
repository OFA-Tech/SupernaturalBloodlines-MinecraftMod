package net.ofatech.supernaturalbloodlines.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class PartLoader {
    private PartLoader() {}

    public static Set<Part> enabledParts(Predicate<String> modLoaded, Predicate<Part> configEnabled) {
        Set<Part> parts = EnumSet.noneOf(Part.class);
        for (Part p : Part.values()) {
            if (configEnabled.test(p) && p.requiredMods().stream().allMatch(modLoaded)) {
                parts.add(p);
            }
        }
        // drop parts whose required parts are off, until nothing else changes
        while (parts.removeIf(p -> !parts.containsAll(p.requiredParts()))) {
            // repeat
        }
        return parts;
    }

    public static String status(Part part, Predicate<String> modLoaded, Predicate<Part> configEnabled) {
        String missing = part.requiredMods().stream().filter(modLoaded.negate()).collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            return "disabled (missing: " + missing + ")";
        }
        if (!configEnabled.test(part)) {
            return "disabled (config)";
        }
        if (!enabledParts(modLoaded, configEnabled).contains(part)) {
            return "disabled (requires " + part.requiredParts() + ")";
        }
        return "enabled";
    }

    public static void initAll(Set<Part> parts, IEventBus bus, ModContainer container) {
        for (Part p : parts) {
            try {
                Class.forName(p.initClass()).getMethod("init", IEventBus.class, ModContainer.class).invoke(null, bus, container);
            } catch (ReflectiveOperationException | LinkageError e) {
                SupernaturalBloodlines.LOGGER.error("Part {} failed to initialise, skipping it", p, e);
            }
        }
    }
}
