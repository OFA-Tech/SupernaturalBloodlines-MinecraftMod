package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSpecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest.VampirismTensuraGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest.WerewolvesGameTests;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves.WerewolfRaceSync;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves.WerewolfSkillGrants;

public final class VampirismTensuraPart {
    private VampirismTensuraPart() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VampireSyncConfig.SPEC, VampireSyncConfig.FILE);
        AdminSpecs.register(VampireSyncConfig.FILE, VampireSyncConfig.SPEC);
        AdminSpecs.lineCheck(VampireSyncConfig.FILE + ":race_level_floor", line -> VampireMapping.parse(VampireMapping.DEFAULT_CURE_RACE, List.of(line), 20).rejected().isEmpty());
        AdminSpecs.lineCheck(VampireSyncConfig.FILE + ":werewolf_skill_grants", line -> SkillGrantSpec.parse(List.of(line)).rejected().isEmpty());
        // Architectury calls SET_RACE listeners in registration order: a hunter whose new race is undead has to leave
        // the hunters before VampireSync tries to make them a vampire (it refuses while they are still a hunter)
        FactionRaceRules.register();
        net.ofatech.supernaturalbloodlines.shared.vampirism.BloodlessPlayers.set(FactionRaceRules::isBloodless);
        VampireSync.register();
        HolyDamageBridge.register();
        boolean werewolves = ModList.get().isLoaded("werewolves");
        if (werewolves) {
            WerewolfSkillGrants.register();
            // after VampireSync (registered above): a vampire who becomes a werewolf is cured to human first, and only then can this map human to beastfolk
            WerewolfRaceSync.register();
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
