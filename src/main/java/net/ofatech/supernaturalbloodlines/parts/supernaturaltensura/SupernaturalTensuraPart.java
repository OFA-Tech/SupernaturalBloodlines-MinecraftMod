package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.gametest.TensuraSkillGameTests;

public final class SupernaturalTensuraPart {
    private SupernaturalTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        TensuraEpReward.register();
        ApexRaceReader.register();
        TensuraSkillEffects.register();
        modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> e.register(TensuraSkillGameTests.class));
        SupernaturalBloodlines.LOGGER.info("SupernaturalTensuraPart enabled");
    }
}
