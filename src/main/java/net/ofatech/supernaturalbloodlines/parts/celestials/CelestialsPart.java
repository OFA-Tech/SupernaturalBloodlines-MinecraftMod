package net.ofatech.supernaturalbloodlines.parts.celestials;

import java.util.concurrent.Callable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.EclipseSignal;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsEclipseNoticeGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsRealEcGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.celestials.gametest.CelestialsVampireGameTests;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
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
    public static final ResourceLocation ECLIPSE_EVENT = ResourceLocation.fromNamespaceAndPath(LunarMood.ECLIPSE_NAMESPACE, LunarMood.ECLIPSE_PATH);

    private static volatile EcAdapter ec;
    private static volatile boolean linkedAtInit;

    private CelestialsPart() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CelestialsConfig.SPEC, CelestialsConfig.FILE);
        AdminSpecs.register(CelestialsConfig.FILE, CelestialsConfig.SPEC);
        install(ModList.get().isLoaded(EC_MOD) ? tryCreate(EcReflect::create) : null);
        linkedAtInit = ec != null;
        EclipseSignal.add((server, on) -> {
            EcAdapter adapter = ec;
            if (adapter != null && LunarMoods.active()) {
                // a permanently broken Enhanced Celestials stops being asked after 3 failures in a row
                LunarMoods.guarded(LunarMoods.Call.MIRROR, () -> on ? adapter.start(server.overworld(), ECLIPSE_EVENT) : adapter.stop(server.overworld(), ECLIPSE_EVENT), false);
                LunarMoods.refresh();
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            EcAdapter adapter = ec;
            if (adapter != null) {
                boolean known = LunarMoods.guarded(LunarMoods.Call.MIRROR, () -> adapter.knows(e.getServer().overworld(), ECLIPSE_EVENT), false);
                SupernaturalBloodlines.LOGGER.info("Enhanced Celestials knows our lunar event {}: {}", ECLIPSE_EVENT, known);
            }
        });
        if (ModList.get().isLoaded("vampirism")) {
            BloodMoonBite.register();
        }
        if (ModList.get().isLoaded("werewolves")) {
            SuperBlueMoon.register();
        }
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(CelestialsGameTests.class);
            if (ModList.get().isLoaded(EC_MOD)) {
                e.register(CelestialsRealEcGameTests.class);
            }
            if (EnabledParts.isEnabled("species")) {
                e.register(CelestialsEclipseNoticeGameTests.class);
            }
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

    /**
     * Makes {@code adapter} (null for none) the source of the lunar mood.
     */
    @VisibleForTesting
    public static void install(@Nullable EcAdapter adapter) {
        ec = adapter;
        LunarSource source = adapter == null ? LunarSource.NONE : new EcSource(adapter);
        LunarMoods.use(source, CelestialsConfig::tuning);
    }

    /** True when the real start-up linked Enhanced Celestials (never true in the test runtime, which has none). */
    @VisibleForTesting
    public static boolean linkedAtInit() {
        return linkedAtInit;
    }

    /** A fresh adapter on the real Enhanced Celestials classes (for the game tests that run with EC jars). */
    @VisibleForTesting
    public static EcAdapter createReal() throws ReflectiveOperationException {
        return EcReflect.create();
    }

    /** The adapter in use; null when there is no Enhanced Celestials (or its API is not the one this build knows). */
    @VisibleForTesting
    public static @Nullable EcAdapter adapter() {
        return ec;
    }

    /** The adapter {@code create} builds, or null (with one warning) when it cannot: no half-working adapter is ever used. */
    @VisibleForTesting
    @Nullable
    static EcAdapter tryCreate(Callable<? extends EcAdapter> create) {
        try {
            return create.call();
        } catch (Exception | LinkageError e) {
            SupernaturalBloodlines.LOGGER.warn("Enhanced Celestials 2 is installed but its API is not what this build knows ({}); the lunar events are ignored", e.toString());
            return null;
        }
    }
}
