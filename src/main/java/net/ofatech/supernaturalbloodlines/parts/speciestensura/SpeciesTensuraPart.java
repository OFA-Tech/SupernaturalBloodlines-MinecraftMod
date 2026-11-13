package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexOffer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexRaces;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest.ApexFactionGameTests;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest.ApexGameTests;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest.SpeciesLayerGameTests;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest.SpeciesTensuraWerewolvesGameTests;
import net.ofatech.supernaturalbloodlines.shared.tensura.LayerQuery;

public final class SpeciesTensuraPart {
    private SpeciesTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SpeciesTensuraConfig.SPEC, SpeciesTensuraConfig.FILE);
        ApexRaces.register();
        if (!ApexOffer.hookApplied()) {
            SupernaturalBloodlines.LOGGER.warn("species_tensura: the ManasRaceInstance.getNextEvolutions hook is not in place, "
                    + "no apex evolution will be offered (species keep working)");
        }
        SpeciesEligibility.setCheck(TensuraHumanLikeCheck.INSTANCE);
        TensuraSupernatural.register();
        LayerQuery.install(SpeciesLayer::treatedAsUndeadForBridge, SpeciesLayer::isApexVampireType);
        SpeciesLayer.register();
        // the part is only initialised when its mods are present, so the tests only exist then
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(SpeciesLayerGameTests.class);
            e.register(ApexGameTests.class);
            if (EnabledParts.snapshot().contains(Part.VAMPIRISM_TENSURA)) {
                e.register(ApexFactionGameTests.class);
            }
            // these link Werewolves classes, and the werewolf level grants live in vampirism_tensura
            if (ModList.get().isLoaded("werewolves") && EnabledParts.snapshot().contains(Part.VAMPIRISM_TENSURA)) {
                e.register(SpeciesTensuraWerewolvesGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("SpeciesTensuraPart enabled");
    }
}
