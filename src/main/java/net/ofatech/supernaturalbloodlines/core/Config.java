package net.ofatech.supernaturalbloodlines.core;

import com.electronwill.nightconfig.core.file.FileConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class Config {
    public static final ModConfigSpec COMMON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Switch a part off to keep it disabled even when its mods are installed. Restart required.")
                .push("parts");
        for (Part p : Part.values()) {
            b.define(p.configKey(), true);
        }
        b.pop();
        COMMON = b.build();
    }

    private Config() {}

    /**
     * COMMON configs load after mod construction and after registry events, so spec values are not readable
     * when parts must register. Read the toml directly; a missing file, key or bad value means enabled.
     */
    public static boolean partEnabledEarly(Part part) {
        Path file = FMLPaths.CONFIGDIR.get().resolve(SupernaturalBloodlines.MODID + "-common.toml");
        if (!Files.isRegularFile(file)) {
            return true;
        }
        try (FileConfig cfg = FileConfig.of(file)) {
            cfg.load();
            Object v = cfg.get("parts." + part.configKey());
            return !(v instanceof Boolean flag) || flag;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
