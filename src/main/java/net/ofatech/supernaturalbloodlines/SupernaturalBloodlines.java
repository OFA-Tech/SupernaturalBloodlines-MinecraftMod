package net.ofatech.supernaturalbloodlines;

import com.mojang.logging.LogUtils;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.ofatech.supernaturalbloodlines.core.Config;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.PartLoader;
import org.slf4j.Logger;

@Mod(SupernaturalBloodlines.MODID)
public final class SupernaturalBloodlines {
    public static final String MODID = "supernaturalbloodlines";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SupernaturalBloodlines(IEventBus modBus, ModContainer container) {
        LOGGER.info("Supernatural Bloodlines loading");
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON);

        ModList mods = ModList.get();
        // Config.COMMON is not loaded yet at construction, so read the file directly (see Config)
        Set<Part> parts = PartLoader.enabledParts(mods::isLoaded, Config::partEnabledEarly);
        for (Part p : Part.values()) {
            LOGGER.info("Part {}: {}", p, PartLoader.status(p, mods::isLoaded, Config::partEnabledEarly));
        }
        PartLoader.initAll(parts, modBus);
    }
}
