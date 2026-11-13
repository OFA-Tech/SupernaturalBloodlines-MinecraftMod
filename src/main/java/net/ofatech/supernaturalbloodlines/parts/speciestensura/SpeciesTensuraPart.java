package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest.SpeciesLayerGameTests;
import net.ofatech.supernaturalbloodlines.shared.tensura.LayerQuery;

public final class SpeciesTensuraPart {
    private SpeciesTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SpeciesTensuraConfig.SPEC, SpeciesTensuraConfig.FILE);
        SpeciesEligibility.setCheck(TensuraHumanLikeCheck.INSTANCE);
        TensuraSupernatural.register();
        LayerQuery.install(SpeciesLayer::treatAsUndead, SpeciesLayer::isVampireTypeSpecies);
        SpeciesLayer.register();
        // the part is only initialised when its mods are present, so the tests only exist then
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(SpeciesLayerGameTests.class));
        SupernaturalBloodlines.LOGGER.info("SpeciesTensuraPart enabled");
    }
}
