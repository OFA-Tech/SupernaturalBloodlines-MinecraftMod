package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.PrimevalRoarGrant;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/** S-WOL-8 Primeval Roar: granted to a True Alpha on alpha_primeval. Registered only when Werewolves is loaded (it links {@code WReference}). */
@PrefixGameTestTemplate(false)
public final class PrimevalGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String PRIMEVAL = NS + ":alpha_primeval";
    private static final String SKILL = PrimevalRoarGrant.SKILL;

    private PrimevalGameTests() {}

    private static ServerPlayer werewolf(GameTestHelper helper, String race) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not become a werewolf");
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(race), false);
        return p;
    }

    private static void rank(ServerPlayer p, WolfState.Rank rank) {
        var s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().wolf(s.wolf().toBuilder().rank(rank).build()).build());
    }

    private static boolean granted(ServerPlayer p) {
        return SupernaturalAccess.get(p).skills().contains(SKILL);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCatalogueHasTheGrantedSkill(GameTestHelper helper) {
        var spec = SkillTable.current().skill(SKILL);
        helper.assertTrue(spec.isPresent() && spec.get().grantedOnly(), "S-WOL-8 is not in the catalogue as granted-only: " + spec);
        helper.succeed();
    }

    /** The grant follows the rank at once, with no 200-tick wait. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void trueAlphaOnAlphaPrimevalIsGrantedPrimevalRoar(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, PRIMEVAL);
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(!granted(p) && !LadderQuery.owns(p, SKILL), "an Alpha was granted the Primeval Roar");
        int sp = SupernaturalAccess.get(p).sp();

        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(granted(p), "becoming a True Alpha did not grant the Primeval Roar");
        helper.assertTrue(LadderQuery.owns(p, SKILL), "the granted skill is not active");
        helper.assertTrue(LadderQuery.value(p, SKILL, "range_factor", 1) == 2, "range_factor " + LadderQuery.value(p, SKILL, "range_factor", 1));
        helper.assertTrue(SupernaturalAccess.get(p).sp() == sp, "the grant cost SP");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDormantTrueAlphaLosesItUntilTheRejoin(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, PRIMEVAL);
        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(granted(p), "control: not granted");

        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        PrimevalRoarGrant.sync(p); // the 200-tick pass; the faction event does the same at once
        helper.assertTrue(!granted(p) && !LadderQuery.owns(p, SKILL), "a dormant True Alpha kept the Primeval Roar");
        helper.assertTrue(LadderQuery.value(p, SKILL, "range_factor", 1) == 1, "the dormant skill still gives its number");
        helper.assertTrue(BloodlineAccess.get(p).wolf().rank() == WolfState.Rank.TRUE_ALPHA, "the rank was lost (R91)");

        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not rejoin");
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(granted(p) && LadderQuery.owns(p, SKILL), "the rejoin did not bring the Primeval Roar back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyOnTheApexRace(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, "tensura:human_saint");
        rank(p, WolfState.Rank.TRUE_ALPHA);
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(!granted(p), "a True Alpha on a saint was granted it");
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(PRIMEVAL), false);
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(granted(p), "a True Alpha on alpha_primeval was not granted it");
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("tensura:human_saint"), false);
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(!granted(p), "it stayed after the race changed");
        helper.succeed();
    }

    /** A bought skill is never touched by the grant's sync. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherSkillsAreLeftAlone(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, "tensura:human_saint");
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(java.util.Set.of("S-WOL-1", "S-WOL-6")).build());
        rank(p, WolfState.Rank.TRUE_ALPHA);
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(SupernaturalAccess.get(p).skills().equals(java.util.Set.of("S-WOL-1", "S-WOL-6")), "skills " + SupernaturalAccess.get(p).skills());
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(PRIMEVAL), false);
        PrimevalRoarGrant.sync(p);
        helper.assertTrue(SupernaturalAccess.get(p).skills().equals(java.util.Set.of("S-WOL-1", "S-WOL-6", SKILL)), "skills " + SupernaturalAccess.get(p).skills());
        helper.succeed();
    }
}
