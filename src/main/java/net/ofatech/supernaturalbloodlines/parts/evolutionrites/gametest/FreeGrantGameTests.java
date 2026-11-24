package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.performer;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.evolution.EvolutionRecipe;
import net.ofatech.supernaturalbloodlines.core.evolution.GrantForm;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoGrant;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AddonProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.EvolutionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * The engine's parts that plan 19b adds, on Tensura skills only (no addon jar needed): the free grant form, a probe that names another skill to grant and
 * skills that count as held, and a retired grant. The merge itself is {@link RaphaelMergeGameTests}.
 */
@PrefixGameTestTemplate(false)
public final class FreeGrantGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "evolve_great_sage";
    private static final String SAGE = "tensura:sage";
    private static final String GREAT_SAGE = "tensura:great_sage";
    private static final String DEGENERATE = "tensura:degenerate";
    private static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    private static final double EP = 5_000_000;

    private FreeGrantGameTests() {
    }

    private static ManasSkill skill(String id) {
        return SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
    }

    private static EvolutionRecipe recipe(GrantForm form, String grants) {
        return new EvolutionRecipe("x", Tier.UNIQUE, grants, form, List.of(), List.of(), "tensura", "own.sage_great_sage", Set.of(), 0);
    }

    private static void clean(GameTestHelper helper, ServerPlayer... ps) {
        clearDropped(helper);
        for (ServerPlayer p : ps) {
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        EvoSkills.retiredHook = null;
    }

    /** A probe that is only what the test says it is. */
    private static AddonProbe probe(String override, String... alsoHolds) {
        return new AddonProbe() {
            @Override
            public Optional<String> missingHook() {
                return Optional.empty();
            }

            @Override
            public boolean roadOpen() {
                return true;
            }

            @Override
            public boolean satisfied(ServerPlayer target, EvolutionRecipe recipe) {
                return true;
            }

            @Override
            public Optional<String> grantOverride() {
                return Optional.ofNullable(override);
            }

            @Override
            public List<String> alsoHolds() {
                return List.of(alsoHolds);
            }
        };
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFreeGrantCarriesBothTagsAndChargesNothing(GameTestHelper helper) {
        if (!EvoTestSupport.learnOpen(helper, GREAT_SAGE)) {
            helper.succeed(); // an installed addon closes this learn by its own rule (19a)
            return;
        }
        RecordingPlayer control = onTheCircle(helper, false, EP);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            helper.assertTrue(SkillHelper.learnSkill(control, skill(GREAT_SAGE)), "the control learn failed");
            double price = EP - EnergyHelper.getBaseMaxEP(control);
            helper.assertTrue(price > 0, "control: a plain learn charged nothing, so this test would prove nothing: " + price);
            helper.assertTrue(EvoGrant.learnFree(target, skill(GREAT_SAGE)), "the free learn was refused");
            ManasSkillInstance inst = SkillAPI.getSkillsFrom(target).getSkill(skill(GREAT_SAGE)).orElseThrow();
            helper.assertTrue(inst instanceof TensuraSkillInstance, "not a Tensura instance: " + inst.getClass());
            var tag = ((TensuraSkillInstance) inst).getOrCreateTag();
            helper.assertTrue(tag.getBoolean("NoMagiculeCost") && tag.getBoolean("GiftedInstance"), "the instance does not carry both tags: " + tag);
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "the free learn charged " + (EP - EnergyHelper.getBaseMaxEP(target)));
        } finally {
            clean(helper, control, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFreeLearnThatIsVetoedReturnsFalseAndLeavesNothing(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, false, EP);
        SkillEvents.UnlockSkillEvent veto = (inst, who, message) -> who == target && inst.getSkillId().equals(ResourceLocation.parse(DEGENERATE)) ? EventResult.interruptFalse() : EventResult.pass();
        SkillEvents.UNLOCK_SKILL.register(veto);
        try {
            helper.assertTrue(!EvoGrant.learnFree(target, skill(DEGENERATE)), "a vetoed free learn reported success");
            helper.assertTrue(!has(target, DEGENERATE), "a vetoed free learn left the skill");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP changed");
        } finally {
            SkillEvents.UNLOCK_SKILL.unregister(veto);
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFreeRecipeHasNoTensuraPriceInItsEpCheck(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            ManasSkill great = skill(GREAT_SAGE);
            double plain = EvolutionRiteFactory.priceOf(recipe(GrantForm.DEFAULT, GREAT_SAGE), target, great);
            helper.assertTrue(plain == EvoSkills.price(target, great), "the default form is not Tensura's price: " + plain);
            helper.assertTrue(EvolutionRiteFactory.priceOf(recipe(GrantForm.FREE, GREAT_SAGE), target, great) == 0, "a free grant has a price");
        } finally {
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFreeGrantStillForgetsWhatTheRecipeConsumesOnlyAfterTheLearn(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, false, EP);
        EvolutionRecipe free = new EvolutionRecipe("x", Tier.UNIQUE, GREAT_SAGE, GrantForm.FREE, List.of(SAGE), List.of(), "tensura", "own.sage_great_sage", Set.of(), 0);
        SkillEvents.UnlockSkillEvent veto = (inst, who, message) -> who == target && inst.getSkillId().equals(ResourceLocation.parse(GREAT_SAGE)) ? EventResult.interruptFalse() : EventResult.pass();
        try {
            give(target, SAGE, 500);
            SkillEvents.UNLOCK_SKILL.register(veto);
            helper.assertTrue(EvolutionRiteFactory.grant(target, free, skill(GREAT_SAGE)) == EvolutionRiteFactory.Grant.VETOED, "a vetoed free grant was not VETOED");
            helper.assertTrue(has(target, SAGE), "a vetoed free grant consumed Sage");
            SkillEvents.UNLOCK_SKILL.unregister(veto);
            helper.assertTrue(EvolutionRiteFactory.grant(target, free, skill(GREAT_SAGE)) == EvolutionRiteFactory.Grant.DONE, "the free grant did not go through");
            helper.assertTrue(has(target, GREAT_SAGE) && !has(target, SAGE), "after a free grant: great sage " + has(target, GREAT_SAGE) + ", sage " + has(target, SAGE));
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "the free grant charged EP");
        } finally {
            SkillEvents.UNLOCK_SKILL.unregister(veto);
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGrantOverrideReplacesTheGrantedSkill(GameTestHelper helper) {
        EvolutionRecipe r = recipe(GrantForm.FREE, GREAT_SAGE);
        helper.assertTrue(EvolutionRiteFactory.resolveGrant(r, Optional.empty()).orElseThrow() == skill(GREAT_SAGE), "no probe: the recipe's own skill");
        helper.assertTrue(EvolutionRiteFactory.resolveGrant(r, Optional.of(probe(null))).orElseThrow() == skill(GREAT_SAGE), "a probe with no override: the recipe's own skill");
        helper.assertTrue(EvolutionRiteFactory.resolveGrant(r, Optional.of(probe(SAGE))).orElseThrow() == skill(SAGE), "the override is not what is granted");
        helper.assertTrue(EvolutionRiteFactory.resolveGrant(r, Optional.of(probe("tensura:there_is_no_such_skill"))).isEmpty(), "an override that does not exist must make the grant absent, not fall back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillOfTheAlsoHoldsListCountsAsHeld(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            Optional<ManasSkill> grant = Optional.of(skill(GREAT_SAGE));
            helper.assertTrue(!EvolutionRiteFactory.holdsGrant(target, Optional.of(probe(null, SAGE)), grant), "nothing held, but held");
            give(target, SAGE, 10);
            helper.assertTrue(EvolutionRiteFactory.holdsGrant(target, Optional.of(probe(null, SAGE)), grant), "a skill of the also-holds list was not counted");
            helper.assertTrue(!EvolutionRiteFactory.holdsGrant(target, Optional.of(probe(null)), grant), "Sage counted without being in the list");
            helper.assertTrue(!EvolutionRiteFactory.holdsGrant(target, Optional.empty(), grant), "no probe, and Sage counted");
            give(target, GREAT_SAGE, 10);
            helper.assertTrue(EvolutionRiteFactory.holdsGrant(target, Optional.empty(), grant), "the grant itself held, but not held");
        } finally {
            clean(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRetiredGrantIsRefusedAsNotReady(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, RITE)) {
            return;
        }
        if (!EvoTestSupport.learnOpen(helper, GREAT_SAGE)) {
            helper.succeed(); // an addon closes the learn: the refusal path of that is 19a's test
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, SAGE, 500);
            EvoSkills.retiredHook = s -> s == skill(GREAT_SAGE);
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, RITE, caster, stacks("minecraft:amethyst_shard", 4, "minecraft:ender_eye", 2, "enchanted:spirit_of_otherwhere", 1)));
            helper.assertTrue(!has(target, GREAT_SAGE), "a retired skill was granted");
            helper.assertTrue(has(target, SAGE), "Sage was taken");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP changed: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(target.received(NOT_READY), "the target heard " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(dropped(helper, "minecraft:ender_eye") == 2, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }
}
