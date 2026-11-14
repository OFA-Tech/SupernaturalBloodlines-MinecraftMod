package net.ofatech.supernaturalbloodlines.parts.supernatural;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.ClaimGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.DetectorGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.MigrationGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.LadderDataGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.RiteSignalGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.TensuraEpGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.SupernaturalStateGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.Detectors;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlice;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlices;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.LadderNet;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

public final class SupernaturalPart {
    private SupernaturalPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SupernaturalConfig.SPEC, SupernaturalConfig.FILE);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new LadderReloadListener()));
        Detectors.register();
        LadderNet.register(modBus);
        LadderQuery.set(new LadderQuery.Provider() {
            @Override
            public Optional<LayerSlice> layerSlice(Player player) {
                SupernaturalState s = SupernaturalAccess.get(player);
                SupernaturalRace race = LayerSlices.raceOf(BloodlineAccess.get(player).species());
                // a player who has not been migrated yet keeps the whole layer they hold: nothing is taken back
                return s.schema() == 0 || race == null ? Optional.empty() : LayerSlices.slice(race, s.rung(race));
            }

            @Override
            public int rung(Player player, SupernaturalRace race) {
                return SupernaturalAccess.get(player).rung(race);
            }
        });
        modBus.addListener((RegisterGameTestsEvent e) -> {
            SupernaturalAccess.trackFakePlayers(true); // this event only fires on the GameTest server, whose players are all FakePlayers
            e.register(SupernaturalStateGameTests.class);
            e.register(LadderDataGameTests.class);
            e.register(DetectorGameTests.class);
            e.register(ClaimGameTests.class);
            e.register(MigrationGameTests.class);
            if (ModList.get().isLoaded("tensura") && EnabledParts.isEnabled("supernaturalTensura")) {
                e.register(TensuraEpGameTests.class);
            }
            if (ModList.get().isLoaded("enchanted")) {
                e.register(RiteSignalGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("SupernaturalPart enabled");
    }
}
