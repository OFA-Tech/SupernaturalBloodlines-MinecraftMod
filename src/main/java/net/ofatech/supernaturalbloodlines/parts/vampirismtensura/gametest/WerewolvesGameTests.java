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
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

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
