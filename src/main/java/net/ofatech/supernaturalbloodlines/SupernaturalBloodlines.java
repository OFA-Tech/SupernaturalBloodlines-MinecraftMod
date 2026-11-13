package net.ofatech.supernaturalbloodlines;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.ofatech.supernaturalbloodlines.core.Config;
import net.ofatech.supernaturalbloodlines.core.EarlySwitches;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.PartEnabledCondition;
import net.ofatech.supernaturalbloodlines.core.PartLoader;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.network.ModNetwork;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexRaces;
import org.slf4j.Logger;

@Mod(SupernaturalBloodlines.MODID)
public final class SupernaturalBloodlines {
    public static final String MODID = "supernaturalbloodlines";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public SupernaturalBloodlines(IEventBus modBus, ModContainer container) {
        LOGGER.info("Supernatural Bloodlines loading");
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON);

        // always registered (not in a part): data of a disabled part must still decode its conditions
        CONDITIONS.register(modBus);
        CONDITIONS.register("part_enabled", () -> PartEnabledCondition.CODEC);
        ModAttachments.register(modBus);
        ModNetwork.register(modBus);
        StalePartModifiers.register();

        ModList mods = ModList.get();
        // Config.COMMON is not loaded yet at construction, so read the file directly (see EarlySwitches)
        Set<Part> switches = EarlySwitches.enabled();
        Set<Part> parts = PartLoader.enabledParts(mods::isLoaded, switches::contains);
        for (Part p : Part.values()) {
            LOGGER.info("Part {}: {}", p, PartLoader.status(p, mods::isLoaded, switches::contains));
        }
        EnabledParts.set(parts);
        PartLoader.initAll(parts, modBus, container);
        if (mods.isLoaded("tensura") && !parts.contains(Part.SPECIES_TENSURA)) {
            // ManasCore throws when a saved race is not registered, which stops that player from loading at all
            ApexRaces.register();
            LOGGER.info("Apex races registered without species_tensura (a saved player may still hold one)");
        }
    }
}
