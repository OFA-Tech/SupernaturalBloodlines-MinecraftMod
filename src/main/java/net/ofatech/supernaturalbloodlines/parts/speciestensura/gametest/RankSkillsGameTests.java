package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Set;
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

/** Registered by the part only when Werewolves is loaded (it links {@code WReference}). */
@PrefixGameTestTemplate(false)
public final class RankSkillsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String BEAST = "tensura:beast_transformation";
    private static final String SENSE = "tensura:godwolf_sense";
    private static final String COMMANDER = "tensura:commander";

    private RankSkillsGameTests() {}

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("tensura:human"), false);
        return p;
    }

    /**
     * A rank only counts while the player is a werewolf. An elf, so that the Beastfolk line (R20b) leaves the race alone: a
     * beastfolk has beast_transformation of its own, which would not be tracked as a rank skill.
     */
    private static ServerPlayer werewolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("tensura:elf"), false);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not become a werewolf");
        return p;
    }

    private static boolean has(ServerPlayer p, String skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse(skill)).isPresent();
    }

    private static void wolf(ServerPlayer p, WolfState.Rank rank, WolfState.Lineage lineage) {
        var s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().wolf(s.wolf().toBuilder().rank(rank).lineage(lineage).build()).build());
    }

    private static void rank(ServerPlayer p, WolfState.Rank rank) {
        wolf(p, rank, BloodlineAccess.get(p).wolf().lineage());
    }

    private static Set<String> tracked(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf().grantedSkills();
    }

    private static void learn(ServerPlayer p, String skill) {
        var instance = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(skill)).createDefaultInstance();
        instance.getOrCreateTag().putBoolean("NoMagiculeCost", true);
        SkillAPI.getSkillsFrom(p).learnSkill(instance);
    }

    private static String race(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaGetsRankSkills(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(has(p, BEAST) && has(p, SENSE), "an Alpha is missing a rank skill");
        helper.assertTrue(!has(p, COMMANDER), "an Alpha got the commander skill");
        helper.assertTrue(tracked(p).equals(Set.of(BEAST, SENSE)), "tracked " + tracked(p));
        helper.assertTrue("tensura:elf".equals(race(p)), "the race changed to " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void trueAlphaGetsTheCommanderSkill(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        rank(p, WolfState.Rank.ALPHA);
        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(has(p, BEAST) && has(p, SENSE) && has(p, COMMANDER), "a True Alpha is missing a rank skill");
        helper.assertTrue(tracked(p).equals(Set.of(BEAST, SENSE, COMMANDER)), "tracked " + tracked(p));
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(!has(p, COMMANDER) && has(p, BEAST), "stepping down to Alpha did not take only the commander skill");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void losingAlphaRemovesOnlyGrantedSkills(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        learn(p, SENSE);
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(has(p, BEAST), "control: the Alpha did not get beast_transformation");
        helper.assertTrue(tracked(p).equals(Set.of(BEAST)), "tracked a skill the player already had: " + tracked(p));

        rank(p, WolfState.Rank.NONE);
        helper.assertTrue(!has(p, BEAST), "the granted skill stayed after the rank was lost");
        helper.assertTrue(has(p, SENSE), "a skill the player had before was taken");
        helper.assertTrue(tracked(p).isEmpty(), "still tracked " + tracked(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void losingTheRankKeepsASkillTheLayerStillWants(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        var s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().species(Species.ORIGINAL_HYBRID).build());
        helper.assertTrue(has(p, SENSE) && has(p, BEAST), "control: the layer did not grant the skills");

        rank(p, WolfState.Rank.ALPHA);
        rank(p, WolfState.Rank.NONE);
        helper.assertTrue(has(p, SENSE) && has(p, BEAST), "losing the rank took a skill the layer still wants");

        // and the other way round: the last source to let go forgets it
        rank(p, WolfState.Rank.ALPHA);
        var now = BloodlineAccess.get(p);
        BloodlineAccess.set(p, now.toBuilder().species(Species.NONE).build());
        helper.assertTrue(has(p, SENSE) && has(p, BEAST), "losing the layer took a skill the rank still wants");
        rank(p, WolfState.Rank.NONE);
        helper.assertTrue(!has(p, SENSE) && !has(p, BEAST), "the skills stayed for good after both sources let go");
        helper.succeed();
    }

    /** Plan 10: a True Alpha keeps the rank when cured, so its skills have to wait for the rejoin. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void dormantTrueAlphaSkillsReturnOnRejoin(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(has(p, BEAST) && has(p, SENSE) && has(p, COMMANDER), "control: the True Alpha is missing a rank skill");
        var handler = VampirismAPI.factionPlayerHandler(p);
        handler.setFactionAndLevel(null, 0);
        helper.assertTrue(BloodlineAccess.get(p).wolf().rank() == WolfState.Rank.TRUE_ALPHA, "the cured True Alpha lost the rank: " + BloodlineAccess.get(p).wolf().rank());
        helper.assertTrue(!has(p, BEAST) && !has(p, SENSE) && !has(p, COMMANDER), "a dormant True Alpha kept rank skills");
        helper.assertTrue(tracked(p).isEmpty(), "still tracked " + tracked(p));
        helper.assertTrue(handler.setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not rejoin the werewolves");
        helper.assertTrue(has(p, BEAST) && has(p, SENSE) && has(p, COMMANDER), "the rank skills did not come back on rejoining");
        helper.assertTrue(tracked(p).equals(Set.of(BEAST, SENSE, COMMANDER)), "tracked " + tracked(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bornLevelFiveGetsGodwolfSense(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        wolf(p, WolfState.Rank.OMEGA, WolfState.Lineage.BORN);
        var handler = VampirismAPI.factionPlayerHandler(p);
        helper.assertTrue(handler.setFactionAndLevel(WReference.WEREWOLF_FACTION, 4), "could not reach werewolf level 4");
        helper.assertTrue(!has(p, SENSE), "godwolf_sense came before level 5");
        helper.assertTrue(handler.setFactionAndLevel(WReference.WEREWOLF_FACTION, 5), "could not reach werewolf level 5");
        helper.assertTrue(has(p, SENSE) && tracked(p).equals(Set.of(SENSE)), "no godwolf_sense at level 5 (tracked " + tracked(p) + ")");

        handler.setFactionAndLevel(null, 0);
        helper.assertTrue(!has(p, SENSE) && tracked(p).isEmpty(), "godwolf_sense stayed after leaving the werewolves");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bittenLevelFiveGetsNothing(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        wolf(p, WolfState.Rank.OMEGA, WolfState.Lineage.BITTEN);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 5), "could not reach level 5");
        helper.assertTrue(!has(p, SENSE), "a bitten werewolf got the born skill");
        helper.succeed();
    }
}
