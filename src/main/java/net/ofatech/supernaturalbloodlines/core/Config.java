package net.ofatech.supernaturalbloodlines.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final ModConfigSpec COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Switch a part off to keep it disabled even when its mods are installed. Restart required.")
                .push("parts");
        for (Part p : Part.values()) {
            b.define(p.configKey(), true);
        }
        b.pop();
        VampireSyncConfig.define(b);
        COMMON = b.build();
    }

    private Config() {}

    /**
     * COMMON configs load after mod construction and after registry events, so spec values are not readable
     * when parts must register. Read the toml directly; a missing file, key or bad value means enabled.
     */
    public static Set<Part> enabledSwitchesEarly() {
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
