package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.GreatSageProbe;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/** The evolution rites in the book's unlock table: sealed until known, unlocked by kind, and absent with their addon or part (spec 10). */
@PrefixGameTestTemplate(false)
public final class EvolutionUnlockGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "evolve_great_sage";

    private EvolutionUnlockGameTests() {
    }

    private static boolean advancementExists(GameTestHelper helper, String id) {
        return helper.getLevel().getServer().getAdvancements().get(ResourceLocation.parse(id)) != null;
    }

    /** The rite kinds read the player's Tensura race through the seam species_tensura installs (19d): a linked race adds its kind, any other adds nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLinkedTensuraRaceAddsItsRiteKind(GameTestHelper helper) {
        var p = net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer.createWithAdvancements(helper);
        String[] race = {"ascension:blood_noble"};
        var old = net.ofatech.supernaturalbloodlines.shared.species.TensuraRaceId.swap(who -> who == p ? race[0] : "");
        try {
            var kinds = RiteUnlocks.kindsOf(p);
            helper.assertTrue(kinds.contains(net.ofatech.supernaturalbloodlines.core.rite.RiteKind.VAMPIRE), "a Bloodfiend is not vampire-kind: " + kinds);
            race[0] = "mysticism:magic_spirit_wolf";
            kinds = RiteUnlocks.kindsOf(p);
            helper.assertTrue(kinds.contains(net.ofatech.supernaturalbloodlines.core.rite.RiteKind.WEREWOLF), "a spirit wolf is not werewolf-kind: " + kinds);
            race[0] = "tensura:human";
            helper.assertTrue(RiteUnlocks.kindsOf(p).isEmpty(), "a plain Tensura race added a kind: " + RiteUnlocks.kindsOf(p));
        } finally {
            net.ofatech.supernaturalbloodlines.shared.species.TensuraRaceId.install(old);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchKnowsTheRitesFromBeingOneAndAHumanSeesThemSealed(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, RITE)) {
            return;
        }
        var witch = net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer.createWithAdvancements(helper);
        var human = net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer.createWithAdvancements(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            RiteUnlocks.reconcile(witch);
            RiteUnlocks.reconcile(human);
            for (String rite : new String[] {RITE, "mastery_common", "mastery_extra", "mastery_unique", "mastery_ultimate"}) {
                helper.assertTrue(RiteUnlocks.isKnown(witch, rite) && RiteUnlocks.done(witch, RiteAdvancements.known(rite)), "a witch does not know " + rite);
                helper.assertTrue(!RiteUnlocks.done(witch, RiteAdvancements.sealed(rite)), rite + " is sealed for a witch");
                helper.assertTrue(!RiteUnlocks.done(human, RiteAdvancements.known(rite)) && RiteUnlocks.done(human, RiteAdvancements.sealed(rite)), rite + " is not sealed for a human");
            }
        } finally {
            BloodlineAccess.set(witch, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** E10 / spec 10.3: a recipe whose addon is not loaded has neither its rite nor its book advancements, so nothing is known or sealed for it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void deusSanguisExistsOnlyWhereAscensionIs(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, RITE)) {
            return;
        }
        boolean ascension = ModList.get().isLoaded("ascension");
        helper.assertTrue((type(helper, "evolve_deus_sanguis") != null) == ascension, "the rite is registered: " + (type(helper, "evolve_deus_sanguis") != null) + ", Ascension is loaded: " + ascension);
        helper.assertTrue(advancementExists(helper, RiteAdvancements.known("evolve_deus_sanguis")) == ascension, "the known advancement follows Ascension");
        helper.assertTrue(advancementExists(helper, RiteAdvancements.sealed("evolve_deus_sanguis")) == ascension, "the sealed advancement follows Ascension");
        for (String rite : new String[] {RITE, "mastery_common", "mastery_extra", "mastery_unique", "mastery_ultimate", "evolve_deus_sanguis", "evolve_raphael"}) {
            helper.assertTrue(advancementExists(helper, RiteAdvancements.known(rite)) == (type(helper, rite) != null), rite + ": its advancements and its rite disagree");
        }
        if (!ascension) {
            var vampire = net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer.createWithAdvancements(helper);
            try {
                RiteUnlocks.reconcile(vampire);
                helper.assertTrue(!RiteUnlocks.known(vampire).contains("evolve_deus_sanguis"), "a rite of a missing addon is known");
                helper.assertTrue(!RiteUnlocks.done(vampire, RiteAdvancements.sealed("evolve_deus_sanguis")), "a rite of a missing addon is sealed");
            } finally {
                BloodlineAccess.set(vampire, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    /** Plan 19b: the dormant Raphael rite exists only where Great Sage is, like Deus Sanguis with Ascension. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void raphaelExistsOnlyWhereGreatSageIs(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, RITE)) {
            return;
        }
        boolean sage = ModList.get().isLoaded("greatsage");
        helper.assertTrue((type(helper, "evolve_raphael") != null) == sage, "the rite is registered: " + (type(helper, "evolve_raphael") != null) + ", Great Sage is loaded: " + sage);
        helper.assertTrue(advancementExists(helper, RiteAdvancements.known("evolve_raphael")) == sage, "the known advancement follows Great Sage");
        helper.assertTrue(advancementExists(helper, RiteAdvancements.sealed("evolve_raphael")) == sage, "the sealed advancement follows Great Sage");
        helper.succeed();
    }

    /** The book shows neither entry while Great Sage's own road to Raphael is closed, and shows the right one when it opens, with no restart. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClosedRoadHidesTheRite(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, RITE)) {
            return;
        }
        if (!ModList.get().isLoaded("greatsage")) {
            helper.succeed();
            return;
        }
        var witch = net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer.createWithAdvancements(helper);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GreatSageProbe.roadHook = () -> false;
            RiteUnlocks.reconcile(witch);
            helper.assertTrue(!RiteUnlocks.present(witch, "evolve_raphael"), "a rite whose road is closed is present");
            helper.assertTrue(!RiteUnlocks.presentRites(witch).contains("evolve_raphael"), "it is listed");
            helper.assertTrue(!RiteUnlocks.done(witch, RiteAdvancements.known("evolve_raphael")) && !RiteUnlocks.done(witch, RiteAdvancements.sealed("evolve_raphael")),
                    "the book would show an entry for a road that is closed");
            GreatSageProbe.roadHook = () -> true;
            RiteUnlocks.reconcile(witch);
            helper.assertTrue(RiteUnlocks.present(witch, "evolve_raphael"), "an open road is not present");
            // an Ultimate is lore-gated (E7): the open road shows the sealed entry until Finn's Notes teach it
            helper.assertTrue(!RiteUnlocks.done(witch, RiteAdvancements.known("evolve_raphael")) && RiteUnlocks.done(witch, RiteAdvancements.sealed("evolve_raphael")),
                    "the open road does not show the sealed entry, or a witch knows the rite without Finn's Notes");
        } finally {
            GreatSageProbe.roadHook = null;
            BloodlineAccess.set(witch, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theEvolutionRitesAreInTheRegistryExactlyWhenThePartIs(GameTestHelper helper) {
        boolean there = type(helper, RITE) != null;
        helper.assertTrue(there == (type(helper, "mastery_common") != null), "the evolution and the mastery rites disagree");
        helper.succeed();
    }
}
