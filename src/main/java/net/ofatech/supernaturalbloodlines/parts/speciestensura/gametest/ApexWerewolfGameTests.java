package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
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

    /** D1 and D2: a plain werewolf is offered {@code alpha_primeval} from a saint of the human-like lines and from Spirit Beast on Beastfolk. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void plainWerewolfIsOfferedAlphaPrimeval(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
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
        // the extra half of the apex race's own bonuses (1000 health, 25 attack, 0.03 speed), on top of what the True Alpha rank gives anyway
        helper.assertTrue(Math.abs(extra(truth, Attributes.MAX_HEALTH) - 500) < 1e-6, "health " + extra(truth, Attributes.MAX_HEALTH));
        helper.assertTrue(Math.abs(extra(truth, Attributes.ATTACK_DAMAGE) - 12.5) < 1e-6, "attack " + extra(truth, Attributes.ATTACK_DAMAGE));
        helper.assertTrue(Math.abs(extra(truth, Attributes.MOVEMENT_SPEED) - 0.015) < 1e-6, "speed " + extra(truth, Attributes.MOVEMENT_SPEED));
        helper.assertTrue(value(alpha, Attributes.MAX_HEALTH) - value(truth, Attributes.MAX_HEALTH) < -499, "the True Alpha is not stronger than the Alpha");

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
}
