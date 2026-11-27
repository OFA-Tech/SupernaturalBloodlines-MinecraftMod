package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest;

import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VReference;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Plan 23 Task 0: what the race hooks the playtest fixes lean on really do (P1 to P5). Each proof logs a {@code PLAN23 Pn} line for the
 * research note. No production behaviour is touched.
 */
@PrefixGameTestTemplate(false)
public final class RaceHookGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NO_EVOLVE = "supernaturalbloodlines.turning.ghoul_no_evolve";

    private RaceHookGameTests() {
    }

    private static void note(String proof, String text) {
        SupernaturalBloodlines.LOGGER.info("PLAN23 {} {}", proof, text);
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse("none");
    }

    private static String id(ManasRaceInstance r) {
        return r == null ? "none" : r.getRaceId().toString();
    }

    private static ManasRace raceOf(String id) {
        return RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
    }

    private static boolean setRace(LivingEntity e, String id) {
        return RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    /** One SET_RACE call as a listener sees it. */
    private record Seen(String change, boolean flag, boolean changeable, boolean message, boolean menuOpen) {
        @Override
        public String toString() {
            return change + " flag=" + flag + " changeable=" + changeable + " message=" + message + " reincarnationMenuOpen=" + menuOpen;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPickAnEvolutionAndOurOwnWriteLookDifferentToSetRace(GameTestHelper helper) throws Exception {
        ServerPlayer p = TestPlayers.create(helper);
        List<Seen> seen = new ArrayList<>();
        RaceEvents.SetRaceEvent probe = (oldR, entity, newR, flag, changeable, message) -> {
            if (entity == p) {
                seen.add(new Seen(id(oldR) + ">" + id(newR), flag, Boolean.TRUE.equals(changeable.get()), message.isPresent(),
                        p.containerMenu instanceof ReincarnationMenu));
            }
            return EventResult.pass();
        };
        RaceEvents.SET_RACE.register(probe);
        String command = null;
        try {
            setRace(p, "tensura:human");
            seen.clear();
            // (a) the reincarnation pick: the menu is the player's open container while ReincarnationMenu.setRace runs inside clickMenuButton
            p.containerMenu = new ReincarnationMenu(1, p.getInventory(), p);
            ReincarnationMenu.setRace(p, raceOf("tensura:elf"), false, false);
            p.containerMenu = p.inventoryMenu;
            // (b) our own write, as VampireSync and WerewolfRaceSync do it
            setRace(p, "tensura:dwarf");
            // (c) an evolution (Races.evolveRace)
            setRace(p, "tensura:human");
            seen.remove(seen.size() - 1);
            RaceAPI.getRaceFrom(p).evolveRace(ResourceLocation.parse("tensura:enlightened_human"));
            // (d) Tensura's race command, run by an operator
            var source = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            for (String candidate : List.of("tensura edit @s race set tensura:merfolk", "tensura race @s set tensura:merfolk",
                    "tensura edit race @s set tensura:merfolk", "tensura race set @s tensura:merfolk")) {
                try {
                    dispatcher.execute(candidate, source);
                    command = candidate;
                    break;
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                    // the next spelling
                }
            }
            if (command == null) {
                note("P1", "race command usage: " + String.join(" | ", dispatcher.getSmartUsage(dispatcher.getRoot().getChild("tensura"), source).values()));
            }
        } finally {
            RaceEvents.SET_RACE.unregister(probe);
            p.containerMenu = p.inventoryMenu;
        }
        note("P1", "seen " + seen + " command=" + command);
        helper.assertTrue(seen.size() == 4, "SET_RACE fired " + seen.size() + " times: " + seen);
        Seen pick = seen.get(0);
        Seen ours = seen.get(1);
        Seen evolution = seen.get(2);
        Seen admin = seen.get(3);
        helper.assertTrue(command != null, "no spelling of the race command ran");
        helper.assertTrue(pick.change().equals("tensura:human>tensura:elf") && !pick.flag() && pick.changeable() && pick.message() && pick.menuOpen(), "pick " + pick);
        helper.assertTrue(!ours.flag() && !ours.changeable() && !ours.message() && !ours.menuOpen(), "our write " + ours);
        helper.assertTrue(evolution.flag() && !evolution.changeable() && !evolution.menuOpen(), "evolution " + evolution);
        helper.assertTrue(admin.change().endsWith(">tensura:merfolk") && !admin.menuOpen(), "command " + admin);
        // finding: the command and our own write carry the same flags, so only the open menu (or the changeable flag) separates a pick
        helper.assertTrue(admin.flag() == ours.flag() && admin.changeable() == ours.changeable(), "the command differs from our write: " + admin + " vs " + ours);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRaceALaterListenerRefusedIsUnchangedNextTick(GameTestHelper helper) {
        ServerPlayer refused = TestPlayers.create(helper);
        ServerPlayer control = TestPlayers.create(helper);
        setRace(refused, "tensura:human");
        setRace(control, "tensura:human");
        RaceEvents.SetRaceEvent veto = (oldR, entity, newR, flag, changeable, message) ->
                entity == refused ? EventResult.interruptFalse() : EventResult.pass();
        RaceEvents.SET_RACE.register(veto);
        boolean refusedResult;
        boolean controlResult;
        try {
            refusedResult = setRace(refused, "tensura:dwarf");
            controlResult = setRace(control, "tensura:dwarf");
        } finally {
            RaceEvents.SET_RACE.unregister(veto);
        }
        String refusedNow = race(refused);
        List<String> nextTick = new ArrayList<>();
        Consumer<ServerTickEvent.Post> reader = new Consumer<>() {
            @Override
            public void accept(ServerTickEvent.Post e) {
                if (nextTick.isEmpty()) {
                    nextTick.add(race(refused));
                    nextTick.add(race(control));
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(reader);
        boolean finalRefusedResult = refusedResult;
        boolean finalControlResult = controlResult;
        helper.runAfterDelay(3, () -> {
            NeoForge.EVENT_BUS.unregister(reader);
            note("P2", "setRace refused=" + finalRefusedResult + " control=" + finalControlResult + " now=" + refusedNow + " nextTick=" + nextTick);
            helper.assertTrue(!finalRefusedResult && finalControlResult, "setRace answered " + finalRefusedResult + "/" + finalControlResult);
            helper.assertTrue("tensura:human".equals(refusedNow), "a refused race changed at once: " + refusedNow);
            helper.assertTrue(List.of("tensura:human", "tensura:dwarf").equals(nextTick), "next tick saw " + nextTick);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulEvolutionToVampireIsRefusedToday(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")) {
            note("P3", "SKIPPED: the Ghoul needs species and species_tensura");
            helper.succeed();
            return;
        }
        RecordingPlayer ghoul = RecordingPlayer.create(helper);
        ServerPlayer elf = TestPlayers.create(helper);
        List<String> changes = new ArrayList<>();
        Consumer<PlayerFactionEvent.FactionLevelChanged> counter = ev -> {
            if (ev.getPlayer().getPlayer() == elf) {
                changes.add(ev.getOldLevel() + ">" + ev.getNewLevel());
            }
        };
        try {
            setRace(ghoul, "tensura:human");
            var sanguinare = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:sanguinare")).orElseThrow();
            ghoul.addEffect(new MobEffectInstance(sanguinare, 100));
            sanguinare.value().applyEffectTick(ghoul, 0);
            helper.assertTrue(BloodlineAccess.get(ghoul).ghoul() && "tensura:ghoul".equals(race(ghoul))
                    && handler(ghoul).getCurrentFaction() == VReference.VAMPIRE_FACTION && handler(ghoul).getCurrentLevel() == 1,
                    "setup: ghoul=" + BloodlineAccess.get(ghoul).ghoul() + " race=" + race(ghoul) + " level=" + handler(ghoul).getCurrentLevel());
            boolean evolved = RaceAPI.getRaceFrom(ghoul).evolveRace(ResourceLocation.parse("tensura:vampire"));
            boolean levelTwo = handler(ghoul).setFactionAndLevel(VReference.VAMPIRE_FACTION, 2);
            int ghoulLevel = handler(ghoul).getCurrentLevel();

            setRace(elf, "tensura:elf");
            helper.assertTrue(handler(elf).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "setup: the elf could not join");
            NeoForge.EVENT_BUS.addListener(counter);
            setRace(elf, "tensura:vampire");
            List<String> afterRace = List.copyOf(changes);
            // control: a real level change of the same player is seen, so an empty list above means nothing changed
            handler(elf).setFactionAndLevel(VReference.VAMPIRE_FACTION, 2);
            NeoForge.EVENT_BUS.unregister(counter);
            note("P3", "ghoul evolve->vampire=" + evolved + " race=" + race(ghoul) + " told=" + ghoul.received(NO_EVOLVE)
                    + " level2=" + levelTwo + " level=" + ghoulLevel + "; level-1 vampire on tensura:vampire faction changes=" + afterRace + " control=" + changes);
            helper.assertTrue(!evolved && "tensura:ghoul".equals(race(ghoul)) && ghoul.received(NO_EVOLVE), "the Ghoul evolution was not refused: " + race(ghoul));
            helper.assertTrue(ghoulLevel == 1, "a Ghoul reached level " + ghoulLevel);
            helper.assertTrue(List.of("1>2").equals(changes), "the probe did not see the control level change: " + changes);
            helper.assertTrue(afterRace.isEmpty(), "raiseToRaceFloor changed a level-1 vampire: " + afterRace);
        } finally {
            NeoForge.EVENT_BUS.unregister(counter);
            BloodlineAccess.set(ghoul, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void speciesOffLeavesADirewolfPickAlone(GameTestHelper helper) {
        if (EnabledParts.isEnabled("species")) {
            note("P4", "SKIPPED: species is on (this proof is for -PpartsOff=species)");
            helper.succeed();
            return;
        }
        List<String> rows = new ArrayList<>();
        for (String raceId : List.of("mysticism:direwolf", "tensura:beastfolk", "tensura:ghoul")) {
            if (raceOf(raceId) == null) {
                rows.add(raceId + " not registered");
                continue;
            }
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            setRace(p, raceId);
            rows.add(raceId + " race=" + race(p) + " faction=" + handler(p).getCurrentFaction());
            helper.assertTrue(handler(p).getCurrentFaction() == null, raceId + " gave a faction with species off: " + handler(p).getCurrentFaction());
        }
        note("P4", String.join("; ", rows));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUndeadOrNoBloodRaceCannotJoinTheWerewolves(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves")) {
            note("P5", "SKIPPED: Werewolves is not loaded");
            helper.succeed();
            return;
        }
        IPlayableFaction<?> werewolves = null;
        for (IPlayableFaction<?> f : VampirismAPI.factionRegistry().getPlayableFactions()) {
            if (f.getID().toString().equals("werewolves:werewolf")) {
                werewolves = f;
            }
        }
        helper.assertTrue(werewolves != null, "no werewolves:werewolf faction");
        boolean awaken = EnabledParts.isEnabled("species") && EnabledParts.isEnabled("vampirismEnchanted");
        List<String> rows = new ArrayList<>();
        for (String raceId : List.of("tensura:ghoul", "tensura:slime", "tensura:wight")) {
            helper.assertTrue(raceOf(raceId) != null, raceId + " is not a race");
            // the awakening first, on a player in no faction: GeneHandler.awakenHuman asks joinAllowed (CanJoinFaction) before anything else
            ServerPlayer woken = TestPlayers.create(helper);
            setRace(woken, "tensura:human");
            setRace(woken, raceId);
            String awakened = "not run";
            if (awaken) {
                BloodlineState before = BloodlineAccess.get(woken);
                awakened = GeneHandler.awakenHuman(woken,
                        PackEvent.GENE_AWAKENED) + " stateUnchanged=" + BloodlineAccess.get(woken).equals(before)
                        + " faction=" + handler(woken).getCurrentFaction();
            }
            // then the raw join on another player: setFactionAndLevel does not post CanJoinFaction (finding), so only canJoin carries R103
            ServerPlayer raw = TestPlayers.create(helper);
            setRace(raw, "tensura:human");
            setRace(raw, raceId);
            boolean canJoin = handler(raw).canJoin(werewolves);
            boolean joined = handler(raw).setFactionAndLevel(werewolves, 1);
            rows.add(raceId + " race=" + race(raw) + " canJoin=" + canJoin + " rawSetFactionAndLevel=" + joined
                    + " undeadOrDaemon=" + RaceTags.isUndeadOrDaemon(RaceAPI.getRaceFrom(raw).getRace().orElse(null))
                    + " bloodless=" + FactionRaceRules.isBloodless(raw) + " awakenHuman=" + awakened);
            handler(raw).setFactionAndLevel(null, 0);
        }
        note("P5", String.join("; ", rows));
        for (String row : rows) {
            helper.assertTrue(row.contains("canJoin=false rawSetFactionAndLevel=true"), "CanJoinFaction or the raw join differs from the proof: " + row);
            helper.assertTrue(!awaken || row.contains("awakenHuman=" + FactionRiteFactory.JOIN_DENIED
                    + " stateUnchanged=true faction=null"), "the awakening was not refused by R103: " + row);
        }
        helper.succeed();
    }

    /** Finding for Task 14: the awakening the plan models a wolf pick on refuses a player who has no werewolf gene. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGeneAwakeningRefusesAHumanWithoutTheGene(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves") || !EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("vampirismEnchanted")) {
            note("P5b", "SKIPPED: needs Werewolves, species and vampirism_enchanted");
            helper.succeed();
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        setRace(p, "tensura:beastfolk");
        BloodlineState before = BloodlineAccess.get(p);
        String refusal = GeneHandler.awakenHuman(p, PackEvent.GENE_AWAKENED);
        note("P5b", "beastfolk human without the gene: awakenHuman(GENE_AWAKENED)=" + refusal + " faction=" + handler(p).getCurrentFaction());
        helper.assertTrue("supernaturalbloodlines.refusal.no_gene".equals(refusal) && BloodlineAccess.get(p).equals(before) && handler(p).getCurrentFaction() == null,
                "a gene-less awakening answered " + refusal);
        helper.succeed();
    }
}
