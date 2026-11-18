package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexFallback;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.PrimevalBoost;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;

/** The werewolf side of the apex races: registered by the part only when Werewolves is loaded (it links {@code WReference}). */
@PrefixGameTestTemplate(false)
public final class ApexWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ApexWerewolfGameTests() {}

    private static void setRace(ServerPlayer p, String id) {
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static List<String> offered(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().orElseThrow().getNextEvolutions(p).stream().map(r -> r.getRegistryName().toString()).toList();
    }

    private static boolean offers(ServerPlayer p, String id) {
        return offered(p).contains(NS + ":" + id);
    }

    private static boolean offersApex(ServerPlayer p) {
        return offered(p).stream().anyMatch(id -> id.startsWith(NS + ":"));
    }

    private static ServerPlayer werewolf(GameTestHelper helper, Species species) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(species).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not become a werewolf");
        setRace(p, "tensura:human_saint");
        return p;
    }

    private static void rank(ServerPlayer p, WolfState.Rank rank) {
        var s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().wolf(s.wolf().toBuilder().rank(rank).build()).build());
    }

    private static boolean laddersGateApex() {
        return EnabledParts.isEnabled("supernatural") && EnabledParts.isEnabled("supernaturalTensura");
    }

    private static void setLadder(ServerPlayer p, SupernaturalRace race, int rung) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().schema(MigrationRules.SCHEMA).rung(race, rung).build());
    }

    private static int top(SupernaturalRace race) {
        return LadderTable.current().get(race).rungs().size() - 1;
    }

    /** D1 and D2: an Alpha werewolf (the apex is for those who hold a spark, see the gate test below) is offered {@code alpha_primeval} from a saint of the human-like lines and from Spirit Beast on Beastfolk. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaWerewolfIsOfferedAlphaPrimeval(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
        rank(p, WolfState.Rank.ALPHA);
        for (String id : new String[] {"tensura:human_saint", "tensura:elf_saint", "tensura:spirit_beast", "tensura:divine_beast"}) {
            setRace(p, id);
            helper.assertTrue(offers(p, "alpha_primeval"), "a werewolf on " + id + " is not offered the apex race: " + offered(p));
        }
        for (String id : new String[] {"tensura:beastfolk", "tensura:beast_lord", "tensura:vampire_lord", "tensura:vampire_overcomer", "tensura:human"}) {
            setRace(p, id);
            helper.assertTrue(!offersApex(p), "a werewolf on " + id + " is offered: " + offered(p));
        }
        setRace(p, NS + ":alpha_primeval");
        helper.assertTrue(offered(p).isEmpty(), "the apex race has a next evolution: " + offered(p));
        helper.succeed();
    }

    /** 12d Task 3: the gate is the werewolf ladder's own top rung (L-WOL-6). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfApexNeedsTheTopRung(GameTestHelper helper) {
        if (!laddersGateApex()) {
            helper.succeed();
            return;
        }
        ServerPlayer p = werewolf(helper, Species.NONE);
        rank(p, WolfState.Rank.ALPHA);
        setRace(p, "tensura:spirit_beast");
        setLadder(p, SupernaturalRace.WEREWOLF, top(SupernaturalRace.WEREWOLF) - 1);
        helper.assertTrue(!offersApex(p), "offered below L-WOL-6: " + offered(p));
        setLadder(p, SupernaturalRace.WEREWOLF, top(SupernaturalRace.WEREWOLF));
        helper.assertTrue(offers(p, "alpha_primeval"), "not offered at L-WOL-6: " + offered(p));
        helper.succeed();
    }

    /** A witch-werewolf can reach two apex rungs; Tensura's screen then lists both, on a saint and on Beastfolk. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchWerewolfOfferedBothApexes(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.WITCH);
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(PlayerRaces.of(p).equals(List.of(SupernaturalRace.WEREWOLF, SupernaturalRace.WITCH)), "not a witch werewolf");
        if (laddersGateApex()) {
            setLadder(p, SupernaturalRace.WEREWOLF, top(SupernaturalRace.WEREWOLF));
            setLadder(p, SupernaturalRace.WITCH, top(SupernaturalRace.WITCH) - 1);
            helper.assertTrue(offers(p, "alpha_primeval") && !offers(p, "ancestral_witch"), "only the werewolf rung is at the top: " + offered(p));
            setLadder(p, SupernaturalRace.WITCH, top(SupernaturalRace.WITCH));
        }
        helper.assertTrue(offers(p, "alpha_primeval") && offers(p, "ancestral_witch"), "not offered both apex races on a saint: " + offered(p));
        // a werewolf is on the Beastfolk line and the magic ladder stacks on it
        for (String id : new String[] {"tensura:spirit_beast", "tensura:divine_beast"}) {
            setRace(p, id);
            helper.assertTrue(offers(p, "alpha_primeval") && offers(p, "ancestral_witch"), "a witch werewolf on " + id + " is not offered both: " + offered(p));
        }
        setRace(p, "tensura:beast_lord");
        helper.assertTrue(!offersApex(p), "offered from the tier below: " + offered(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonerWerewolfOnSpiritBeastIsOfferedVoidSiphon(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.SIPHONER);
        rank(p, WolfState.Rank.ALPHA);
        setRace(p, "tensura:spirit_beast");
        helper.assertTrue(offers(p, "alpha_primeval") && offers(p, "void_siphon"), "a siphoner werewolf on Spirit Beast is not offered both: " + offered(p));
        helper.succeed();
    }

    private static double value(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
        return p.getAttributeValue(a);
    }

    /** The amount the True Alpha modifier adds before other modifiers (the wolf rank's multipliers scale the sum, so the value delta is larger). */
    private static double extra(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
        var modifier = p.getAttribute(a).getModifier(PrimevalBoost.ID);
        return modifier == null ? 0 : modifier.amount();
    }

    /** L-WOL-6: a True Alpha (not dormant) on alpha_primeval has the apex bonuses x1.5, an Alpha or anyone else does not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void trueAlphaOnAlphaPrimevalHasOneAndAHalfTheBonuses(GameTestHelper helper) {
        ServerPlayer alpha = werewolf(helper, Species.NONE);
        ServerPlayer truth = werewolf(helper, Species.NONE);
        setRace(alpha, NS + ":alpha_primeval");
        setRace(truth, NS + ":alpha_primeval");
        rank(alpha, WolfState.Rank.ALPHA);
        SpeciesLayer.sync(alpha);
        helper.assertTrue(!alpha.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "an Alpha got the True Alpha bonus");

        rank(truth, WolfState.Rank.TRUE_ALPHA);
        SpeciesLayer.sync(truth);
        helper.assertTrue(truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "no bonus for a True Alpha");
        // the extra half of the apex race's own bonuses (900 health, 25 attack, 0.03 speed), on top of what the True Alpha rank gives anyway
        helper.assertTrue(Math.abs(extra(truth, Attributes.MAX_HEALTH) - 450) < 1e-6, "health " + extra(truth, Attributes.MAX_HEALTH));
        helper.assertTrue(Math.abs(extra(truth, Attributes.ATTACK_DAMAGE) - 12.5) < 1e-6, "attack " + extra(truth, Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(extra(truth, Attributes.MOVEMENT_SPEED) - 0.015) < 1e-6, "speed " + extra(truth, Attributes.MOVEMENT_SPEED));
        helper.assertTrue(value(alpha, Attributes.MAX_HEALTH) - value(truth, Attributes.MAX_HEALTH) < -449, "the True Alpha is not stronger than the Alpha");

        // a dormant True Alpha (cured, or left the werewolves) loses it until the rejoin
        VampirismAPI.factionPlayerHandler(truth).leaveFaction(false);
        SpeciesLayer.sync(truth);
        helper.assertTrue(!truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "a dormant True Alpha kept the bonus");
        helper.assertTrue(BloodlineAccess.get(truth).wolf().rank() == WolfState.Rank.TRUE_ALPHA, "the rank was lost (R91)");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(truth).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not rejoin");
        SpeciesLayer.sync(truth);
        helper.assertTrue(truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "the bonus did not come back on the rejoin");

        // another race: no bonus; the apex race stays a race of the species
        setRace(truth, "tensura:human_saint");
        SpeciesLayer.sync(truth);
        helper.assertTrue(!truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "the bonus stayed on another race");
        setRace(truth, NS + ":alpha_primeval");
        SpeciesLayer.sync(truth);
        helper.assertTrue(truth.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(PrimevalBoost.ID)
                && truth.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(PrimevalBoost.ID), "attack and speed are not all boosted");

        // the modifier is saved with the player: with the part off, login strips it
        StalePartModifiers.strip(truth, EnumSet.noneOf(Part.class));
        helper.assertTrue(!truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "the bonus stayed with the part off");
        helper.succeed();
    }

    private static final String APEX = NS + ":alpha_primeval";

    /** Owner rule 5: an alpha_primeval who loses the spark (stolen, given away, cured) goes back to the race before; it returns with the spark. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anApexWolfWhoLosesTheSparkFallsBackAndTheRaceReturnsWithIt(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE); // on tensura:human_saint
        setRace(p, APEX);
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(APEX.equals(race(p)), "an Alpha with a spark is not on the apex race: " + race(p));
        helper.assertTrue("tensura:human_saint".equals(BloodlineAccess.get(p).wolf().preApex()), "the race before was not recorded: " + BloodlineAccess.get(p).wolf().preApex());

        rank(p, WolfState.Rank.OMEGA); // the spark was stolen or given away
        helper.assertTrue("tensura:human_saint".equals(race(p)), "the race did not fall back: " + race(p));
        helper.assertTrue(!net.ofatech.supernaturalbloodlines.shared.tensura.PrimevalAlpha.active(p), "the apex stayed active without a spark");

        rank(p, WolfState.Rank.ALPHA); // a spark again
        helper.assertTrue(APEX.equals(race(p)), "the apex race did not come back with the spark: " + race(p));
        helper.succeed();
    }

    /** A True Alpha who goes dormant (cured, out of the werewolves) falls back as well, and the apex returns on the rejoin. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDormantTrueAlphaFallsBackAndTheApexReturnsOnTheRejoin(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
        setRace(p, APEX);
        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(APEX.equals(race(p)), "control: not on the apex race");
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.assertTrue("tensura:human_saint".equals(race(p)), "the dormant True Alpha kept the apex race: " + race(p));
        helper.assertTrue(BloodlineAccess.get(p).wolf().rank() == WolfState.Rank.TRUE_ALPHA, "the rank was lost (R91)");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not rejoin");
        helper.assertTrue(APEX.equals(race(p)), "the apex race did not return on the rejoin: " + race(p));
        helper.succeed();
    }

    /** Only a change counts, and only the player's own evolution is theirs to keep: a wolf who never had a spark stays, and one who evolved on is not sent back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFallbackLeavesAWolfWhoNeverHeldASparkAndOneWhoEvolvedOn(GameTestHelper helper) {
        ServerPlayer never = werewolf(helper, Species.NONE);
        setRace(never, APEX);
        rank(never, WolfState.Rank.BETA);
        rank(never, WolfState.Rank.OMEGA);
        helper.assertTrue(APEX.equals(race(never)), "a wolf with no spark was sent back: " + race(never));

        ServerPlayer moved = werewolf(helper, Species.NONE);
        setRace(moved, APEX);
        rank(moved, WolfState.Rank.ALPHA);
        rank(moved, WolfState.Rank.OMEGA);
        setRace(moved, "tensura:elf"); // their own choice, after the fall
        rank(moved, WolfState.Rank.ALPHA);
        helper.assertTrue("tensura:elf".equals(race(moved)), "the race they chose was overwritten: " + race(moved));

        ServerPlayer legacy = werewolf(helper, Species.NONE);
        setRace(legacy, APEX);
        rank(legacy, WolfState.Rank.ALPHA);
        BloodlineAccess.set(legacy, BloodlineAccess.get(legacy).toBuilder().wolf(BloodlineAccess.get(legacy).wolf().toBuilder().preApex("").build()).build());
        rank(legacy, WolfState.Rank.OMEGA);
        helper.assertTrue("tensura:spirit_beast".equals(race(legacy)), "a wolf with no record went to " + race(legacy));
        helper.succeed();
    }

    /** SET_RACE fires before the change and a later listener can refuse it: a refused evolution must leave nothing recorded. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRefusedEvolutionIntoTheApexIsNotRecorded(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE); // on tensura:human_saint
        RaceEvents.SetRaceEvent refuse = (oldRace, entity, newRace, flag, changeable, message) ->
                entity == p && newRace != null && APEX.equals(newRace.getRaceId().toString()) ? EventResult.interruptFalse() : EventResult.pass();
        RaceEvents.SET_RACE.register(refuse);
        try {
            setRace(p, APEX);
            ApexFallback.settle(p);
            helper.assertTrue("tensura:human_saint".equals(race(p)), "the refusal did not hold: " + race(p));
            helper.assertTrue(BloodlineAccess.get(p).wolf().preApex().isEmpty(), "a refused evolution was recorded: " + BloodlineAccess.get(p).wolf().preApex());
        } finally {
            RaceEvents.SET_RACE.unregister(refuse);
        }
        setRace(p, APEX);
        ApexFallback.settle(p);
        helper.assertTrue("tensura:human_saint".equals(BloodlineAccess.get(p).wolf().preApex()), "an evolution that went through was not recorded: " + BloodlineAccess.get(p).wolf().preApex());
        helper.succeed();
    }

    /** Alpha and True Alpha reach the apex: a Beta (one who fell back, say) is not offered it, an Alpha is. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfIsOfferedTheApexOnlyWhileTheyHoldASpark(GameTestHelper helper) {
        if (!laddersGateApex()) {
            helper.succeed();
            return;
        }
        ServerPlayer p = werewolf(helper, Species.NONE);
        setLadder(p, SupernaturalRace.WEREWOLF, top(SupernaturalRace.WEREWOLF));
        rank(p, WolfState.Rank.BETA);
        helper.assertTrue(!offers(p, "alpha_primeval"), "a Beta was offered the apex: " + offered(p));
        rank(p, WolfState.Rank.ALPHA);
        helper.assertTrue(offers(p, "alpha_primeval"), "an Alpha was not offered it: " + offered(p));
        rank(p, WolfState.Rank.TRUE_ALPHA);
        helper.assertTrue(offers(p, "alpha_primeval"), "a True Alpha was not offered it: " + offered(p));
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.assertTrue(!offersApex(p), "a dormant True Alpha was offered an apex: " + offered(p));
        helper.succeed();
    }

    /** Owner rule 4: the True Alpha's full-spark bonus stacks with the apex x1.5 (the Primeval boost is a separate modifier on top). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTrueAlphaFullSparkBonusStacksWithTheApexBoost(GameTestHelper helper) {
        ServerPlayer truth = werewolf(helper, Species.NONE);
        setRace(truth, APEX);
        rank(truth, WolfState.Rank.TRUE_ALPHA);
        SpeciesLayer.sync(truth);
        double attack = value(truth, Attributes.ATTACK_DAMAGE);
        helper.assertTrue(truth.getAttribute(Attributes.MAX_HEALTH).hasModifier(PrimevalBoost.ID), "the apex x1.5 is not on");
        // the rank's own bonus (+20% attack x1.1 x1.4 at the default cap of 5 sparks) is a separate modifier from the apex boost: both are there
        helper.assertTrue(Math.abs(value(truth, Attributes.MAX_HEALTH) - (20 + 900 + 450 + 6.16)) < 0.01, "health " + value(truth, Attributes.MAX_HEALTH));
        helper.assertTrue(attack > 0, "no attack");
        helper.succeed();
    }
}
