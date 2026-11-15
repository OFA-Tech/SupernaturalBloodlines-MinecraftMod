package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.gametest.PrimevalGameTests;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.gametest.TensuraSkillGameTests;

public final class SupernaturalTensuraPart {
    private SupernaturalTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        TensuraEpReward.register();
        ApexRaceReader.register();
        PrimevalRoarGrant.register();
        TensuraSkillEffects.register();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubClient.register();
        }
        modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            e.register(TensuraSkillGameTests.class);
            if (net.neoforged.fml.ModList.get().isLoaded("werewolves")) {
                e.register(PrimevalGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("SupernaturalTensuraPart enabled");
    }
}
