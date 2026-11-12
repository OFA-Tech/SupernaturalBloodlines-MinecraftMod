package net.ofatech.supernaturalbloodlines.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Part switches read straight from the toml. COMMON configs load after mod construction and after mixins are
 * applied, so spec values are not readable when parts must register. This class holds no ModConfigSpec, so a
 * mixin plugin can use it without loading {@link Config}. A missing file, key or bad value means enabled.
 */
public final class EarlySwitches {
    private static final Logger LOGGER = LoggerFactory.getLogger(EarlySwitches.class);

    private EarlySwitches() {}

    public static Set<Part> enabled() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(SupernaturalBloodlines.MODID + "-common.toml");
        Set<Part> on = EnumSet.noneOf(Part.class);
        for (Part p : Part.values()) {
            if (partEnabled(file, p)) {
                on.add(p);
            }
        }
        return on;
    }

    static boolean partEnabled(Path file, Part part) {
        if (!Files.isRegularFile(file)) {
            return true;
        }
        try {
            CommentedConfig cfg = TomlFormat.instance().createParser().parse(Files.readString(file));
            Object v = cfg.get("parts." + part.configKey());
            if (v == null) {
                return true;
            }
            if (v instanceof Boolean flag) {
                return flag;
            }
            LOGGER.warn("{}: parts.{} is not a boolean ({}), treating it as enabled", file, part.configKey(), v);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("{}: cannot be parsed ({}), treating all parts as enabled", file, e.toString());
        }
        return true;
    }
}
