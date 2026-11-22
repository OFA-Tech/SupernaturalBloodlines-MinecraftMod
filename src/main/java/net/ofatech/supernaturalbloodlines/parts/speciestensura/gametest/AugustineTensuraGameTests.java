package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.race.RaceUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/** Phase 18c Task 2b (R200, owner Q3): an Augustine gets a plain vampire's Tensura treatment: race, layer, undead, apex offer, ladder top. Runs with Tensura. */
@PrefixGameTestTemplate(false)
public final class AugustineTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private AugustineTensuraGameTests() {
    }

    private static void setRace(LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
    }

    private static boolean has(ServerPlayer p, String skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse(skill)).isPresent();
    }

    private static List<String> offered(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().orElseThrow().getNextEvolutions(p).stream().map(r -> r.getRegistryName().toString()).toList();
    }

    private static boolean laddersGateApex() {
        return EnabledParts.isEnabled("supernatural") && EnabledParts.isEnabled("supernaturalTensura");
    }

    private static void setLadder(ServerPlayer p, int rung) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().schema(MigrationRules.SCHEMA).rung(SupernaturalRace.AUGUSTINE, rung).build());
    }

    private static ServerPlayer vampireOf(GameTestHelper helper, String tensuraRace, Species s) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, tensuraRace);
        species(p, s);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not join the vampire faction");
        return p;
    }

    /** R20a: a vampire with no species, whose race is exactly tensura:human, becomes tensura:vampire. An Augustine counts as one; a heretic does not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineWhoIsHumanBecomesTensuraVampire(GameTestHelper helper) {
        ServerPlayer augustine = vampireOf(helper, "tensura:human", Species.AUGUSTINE);
        ServerPlayer plain = vampireOf(helper, "tensura:human", Species.NONE);
        ServerPlayer heretic = vampireOf(helper, "tensura:human", Species.HERETIC);
        helper.assertTrue("tensura:vampire".equals(race(plain)), "control: a plain vampire's race is " + race(plain));
        helper.assertTrue("tensura:vampire".equals(race(augustine)), "an Augustine's race is " + race(augustine));
        helper.assertTrue("tensura:human".equals(race(heretic)), "control: a heretic's race changed to " + race(heretic));
        // the cure takes the same way back
        VampirismAPI.factionPlayerHandler(augustine).setFactionAndLevel(null, 0);
        helper.assertTrue("tensura:human".equals(race(augustine)), "the cure left an Augustine on " + race(augustine));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineKeepsAnElf(GameTestHelper helper) {
        ServerPlayer augustine = vampireOf(helper, "tensura:elf", Species.AUGUSTINE);
        helper.assertTrue("tensura:elf".equals(race(augustine)), "an Augustine elf's race changed to " + race(augustine));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineGetsThePlainVampiresLayerAndUndeadTreatment(GameTestHelper helper) {
        ServerPlayer augustine = vampireOf(helper, "tensura:elf", Species.AUGUSTINE);
        ServerPlayer plain = vampireOf(helper, "tensura:elf", Species.NONE);
        helper.assertTrue(RaceUtils.isUndead(plain), "control: a plain vampire is not undead for Tensura");
        helper.assertTrue(RaceUtils.isUndead(augustine), "an Augustine is not undead for Tensura");
        helper.assertTrue(has(plain, "tensura:drain") && has(augustine, "tensura:drain"), "the layer's skill: plain " + has(plain, "tensura:drain") + ", Augustine " + has(augustine, "tensura:drain"));
        helper.assertTrue(BloodlineAccess.get(augustine).layerSkills().equals(Set.of("tensura:drain")), "tracked " + BloodlineAccess.get(augustine).layerSkills());
        helper.assertTrue(BloodlineAccess.get(augustine).layerSkills().equals(BloodlineAccess.get(plain).layerSkills()), "not the plain vampire's layer");
        // and it goes with the species
        species(augustine, Species.NONE);
        VampirismAPI.factionPlayerHandler(augustine).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3);
        helper.assertTrue(has(augustine, "tensura:drain"), "a plain vampire (the Augustine, cured of the species) lost the layer");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineIsOfferedElderVampire(GameTestHelper helper) {
        ServerPlayer augustine = TestPlayers.create(helper);
        setRace(augustine, "tensura:human_saint");
        species(augustine, Species.AUGUSTINE);
        helper.assertTrue(offered(augustine).contains(NS + ":elder_vampire"), "an Augustine is not offered elder_vampire: " + offered(augustine));
        // a plain vampire is offered the same
        ServerPlayer plain = vampireOf(helper, "tensura:human_saint", Species.NONE);
        helper.assertTrue(offered(plain).contains(NS + ":elder_vampire"), "control: a plain vampire is not offered it: " + offered(plain));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineApexNeedsTheTopRung(GameTestHelper helper) {
        if (!laddersGateApex()) {
            helper.succeed(); // nothing to gate on
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human_saint");
        species(p, Species.AUGUSTINE);
        int top = LadderTable.current().get(SupernaturalRace.AUGUSTINE).rungs().size() - 1;
        helper.assertTrue(top == 6, "the Augustine ladder has " + (top + 1) + " rungs, not 7 (L-AUG-6 is the Tensura rung)");
        setLadder(p, top - 1);
        helper.assertTrue(!LadderQuery.apexRungReached(p, SupernaturalRace.AUGUSTINE), "reached below L-AUG-6");
        helper.assertTrue(!offered(p).contains(NS + ":elder_vampire"), "offered below the top rung: " + offered(p));
        setLadder(p, top);
        helper.assertTrue(LadderQuery.apexRungReached(p, SupernaturalRace.AUGUSTINE), "not reached at L-AUG-6");
        helper.assertTrue(offered(p).contains(NS + ":elder_vampire"), "not offered at the top rung: " + offered(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void augustineCuredFallsBackLikeAVampire(GameTestHelper helper) {
        ServerPlayer p = vampireOf(helper, "tensura:vampire", Species.AUGUSTINE);
        helper.assertTrue("tensura:vampire".equals(race(p)), "setup: race " + race(p));
        // the cure leaves the faction and the species: a race in the vampire line goes back to human
        BloodlineAccess.set(p, BloodlineState.NONE);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue("tensura:human".equals(race(p)), "a cured Augustine kept the vampire race: " + race(p));
        // an elf is not in the vampire line: the cure leaves it
        ServerPlayer elf = vampireOf(helper, "tensura:elf", Species.AUGUSTINE);
        BloodlineAccess.set(elf, BloodlineState.NONE);
        VampirismAPI.factionPlayerHandler(elf).setFactionAndLevel(null, 0);
        helper.assertTrue("tensura:elf".equals(race(elf)), "a cured Augustine elf lost its race: " + race(elf));
        helper.succeed();
    }
}
