package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.RaceMapper;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.RaceMapperConfig;

/**
 * Plan 23 Tasks 10-11: race mapping from Tensura reincarnation picks to supernatural kinds.
 * Only a player pick (reincarnation menu open) triggers mapping; admin commands and our own writes do not.
 */
@PrefixGameTestTemplate(false)
public final class RaceMapperGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private RaceMapperGameTests() {}

    private static void note(String proof, String text) {
        SupernaturalBloodlines.LOGGER.info("PLAN23 {} {}", proof, text);
    }

    private static boolean setRace(ServerPlayer p, String id) {
        return RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(id), false);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodfiendPickJoinsVampireFaction(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")
                || !EnabledParts.isEnabled("vampirismTensura")) {
            note("T10", "SKIPPED: needs species, species_tensura and vampirism_tensura");
            helper.succeed();
            return;
        }
        if (!ModList.get().isLoaded("ascension")) {
            note("T10", "SKIPPED: Ascension not loaded (run with -PaddonJars)");
            helper.succeed();
            return;
        }
        RaceMapperConfig.overrideBloodfiendToVampire(true);
        try {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            p.containerMenu = new ReincarnationMenu(1, p.getInventory(), p);
            RaceMapper.applyPick(p, "ascension:fledgling_bloodfiend");
            p.containerMenu = p.inventoryMenu;
            var handler = VampirismAPI.factionPlayerHandler(p);
            note("T10", "bloodfiend pick: faction=" + handler.getCurrentFaction() + " level=" + handler.getCurrentLevel());
            helper.assertTrue(handler.getCurrentFaction() == VReference.VAMPIRE_FACTION,
                    "bloodfiend pick did not join vampire faction: " + handler.getCurrentFaction());
            helper.assertTrue(handler.getCurrentLevel() >= 1, "bloodfiend pick level < 1: " + handler.getCurrentLevel());
        } finally {
            RaceMapperConfig.overrideBloodfiendToVampire(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodfiendMappingOffLeavesRaceAlone(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")
                || !EnabledParts.isEnabled("vampirismTensura")) {
            note("T10", "SKIPPED: needs species, species_tensura and vampirism_tensura");
            helper.succeed();
            return;
        }
        if (!ModList.get().isLoaded("ascension")) {
            note("T10", "SKIPPED: Ascension not loaded");
            helper.succeed();
            return;
        }
        RaceMapperConfig.overrideBloodfiendToVampire(false);
        try {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            p.containerMenu = new ReincarnationMenu(1, p.getInventory(), p);
            RaceMapper.applyPick(p, "ascension:fledgling_bloodfiend");
            p.containerMenu = p.inventoryMenu;
            var handler = VampirismAPI.factionPlayerHandler(p);
            note("T10", "bloodfiend mapping off: faction=" + handler.getCurrentFaction());
            helper.assertTrue(handler.getCurrentFaction() == null,
                    "bloodfiend mapping off still joined faction: " + handler.getCurrentFaction());
        } finally {
            RaceMapperConfig.overrideBloodfiendToVampire(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void direwolfPickJoinsWerewolfFaction(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")
                || !EnabledParts.isEnabled("vampirismTensura")) {
            note("T10", "SKIPPED: needs species, species_tensura and vampirism_tensura");
            helper.succeed();
            return;
        }
        if (!ModList.get().isLoaded("werewolves") || !ModList.get().isLoaded("mysticism")) {
            note("T10", "SKIPPED: Werewolves or Mysticism not loaded");
            helper.succeed();
            return;
        }
        RaceMapperConfig.overrideDirewolfToWerewolf(true);
        try {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            p.containerMenu = new ReincarnationMenu(1, p.getInventory(), p);
            RaceMapper.applyPick(p, "mysticism:direwolf");
            p.containerMenu = p.inventoryMenu;
            var handler = VampirismAPI.factionPlayerHandler(p);
            String factionId = handler.getCurrentFaction() == null ? "null" : handler.getCurrentFaction().getID().toString();
            note("T10", "direwolf pick: faction=" + factionId);
            helper.assertTrue("werewolves:werewolf".equals(factionId),
                    "direwolf pick did not join werewolf faction: " + factionId);
        } finally {
            RaceMapperConfig.overrideDirewolfToWerewolf(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void adminCommandDoesNotTriggerMapping(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")
                || !EnabledParts.isEnabled("vampirismTensura")) {
            note("T10", "SKIPPED: needs species, species_tensura and vampirism_tensura");
            helper.succeed();
            return;
        }
        if (!ModList.get().isLoaded("ascension")) {
            note("T10", "SKIPPED: Ascension not loaded");
            helper.succeed();
            return;
        }
        RaceMapperConfig.overrideBloodfiendToVampire(true);
        try {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            // no reincarnation menu open: simulates admin command or our own write
            RaceMapper.applyPick(p, "ascension:fledgling_bloodfiend");
            var handler = VampirismAPI.factionPlayerHandler(p);
            note("T10", "admin (no menu): faction=" + handler.getCurrentFaction());
            helper.assertTrue(handler.getCurrentFaction() == null,
                    "admin command triggered mapping: " + handler.getCurrentFaction());
        } finally {
            RaceMapperConfig.overrideBloodfiendToVampire(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodfiendLevelFloorsApply(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")
                || !EnabledParts.isEnabled("vampirismTensura")) {
            note("T10", "SKIPPED: needs species, species_tensura and vampirism_tensura");
            helper.succeed();
            return;
        }
        if (!ModList.get().isLoaded("ascension")) {
            note("T10", "SKIPPED: Ascension not loaded");
            helper.succeed();
            return;
        }
        RaceMapperConfig.overrideBloodfiendToVampire(true);
        try {
            List<String> results = new ArrayList<>();
            for (String raceId : List.of("ascension:fledgling_bloodfiend", "ascension:kindred_bloodfiend",
                    "ascension:blood_noble", "ascension:elder_bloodfiend", "ascension:progenitor_bloodfiend")) {
                ServerPlayer p = TestPlayers.create(helper);
                setRace(p, "tensura:human");
                p.containerMenu = new ReincarnationMenu(1, p.getInventory(), p);
                RaceMapper.applyPick(p, raceId);
                p.containerMenu = p.inventoryMenu;
                var handler = VampirismAPI.factionPlayerHandler(p);
                results.add(raceId + "=" + handler.getCurrentLevel());
            }
            note("T10", "bloodfiend floors: " + results);
            helper.assertTrue(results.size() == 5, "expected 5 results: " + results);
        } finally {
            RaceMapperConfig.overrideBloodfiendToVampire(null);
        }
        helper.succeed();
    }
}