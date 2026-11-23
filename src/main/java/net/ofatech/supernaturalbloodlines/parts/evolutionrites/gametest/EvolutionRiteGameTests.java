package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.performer;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.core.evolution.GrantForm;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.EvolutionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/** The evolution rite on the real rite files (plan 19a, tasks 4 and 7): Sage to Great Sage, and the vampire road to Deus Sanguis where Ascension is there. */
@PrefixGameTestTemplate(false)
public final class EvolutionRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "evolve_great_sage";
    private static final String SAGE = "tensura:sage";
    private static final String GREAT_SAGE = "tensura:great_sage";
    private static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    private static final String ALREADY = "supernaturalbloodlines.refusal.evolution.already";
    private static final double EP = 5_000_000;

    private EvolutionRiteGameTests() {
    }

    private static boolean noRite(GameTestHelper helper) {
        if (type(helper, RITE) == null) {
            helper.succeed(); // the rite's data needs the part
            return true;
        }
        return false;
    }

    private static List<net.minecraft.world.item.ItemStack> items() {
        return stacks("minecraft:amethyst_shard", 4, "minecraft:ender_eye", 2, "enchanted:spirit_of_otherwhere", 1);
    }

    /** What Tensura itself takes from base Max EP for learning Great Sage with no free switch (a control learn on a player of the same EP). */
    private static double tensuraPrice(GameTestHelper helper) {
        RecordingPlayer control = onTheCircle(helper, false, EP);
        try {
            ManasSkill greatSage = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GREAT_SAGE));
            helper.assertTrue(io.github.manasmods.tensura.ability.SkillHelper.learnSkill(control, greatSage), "the control learn failed");
            return EP - EnergyHelper.getBaseMaxEP(control);
        } finally {
            EvoTestSupport.forgetAll(control);
            remove(helper, control);
            BloodlineAccess.set(control, BloodlineState.NONE);
        }
    }

    private static void clean(GameTestHelper helper, net.minecraft.server.level.ServerPlayer... ps) {
        clearDropped(helper);
        for (var p : ps) {
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        EvolutionRiteFactory.beforeGrant = () -> { };
        EvolutionRitesConfig.override("rites_enabled", null);
        EvolutionRitesConfig.override("disabled_recipes", null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteDataLoads(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        var type = type(helper, RITE);
        helper.assertTrue(type.getPower() == 9000, "power is " + type.getPower());
        helper.assertTrue(type.getItems().size() == 3, "the ingredients: " + type.getItems());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatSageIsGrantedToAConsentingTargetOnAFullMoon(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(has(target, GREAT_SAGE), "no Great Sage; the target heard " + target.heard() + ", the caster " + caster.heard());
            helper.assertTrue(has(target, SAGE), "Sage was taken");
            helper.assertTrue(target.received(EvolutionRiteFactory.DONE), "the target was not told: " + target.heard());
            helper.assertTrue(caster.received(EvolutionRiteFactory.DONE_CASTER), "the performer was not told: " + caster.heard());
            double price = tensuraPrice(helper);
            helper.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(target) - (EP - 100_000 - price)) < 1, "base max EP is " + EnergyHelper.getBaseMaxEP(target) + ", Tensura's price is " + price);
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 0, "a good rite gave the ingredients back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noGrantOnAnOrdinaryNight(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            atNight(helper, false, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(!has(target, GREAT_SAGE), "Great Sage on an ordinary night");
            helper.assertTrue(caster.received("supernaturalbloodlines.rite.needs_full_moon"), "the performer heard " + caster.heard());
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAWitchTypeWorksIt(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.NONE);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(!has(target, GREAT_SAGE), "a non-witch evolved someone");
            helper.assertTrue(caster.received("supernaturalbloodlines.refusal.evolution.not_witch"), "the performer heard " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetWhoDoesNotSneakIsAskedAndNothingChanges(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            give(target, SAGE, 500);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(!has(target, GREAT_SAGE), "an unwilling target was evolved");
            helper.assertTrue(target.receivedOnActionBar("supernaturalbloodlines.rite.sneak_to_evolve"), "the target was not asked: " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** E5: the performer hears one thing whatever the target is; the target hears why. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePerformerHearsTheSameWhateverTheTargetIs(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        String[] expectedForTarget = {NOT_READY, ALREADY, NOT_READY, NOT_READY};
        for (int kind = 0; kind < 4; kind++) {
            for (boolean sneaking : new boolean[] {false, true}) {
                RecordingPlayer caster = performer(helper, Species.WITCH);
                RecordingPlayer target = onTheCircle(helper, sneaking, EP);
                try {
                    switch (kind) {
                        case 0 -> { } // holds nothing
                        case 1 -> give(target, GREAT_SAGE, 0); // already has it
                        case 2 -> give(target, SAGE, 10); // Sage, not mastered
                        default -> {
                            give(target, SAGE, 500);
                            EnergyHelper.setBaseMaxEP(target, 10); // cannot pay
                        }
                    }
                    clearDropped(helper);
                    atNight(helper, true, () -> start(helper, RITE, caster, items()));
                    helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "kind " + kind + " sneaking " + sneaking + ": the performer heard " + caster.heard());
                    helper.assertTrue(!(kind != 1 && has(target, GREAT_SAGE)), "kind " + kind + " was evolved");
                    if (sneaking) {
                        helper.assertTrue(target.received(expectedForTarget[kind]), "kind " + kind + ": the target heard " + target.heard());
                    }
                    helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "kind " + kind + ": the ingredients were not given back");
                } finally {
                    clean(helper, caster, target);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSelfCastIsToldPlainly(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer both = onTheCircle(helper, false, EP);
        BloodlineAccess.set(both, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        try {
            atNight(helper, true, () -> start(helper, RITE, both, items()));
            helper.assertTrue(both.received(NOT_READY), "a witch with no Sage heard " + both.heard());
            clearDropped(helper);
            give(both, SAGE, 500);
            atNight(helper, true, () -> start(helper, RITE, both, items()));
            helper.assertTrue(has(both, GREAT_SAGE), "a witch who is her own target was not evolved: " + both.heard());
        } finally {
            clean(helper, both);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noCentreAndNoCasterCancelsSilently(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper); // not in the level, nobody on the circle
        BloodlineAccess.set(caster, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        try {
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(caster.heard().isEmpty(), "the offline performer was told " + caster.heard());
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clearDropped(helper);
            BloodlineAccess.set(caster, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillGivenInTheMeantimeIsSeenAtGrantTime(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            EvolutionRiteFactory.beforeGrant = () -> give(target, GREAT_SAGE, -100); // Skill Obtain, a book: another mod gave it
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(target.received(ALREADY), "the target heard " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP was spent: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    private static EvolutionRecipe fixture(List<String> consumes) {
        return new EvolutionRecipe("fixture", Tier.UNIQUE, GREAT_SAGE, GrantForm.DEFAULT, consumes, List.of(), "tensura", "own.sage_great_sage", Set.of(), 1000);
    }

    /** A learn that a listener vetoes costs nothing: nobody loses a skill or EP. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVetoedLearnConsumesNothing(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, true, EP);
        ManasSkill greatSage = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GREAT_SAGE));
        var listener = (io.github.manasmods.manascore.skill.api.SkillEvents.UnlockSkillEvent) (inst, entity, msg) ->
                inst.getSkillId().equals(greatSage.getRegistryName()) ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass();
        io.github.manasmods.manascore.skill.api.SkillEvents.UNLOCK_SKILL.register(listener);
        try {
            give(target, SAGE, 500);
            var result = EvolutionRiteFactory.grant(target, fixture(List.of(SAGE)), greatSage);
            helper.assertTrue(result == EvolutionRiteFactory.Grant.VETOED, "the result was " + result);
            helper.assertTrue(has(target, SAGE), "Sage was forgotten although the learn was vetoed");
            helper.assertTrue(!has(target, GREAT_SAGE), "a vetoed skill was learned");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP was spent: " + EnergyHelper.getBaseMaxEP(target));
        } finally {
            io.github.manasmods.manascore.skill.api.SkillEvents.UNLOCK_SKILL.unregister(listener);
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void consumesAreForgottenOnlyAfterTheLearn(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, true, EP);
        ManasSkill greatSage = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GREAT_SAGE));
        try {
            give(target, SAGE, 500);
            var result = EvolutionRiteFactory.grant(target, fixture(List.of(SAGE)), greatSage);
            helper.assertTrue(result == EvolutionRiteFactory.Grant.DONE, "the result was " + result);
            helper.assertTrue(has(target, GREAT_SAGE), "no Great Sage");
            helper.assertTrue(!has(target, SAGE), "Sage, which the recipe consumes, stayed");
            double price = tensuraPrice(helper);
            helper.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(target) - (EP - 1000 - price)) < 1, "EP is " + EnergyHelper.getBaseMaxEP(target) + ", Tensura's price is " + price);
        } finally {
            clean(helper, target);
        }
        helper.succeed();
    }

    /** Each rite file's recipe tier is the tier its skill really has: the night follows it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void tierMatchesTheSkillType(GameTestHelper helper) throws Exception {
        for (String rite : List.of("evolve_great_sage", "evolve_deus_sanguis")) {
            try (var in = EvolutionRiteGameTests.class.getResourceAsStream("/data/supernaturalbloodlines/enchanted/circle_magic/rite/" + rite + ".json")) {
                var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("factory");
                EvolutionRecipe recipe = EvolutionRiteFactory.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow().recipe();
                var skill = EvoSkills.skill(recipe.grants());
                if (skill.isEmpty()) {
                    continue; // its addon is not here
                }
                helper.assertTrue(EvoSkills.tierOf(skill.get()) == recipe.tier(), rite + ": the recipe says " + recipe.tier() + ", the skill is " + EvoSkills.tierOf(skill.get()));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unknownProbeOrSkillMakesTheRecipeUnavailable(GameTestHelper helper) {
        EvolutionRecipe noProbe = new EvolutionRecipe("x", Tier.UNIQUE, GREAT_SAGE, GrantForm.DEFAULT, List.of(), List.of(), "tensura", "no.such", Set.of(), 0);
        EvolutionRecipe noSkill = new EvolutionRecipe("y", Tier.UNIQUE, "nomod:nothing", GrantForm.DEFAULT, List.of(), List.of(), "tensura", "own.sage_great_sage", Set.of(), 0);
        EvolutionRecipe noMod = new EvolutionRecipe("z", Tier.UNIQUE, GREAT_SAGE, GrantForm.DEFAULT, List.of(), List.of(), "nomod", "own.sage_great_sage", Set.of(), 0);
        EvolutionRecipe fine = fixture(List.of());
        helper.assertTrue(!Recipes.available(noProbe, id -> true), "an unknown probe is available");
        helper.assertTrue(!Recipes.available(noSkill, id -> true), "an unknown skill is available");
        helper.assertTrue(!Recipes.available(noMod, id -> id.equals("tensura")), "a recipe of a missing mod is available");
        helper.assertTrue(Recipes.available(fine, id -> true), "a good recipe is unavailable");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchesCloseTheRite(GameTestHelper helper) {
        if (noRite(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            EvolutionRitesConfig.override("rites_enabled", false);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(!has(target, GREAT_SAGE) && caster.received("supernaturalbloodlines.refusal.evolution.closed"), "off: " + caster.heard());
            EvolutionRitesConfig.override("rites_enabled", null);
            clearDropped(helper);
            EvolutionRitesConfig.override("disabled_recipes", List.of("great_sage"));
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(!has(target, GREAT_SAGE), "a disabled recipe worked");
            helper.assertTrue(caster.count("supernaturalbloodlines.refusal.evolution.closed") == 2, "the performer heard " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void performingTeachesThePerformer(GameTestHelper helper) {
        // performing is signalled by the supernatural part's hook on Rite.start: with that part off nothing teaches the performer, by design
        if (noRite(helper)) {
            return;
        }
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        // real advancements: the rite unlocks are advancements, and a plain fake player's do nothing
        LineTestPlayer caster = LineTestPlayer.createWithAdvancements(helper);
        BloodlineAccess.set(caster, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        caster.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
        helper.getLevel().addNewPlayer(caster);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            RiteUnlocks.reconcile(caster);
            RiteUnlocks.forget(caster, RITE); // a witch knows it from being one: take it away, so only performing can teach it
            helper.assertTrue(!RiteUnlocks.known(caster).contains(RITE), "could not forget the rite");
            give(target, SAGE, 500);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(has(target, GREAT_SAGE), "the rite did not work");
            helper.assertTrue(RiteUnlocks.known(caster).contains(RITE), "the performer did not learn the rite");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }
}
