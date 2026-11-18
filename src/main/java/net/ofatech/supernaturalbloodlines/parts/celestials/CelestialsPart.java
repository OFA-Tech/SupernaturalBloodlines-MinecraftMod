package net.ofatech.supernaturalbloodlines.parts.celestials;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.EclipseSignal;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsVampireGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsWolfGameTests;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Optional Enhanced Celestials 2 compatibility. No required mods: without EC (or with this part off) the lunar source stays empty and every
 * factor is 1, so the game plays as before. An EC event is only a second layer on top of the real moon (see {@code core/moon/LunarMood}).
 * The part never touches an EC class except through {@link EcReflect}.
 */
public final class CelestialsPart {
    public static final String EC_MOD = "enhancedcelestials2core";
    /** Our lunar eclipse as an EC event (a datapack file of ours that EC reads when it is installed). */
    public static final ResourceLocation ECLIPSE_EVENT = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "lunar_eclipse");

    private static volatile EcAdapter ec;

    private CelestialsPart() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CelestialsConfig.SPEC, CelestialsConfig.FILE);
        AdminSpecs.register(CelestialsConfig.FILE, CelestialsConfig.SPEC);
        install(find());
        EclipseSignal.add((server, on) -> {
            EcAdapter adapter = ec;
            return adapter != null && (on ? adapter.start(server.overworld(), ECLIPSE_EVENT) : adapter.stop(server.overworld(), ECLIPSE_EVENT));
        });
        if (ModList.get().isLoaded("vampirism")) {
            BloodMoonBite.register();
        }
        if (ModList.get().isLoaded("werewolves")) {
            SuperBlueMoon.register();
        }
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(CelestialsGameTests.class);
            if (ModList.get().isLoaded("vampirism")) {
                e.register(CelestialsVampireGameTests.class);
                if (ModList.get().isLoaded("werewolves")) {
                    e.register(CelestialsWolfGameTests.class);
                }
                if (EnabledParts.isEnabled("species")) {
                    e.register(CelestialsRiteGameTests.class);
                }
            }
        });
        SupernaturalBloodlines.LOGGER.info("CelestialsPart enabled, Enhanced Celestials {}", ec == null ? "not used" : "linked");
    }

    /** Makes {@code adapter} (null for none) the source of the lunar mood. */
    @VisibleForTesting
    public static void install(@Nullable EcAdapter adapter) {
        ec = adapter;
        LunarSource source = adapter == null ? LunarSource.NONE : new EcSource(adapter);
        LunarMoods.use(source, CelestialsConfig::tuning);
    }

    @Nullable
    private static EcAdapter find() {
        if (!ModList.get().isLoaded(EC_MOD)) {
            return null;
        }
        try {
            EcAdapter adapter = EcReflect.create();
            adapter.onChanged((level, previous, next) ->
                    SupernaturalBloodlines.LOGGER.debug("Lunar event in {}: {} -> {}", level.dimension().location(), previous, next));
            return adapter;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            SupernaturalBloodlines.LOGGER.warn("Enhanced Celestials 2 is installed but its API is not what this build knows ({}); the lunar events are ignored", e.toString());
            return null;
        }
    }
}
