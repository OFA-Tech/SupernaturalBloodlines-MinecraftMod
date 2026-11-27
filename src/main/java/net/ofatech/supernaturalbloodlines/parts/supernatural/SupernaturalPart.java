package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.ModSkillGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.PredatorsThirstGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.BloodSkillGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.BloodEffects;
import java.util.List;
import java.util.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.HubKeybinds;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubClientConfig;
import java.util.Set;
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
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.SkillGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.TensuraEpGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.SupernaturalStateGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.Detectors;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillEffects;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlice;
import net.ofatech.supernaturalbloodlines.core.supernatural.LayerSlices;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.LadderNet;
import net.ofatech.supernaturalbloodlines.shared.supernatural.GrantedSkills;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

public final class SupernaturalPart {
    private SupernaturalPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SupernaturalConfig.SPEC, SupernaturalConfig.FILE);
        AdminSpecs.register(SupernaturalConfig.FILE, SupernaturalConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new LadderReloadListener());
            e.addListener(new SkillReloadListener());
        });
        Detectors.register();
        LadderNet.register(modBus);
        net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakSync.register();
        net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.VampirismTweaks.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.TweakClient.register();
            container.registerConfig(ModConfig.Type.CLIENT, HubClientConfig.SPEC, HubClientConfig.FILE);
            AdminSpecs.registerClient(HubClientConfig.FILE, HubClientConfig.SPEC);
            HubKeybinds.register(modBus);
        }
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

            @Override
            public boolean apexRungReached(Player player, SupernaturalRace race) {
                SupernaturalState s = SupernaturalAccess.get(player);
                // not migrated yet: the migration gives the floor on login, and until then nothing is taken back
                return s.schema() == 0 || LadderRules.apexRungReached(LadderTable.current(), s, race);
            }

            @Override
            public boolean owns(Player player, String skillId) {
                SupernaturalState s = SupernaturalAccess.get(player);
                if (!s.skills().contains(skillId)) {
                    return false; // the common answer, without looking up the race
                }
                List<SupernaturalRace> races = TaskTracker.races(player);
                return SkillRules.owns(SkillTable.current(), s, races.get(0), races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty(), skillId,
                        id -> GrantedSkills.holds(player, id));
            }

            @Override
            public Set<String> tensuraSkills(Player player) {
                SupernaturalState s = SupernaturalAccess.get(player);
                List<SupernaturalRace> races = TaskTracker.races(player);
                return SkillEffects.tensuraSkillsOf(SkillTable.current(), SkillRules.active(SkillTable.current(), s, races.get(0),
                        races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty(), id -> GrantedSkills.holds(player, id)));
            }
        });
        SkillEffects.register();
        BloodEffects.register();
        modBus.addListener((RegisterGameTestsEvent e) -> {
            SupernaturalAccess.trackFakePlayers(true); // this event only fires on the GameTest server, whose players are all FakePlayers
            e.register(SupernaturalStateGameTests.class);
            e.register(LadderDataGameTests.class);
            e.register(DetectorGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.HubKindGameTests.class);
            e.register(ClaimGameTests.class);
            e.register(MigrationGameTests.class);
            e.register(SkillGameTests.class);
            e.register(BloodSkillGameTests.class);
            e.register(PredatorsThirstGameTests.class);
            e.register(ModSkillGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.HubGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.AdminGameTests.class);
            e.register(net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.TweaksGameTests.class);
            if (ModList.get().isLoaded("werewolves")) {
                e.register(net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.TweaksWerewolfGameTests.class);
            }
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
