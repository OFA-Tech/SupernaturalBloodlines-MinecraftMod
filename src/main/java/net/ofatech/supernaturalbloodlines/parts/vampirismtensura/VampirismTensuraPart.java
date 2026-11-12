package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest.VampirismTensuraGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest.WerewolvesGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves.WerewolfSkillGrants;

public final class VampirismTensuraPart {
    private VampirismTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VampireSyncConfig.SPEC, VampireSyncConfig.FILE);
        VampireSync.register();
        FactionRaceRules.register();
        HolyDamageBridge.register();
        boolean werewolves = ModList.get().isLoaded("werewolves");
        if (werewolves) {
            WerewolfSkillGrants.register();
        }
        // the part is only initialised when Vampirism and Tensura are present, so the tests only exist then;
        // the Werewolves tests link Werewolves classes, so they are registered only when it is loaded
        modBus.addListener((RegisterGameTestsEvent e) -> {
            e.register(VampirismTensuraGameTests.class);
            if (werewolves) {
                e.register(WerewolvesGameTests.class);
            }
        });
        SupernaturalBloodlines.LOGGER.info("VampirismTensuraPart enabled");
    }
}
