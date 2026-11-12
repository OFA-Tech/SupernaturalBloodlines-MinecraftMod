package net.ofatech.supernaturalbloodlines;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SupernaturalBloodlines.MODID)
public final class SupernaturalBloodlines {
    public static final String MODID = "supernaturalbloodlines";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SupernaturalBloodlines(IEventBus modBus) {
        LOGGER.info("Supernatural Bloodlines loading");
    }
}
