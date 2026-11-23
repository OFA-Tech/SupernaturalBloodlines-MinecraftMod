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
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchKnowsTheRitesFromBeingOneAndAHumanSeesThemSealed(GameTestHelper helper) {
        if (type(helper, RITE) == null) {
            helper.succeed();
            return;
        }
        RecordingPlayer witch = RecordingPlayer.create(helper);
        RecordingPlayer human = RecordingPlayer.create(helper);
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
        if (type(helper, RITE) == null) {
            helper.succeed();
            return;
        }
        boolean ascension = ModList.get().isLoaded("ascension");
        helper.assertTrue((type(helper, "evolve_deus_sanguis") != null) == ascension, "the rite is registered: " + (type(helper, "evolve_deus_sanguis") != null) + ", Ascension is loaded: " + ascension);
        helper.assertTrue(advancementExists(helper, RiteAdvancements.known("evolve_deus_sanguis")) == ascension, "the known advancement follows Ascension");
        helper.assertTrue(advancementExists(helper, RiteAdvancements.sealed("evolve_deus_sanguis")) == ascension, "the sealed advancement follows Ascension");
        if (!ascension) {
            RecordingPlayer vampire = RecordingPlayer.create(helper);
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

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theEvolutionRitesAreInTheRegistryExactlyWhenThePartIs(GameTestHelper helper) {
        boolean there = type(helper, RITE) != null;
        helper.assertTrue(there == (type(helper, "mastery_common") != null), "the evolution and the mastery rites disagree");
        helper.succeed();
    }
}
