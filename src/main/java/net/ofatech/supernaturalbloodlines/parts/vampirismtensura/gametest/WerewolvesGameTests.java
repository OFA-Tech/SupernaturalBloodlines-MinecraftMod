package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.data.existence.EntityExistenceData;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSyncConfig;

/**
 * The tests that need Werewolves classes. Registered by the part only when Werewolves is loaded
 * (VampirismTensuraPart), so nothing here is ever linked without it.
 */
@PrefixGameTestTemplate(false)
public final class WerewolvesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation GODWOLF_SENSE = ResourceLocation.parse("tensura:godwolf_sense");
    private static final String GRANTED = "supernaturalbloodlines:granted";

    private WerewolvesGameTests() {
    }

    private static void setRace(ServerPlayer p, String id) {
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(id), false);
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static boolean hasGodwolfSense(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(GODWOLF_SENSE).isPresent();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfFactionIdMatchesTheRule(GameTestHelper helper) {
        helper.assertTrue(WReference.WEREWOLF_FACTION.getID().toString().equals("werewolves:werewolf"),
                "the werewolf faction id is " + WReference.WEREWOLF_FACTION.getID());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void undeadCannotJoinWerewolves(GameTestHelper helper) {
        IPlayableFaction<?> wolf = WReference.WEREWOLF_FACTION;
        for (String raceId : List.of("tensura:ghoul", "tensura:lesser_daemon", "tensura:slime", "tensura:metal_slime")) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, raceId);
            helper.assertTrue(!handler(p).canJoin(wolf), raceId + " could join the werewolves");
        }
        // control: a plain race can, so the checks above can fail
        for (String raceId : List.of("tensura:human", "tensura:elf")) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, raceId);
            helper.assertTrue(handler(p).canJoin(wolf), "control: " + raceId + " could not join the werewolves");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfMaxLevelGetsGodwolfSense(GameTestHelper helper) {
        IPlayableFaction<?> wolf = WReference.WEREWOLF_FACTION;
        int max = wolf.getHighestReachableLevel();
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");

        helper.assertTrue(handler(p).setFactionAndLevel(wolf, 1), "could not become a level 1 werewolf");
        helper.assertTrue(!hasGodwolfSense(p), "godwolf_sense at level 1");
        helper.assertTrue(handler(p).setFactionAndLevel(wolf, max), "could not reach the max werewolf level");
        helper.assertTrue(hasGodwolfSense(p), "no godwolf_sense at werewolf level " + max);

        // Tensura's free-of-charge switch is one-shot (it removes it itself), and our own mark stays
        var tag = SkillAPI.getSkillsFrom(p).getSkill(GODWOLF_SENSE).orElseThrow().getTag();
        helper.assertTrue(tag != null && tag.getBoolean(GRANTED), "the granted skill carries no grant mark: " + tag);
        helper.assertTrue(!tag.contains("NoMagiculeCost"), "NoMagiculeCost stayed on the skill: " + tag);

        handler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(!hasGodwolfSense(p), "godwolf_sense stayed after leaving the werewolves");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void skillTheWerewolfAlreadyHadIsNotRemoved(GameTestHelper helper) {
        IPlayableFaction<?> wolf = WReference.WEREWOLF_FACTION;
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        ManasSkillInstance own = SkillAPI.getSkillRegistry().get(GODWOLF_SENSE).createDefaultInstance();
        own.getOrCreateTag().putBoolean("NoMagiculeCost", true);
        helper.assertTrue(SkillAPI.getSkillsFrom(p).learnSkill(own), "could not give the player the skill first");

        handler(p).setFactionAndLevel(wolf, wolf.getHighestReachableLevel());
        handler(p).setFactionAndLevel(wolf, 1);
        helper.assertTrue(hasGodwolfSense(p), "a skill the player had before was removed by a level drop");
        handler(p).setFactionAndLevel(wolf, wolf.getHighestReachableLevel());
        handler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(hasGodwolfSense(p), "a skill the player had before was removed on leaving");
        helper.succeed();
    }

    // ---- R20b: the Beastfolk line ----

    private static String race(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse("none");
    }

    private static void rank(ServerPlayer p, WolfState.Rank rank) {
        WolfState w = WolfState.NONE.toBuilder().lineage(WolfState.Lineage.BITTEN).rank(rank).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w).build());
    }

    private static void join(GameTestHelper helper, ServerPlayer p) {
        helper.assertTrue(handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not become a werewolf: " + race(p));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanWerewolfBecomesBeastfolk(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        join(helper, p);
        helper.assertTrue("tensura:beastfolk".equals(race(p)), "the new werewolf is a " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaOnBeastfolkBecomesBeastLord(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        join(helper, p);
        rank(p, WolfState.Rank.BETA);
        helper.assertTrue("tensura:beastfolk".equals(race(p)), "a Beta is a " + race(p));
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue("tensura:beast_lord".equals(race(p)), "the Alpha is a " + race(p));
        rank(p, WolfState.Rank.OMEGA);
        helper.assertTrue("tensura:beast_lord".equals(race(p)), "the race was lowered to " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void trueAlphaBecomesSpiritBeast(GameTestHelper helper) {
        ServerPlayer lord = TestPlayers.create(helper);
        setRace(lord, "tensura:human");
        join(helper, lord);
        rank(lord, WolfState.Rank.ALPHA);
        rank(lord, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue("tensura:spirit_beast".equals(race(lord)), "the True Alpha is a " + race(lord));
        rank(lord, WolfState.Rank.ALPHA);
        helper.assertTrue("tensura:spirit_beast".equals(race(lord)), "the race was lowered to " + race(lord));

        // straight from a beastfolk, and on rejoining with the rank already there
        ServerPlayer direct = TestPlayers.create(helper);
        setRace(direct, "tensura:human");
        join(helper, direct);
        rank(direct, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue("tensura:spirit_beast".equals(race(direct)), "the beastfolk True Alpha is a " + race(direct));
        ServerPlayer rejoin = TestPlayers.create(helper);
        setRace(rejoin, "tensura:human");
        rank(rejoin, WolfState.Rank.TRUE_ALPHA);
        join(helper, rejoin);
        helper.assertTrue("tensura:spirit_beast".equals(race(rejoin)), "the rejoining True Alpha is a " + race(rejoin));

        // a divine beast stays what Tensura made it
        ServerPlayer divine = TestPlayers.create(helper);
        setRace(divine, "tensura:divine_beast");
        join(helper, divine);
        rank(divine, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue("tensura:divine_beast".equals(race(divine)), "the divine beast became " + race(divine));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nonHumanWerewolfKeepsRace(GameTestHelper helper) {
        // an elf, and a vampire-raced hybrid (a plain vampire-race player is cured to human by the vampire sync when they
        // leave the vampires, so the hybrid is the real case); the hybrid is made first so the vampire sync ignores it
        for (String raceId : List.of("tensura:elf", "tensura:vampire")) {
            ServerPlayer p = TestPlayers.create(helper);
            if (raceId.equals("tensura:vampire")) {
                BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(Species.HYBRID).build());
            }
            setRace(p, raceId);
            join(helper, p);
            rank(p, WolfState.Rank.ALPHA);
            rank(p, WolfState.Rank.TRUE_ALPHA);
            helper.assertTrue(raceId.equals(race(p)), raceId + " became " + race(p));
        }
        // a hybrid's race belongs to the species layer, even when it is a plain human
        ServerPlayer hybrid = TestPlayers.create(helper);
        setRace(hybrid, "tensura:human");
        BloodlineAccess.set(hybrid, BloodlineAccess.get(hybrid).toBuilder().species(Species.HYBRID).build());
        join(helper, hybrid);
        helper.assertTrue("tensura:human".equals(race(hybrid)), "the hybrid became " + race(hybrid));
        // not a werewolf: a rank alone changes nothing
        ServerPlayer human = TestPlayers.create(helper);
        setRace(human, "tensura:beastfolk");
        rank(human, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue("tensura:beastfolk".equals(race(human)), "a non-werewolf became " + race(human));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void switchOffLeavesRace(GameTestHelper helper) throws ReflectiveOperationException {
        var field = VampireSyncConfig.class.getDeclaredField("WEREWOLF_BEASTFOLK_ENABLED");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var enabled = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) field.get(null);
        helper.assertTrue(enabled.get(), "the switch is not on by default");
        enabled.set(false);
        try {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, "tensura:human");
            join(helper, p);
            rank(p, WolfState.Rank.ALPHA);
            helper.assertTrue("tensura:human".equals(race(p)), "the switch was off and the race became " + race(p));
        } finally {
            enabled.set(true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfMobsHaveEp(GameTestHelper helper) {
        for (String id : List.of("werewolves:human_werewolf", "werewolves:werewolf_beast", "werewolves:werewolf_survivalist",
                "werewolves:alpha_werewolf")) {
            EntityExistenceData d = helper.getLevel().registryAccess().registryOrThrow(TensuraCustomData.ENTITY_EXISTENCE).stream()
                    .filter(e -> e.entity().toString().equals(id)).findFirst().orElse(null);
            helper.assertTrue(d != null, "no existence data for " + id);
            helper.assertTrue(d.maxMagicule() + d.maxAura() > 0 && d.spiritualHP() > 0, "empty existence data for " + id);
        }
        helper.succeed();
    }
}
