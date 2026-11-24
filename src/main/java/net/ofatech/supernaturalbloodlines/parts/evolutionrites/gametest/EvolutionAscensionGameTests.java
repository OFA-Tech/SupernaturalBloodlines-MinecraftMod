package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atEclipse;
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
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.AscensionProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.EvolutionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * The Rite of the Blood Sovereign against the real Ascension (plan 19c): Deus Sanguis to Blood Sovereign at a lunar eclipse, with Ascension's own requirements.
 * Every test skips itself when Ascension is not on the classpath (CI never has it; {@code -PaddonJars} does) and, where an installed addon closes Tensura's own
 * learn of the skill (Mysticism's Soul Energy price), proves only the refusals.
 */
@PrefixGameTestTemplate(false)
public final class EvolutionAscensionGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "evolve_blood_sovereign";
    private static final String DS = "ascension:deus_sanguis";
    private static final String BS = "ascension:blood_sovereign";
    private static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    private static final String ALREADY = "supernaturalbloodlines.refusal.evolution.already";
    private static final String CLOSED = "supernaturalbloodlines.refusal.evolution.closed";
    private static final String NOT_WITCH = "supernaturalbloodlines.refusal.evolution.not_witch";
    private static final String EP = "minecraft:nether_star";
    private static final String CATALYST = "ascension:ultimate_catalyst";
    private static final double RICH = 20_000_000;

    private EvolutionAscensionGameTests() {
    }

    /** True when the test has nothing to run: no Ascension (the rite is then absent, checked by the unlock test), or the part is off. */
    private static boolean skipped(GameTestHelper helper) {
        if (!ModList.get().isLoaded("ascension")) {
            helper.assertTrue(type(helper, RITE) == null, "the rite exists without Ascension");
            helper.succeed();
            return true;
        }
        return EvoTestSupport.skipped(helper, RITE);
    }

    private static List<ItemStack> items() {
        return stacks(CATALYST, 1, EP, 1, "enchanted:spirit_of_otherwhere", 1);
    }

    /** A target who meets every requirement of the road: Deus Sanguis mastered, a true Hero, rich in EP. */
    private static RecordingPlayer ready(GameTestHelper helper, double baseEp) {
        RecordingPlayer t = onTheCircle(helper, true, baseEp);
        var inst = give(t, DS, 0);
        inst.setMastery(inst.getMaxMastery());
        TensuraStorages.getExistenceFrom(t).setTrueHero(true);
        return t;
    }

    private static void clean(GameTestHelper helper, ServerPlayer... ps) {
        clearDropped(helper);
        for (var p : ps) {
            var existence = TensuraStorages.getExistenceFrom(p);
            if (existence != null) {
                existence.setTrueHero(false);
                existence.setTrueDemonLord(false);
            }
            p.getPersistentData().remove(AscensionProbe.LAST_AWAKENING_TAG);
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        EvolutionRiteFactory.beforeGrant = () -> { };
        EvolutionRitesConfig.override("ultimate_rites_enabled", null);
        EvolutionRitesConfig.override("disabled_recipes", null);
    }

    private static boolean learnClosed(GameTestHelper helper) {
        return !EvoTestSupport.learnOpen(helper, BS);
    }

    private static void eclipse(GameTestHelper helper, RecordingPlayer caster) {
        atEclipse(helper, () -> start(helper, RITE, caster, items()));
    }

    /** A refused rite changed nothing: no Blood Sovereign, Deus Sanguis kept, the ingredients back, the EP as it was, no awakening mark. */
    private static void refusedNothingTaken(GameTestHelper helper, RecordingPlayer target, double baseEp) {
        helper.assertTrue(!has(target, BS), "Blood Sovereign was granted: " + target.heard());
        helper.assertTrue(has(target, DS), "Deus Sanguis was taken");
        helper.assertTrue(dropped(helper, CATALYST) == 1 && dropped(helper, EP) == 1, "the ingredients were not given back");
        helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == baseEp, "EP changed: " + EnergyHelper.getBaseMaxEP(target));
        helper.assertTrue(!target.getPersistentData().contains(AscensionProbe.LAST_AWAKENING_TAG), "the awakening mark was written");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteDataLoads(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        var type = type(helper, RITE);
        helper.assertTrue(type != null && type.getPower() == 20000, "the rite did not load: " + type);
        helper.assertTrue(type.getItems().size() == 3, "the ingredients: " + type.getItems());
        helper.assertTrue(EvoSkills.skill(BS).isPresent() && EvoSkills.skill(DS).isPresent(), "Ascension's skills are not registered");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodSovereignIsGrantedAtAnEclipse(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        if (learnClosed(helper)) {
            helper.succeed(); // an installed addon closes the learn: the refusals below prove that case
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            long before = helper.getLevel().getGameTime();
            double price = EvoSkills.price(target, SkillAPI.getSkillRegistry().get(ResourceLocation.parse(BS)));
            eclipse(helper, caster);
            helper.assertTrue(has(target, BS), "no Blood Sovereign; the target heard " + target.heard() + ", the performer " + caster.heard());
            helper.assertTrue(!has(target, DS), "Deus Sanguis, which the altar forgets, stayed");
            var inst = EvoSkills.held(target, SkillAPI.getSkillRegistry().get(ResourceLocation.parse(BS))).orElseThrow();
            helper.assertTrue(!inst.isTemporarySkill() && inst.getRemoveTime() == -1, "the skill is not permanent: " + inst.getRemoveTime());
            helper.assertTrue(target.received(EvolutionRiteFactory.DONE) && caster.received(EvolutionRiteFactory.DONE_CASTER), "the done lines: " + target.heard() + " / " + caster.heard());
            helper.assertTrue(dropped(helper, CATALYST) == 0 && dropped(helper, EP) == 0, "a good rite gave the ingredients back");
            helper.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(target) - (RICH - price)) < 1, "EP is " + EnergyHelper.getBaseMaxEP(target) + ", Tensura's price is " + price + ": the rite takes no EP of its own and gives no bonus");
            long mark = target.getPersistentData().getLong(AscensionProbe.LAST_AWAKENING_TAG);
            helper.assertTrue(mark >= before && mark <= helper.getLevel().getGameTime(), "the altar's awakening mark is " + mark + ", the game time was " + before);
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueDemonLordQualifiesToo(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        if (learnClosed(helper)) {
            helper.succeed();
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            TensuraStorages.getExistenceFrom(target).setTrueHero(false);
            TensuraStorages.getExistenceFrom(target).setTrueDemonLord(true);
            eclipse(helper, caster);
            helper.assertTrue(has(target, BS), "a true Demon Lord was refused: " + target.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainFullMoonIsNotEnough(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            clearDropped(helper);
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(caster.received("supernaturalbloodlines.rite.needs_eclipse"), "the performer heard " + caster.heard());
            refusedNothingTaken(helper, target, RICH);
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lowEpIsRefusedWithoutAnOracle(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, 4_000_000);
        try {
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(target.received(NOT_READY), "the target heard " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            refusedNothingTaken(helper, target, 4_000_000);
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noStatusIsRefused(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            TensuraStorages.getExistenceFrom(target).setTrueHero(false);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(target.received(NOT_READY) && caster.heard().equals(FactionRiteFactory.NO_EFFECT), "target " + target.heard() + ", performer " + caster.heard());
            refusedNothingTaken(helper, target, RICH);
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deusSanguisMustBeHeldAndMastered(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer notMastered = ready(helper, RICH);
        try {
            EvoSkills.held(notMastered, ResourceLocation.parse(DS)).orElseThrow().setMastery(0);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(notMastered.received(NOT_READY) && caster.heard().equals(FactionRiteFactory.NO_EFFECT), "not mastered: " + notMastered.heard() + " / " + caster.heard());
            refusedNothingTaken(helper, notMastered, RICH);
            EvoTestSupport.forgetAll(notMastered);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(notMastered.received(NOT_READY), "not held: " + notMastered.heard());
            helper.assertTrue(!has(notMastered, BS) && dropped(helper, CATALYST) == 1, "granted or took the catalyst without Deus Sanguis");
        } finally {
            clean(helper, caster, notMastered);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHolderOfBloodSovereignIsToldSo(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            give(target, BS, 0);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(target.received(ALREADY) && caster.heard().equals(FactionRiteFactory.NO_EFFECT), "target " + target.heard() + ", performer " + caster.heard());
            helper.assertTrue(has(target, DS) && dropped(helper, CATALYST) == 1, "something was taken");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** The altar and the rite share one cooldown: the rite reads the tag the altar writes, and writes it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownIsSharedWithTheAltar(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            target.getPersistentData().putLong(AscensionProbe.LAST_AWAKENING_TAG, helper.getLevel().getGameTime()); // the altar awakened them just now
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(target.received(NOT_READY) && caster.heard().equals(FactionRiteFactory.NO_EFFECT), "inside the cooldown: " + target.heard() + " / " + caster.heard());
            helper.assertTrue(!has(target, BS) && dropped(helper, CATALYST) == 1, "granted inside the cooldown");
            if (!learnClosed(helper)) {
                target.getPersistentData().remove(AscensionProbe.LAST_AWAKENING_TAG); // never awakened: no cooldown
                clearDropped(helper);
                eclipse(helper, caster);
                helper.assertTrue(has(target, BS), "a player who never awakened was refused: " + target.heard());
            }
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** A learn that a listener vetoes costs the player nothing: Deus Sanguis stays, no awakening mark, no EP. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVetoedLearnKeepsDeusSanguisAndWritesNoMark(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        var blood = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(BS));
        var veto = (io.github.manasmods.manascore.skill.api.SkillEvents.UnlockSkillEvent) (inst, entity, msg) ->
                inst.getSkillId().equals(blood.getRegistryName()) ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass();
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        try {
            io.github.manasmods.manascore.skill.api.SkillEvents.UNLOCK_SKILL.register(veto);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(target.received(NOT_READY) && caster.heard().equals(FactionRiteFactory.NO_EFFECT), "target " + target.heard() + ", performer " + caster.heard());
            helper.assertTrue(has(target, DS) && !has(target, BS), "a vetoed learn cost Deus Sanguis");
            helper.assertTrue(!target.getPersistentData().contains(AscensionProbe.LAST_AWAKENING_TAG), "the awakening mark was written for a vetoed learn");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == RICH, "EP changed: " + EnergyHelper.getBaseMaxEP(target));
            helper.assertTrue(dropped(helper, CATALYST) == 1, "the catalyst was not given back");
        } finally {
            io.github.manasmods.manascore.skill.api.SkillEvents.UNLOCK_SKILL.unregister(veto);
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchesCloseTheRiteForThePerformer(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = ready(helper, RICH);
        var rules = helper.getLevel().getGameRules();
        @SuppressWarnings("unchecked")
        net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> rule = (net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue>)
                net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Hooks.staticField("com.simplygray.tensuraaddon.AddonGameRules", "DO_ASCENSION_ULTIMATE").value().orElseThrow();
        boolean was = rules.getBoolean(rule);
        try {
            rules.getRule(rule).set(false, helper.getLevel().getServer());
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(caster.received(CLOSED), "the rule off: the performer heard " + caster.heard());
            helper.assertTrue(!target.received(NOT_READY), "the target was told it is not ready: the rule is the world's");
            helper.assertTrue(!has(target, BS) && has(target, DS) && dropped(helper, CATALYST) == 1, "something was taken or granted with the rule off");
        } finally {
            rules.getRule(rule).set(was, helper.getLevel().getServer());
        }
        try {
            EvolutionRitesConfig.override("ultimate_rites_enabled", false);
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(caster.count(CLOSED) == 2 && !has(target, BS), "ultimate_rites_enabled off: " + caster.heard());
            EvolutionRitesConfig.override("ultimate_rites_enabled", null);
            EvolutionRitesConfig.override("disabled_recipes", List.of("blood_sovereign"));
            clearDropped(helper);
            eclipse(helper, caster);
            helper.assertTrue(caster.count(CLOSED) == 3 && !has(target, BS), "a disabled recipe: " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAWitchTypePerformsItAndOnlyWithConsent(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer notWitch = performer(helper, Species.NONE);
        RecordingPlayer target = ready(helper, RICH);
        try {
            clearDropped(helper);
            eclipse(helper, notWitch);
            helper.assertTrue(notWitch.received(NOT_WITCH) && !has(target, BS), "a non-witch performed it: " + notWitch.heard());
            target.setShiftKeyDown(false);
            RecordingPlayer witch = performer(helper, Species.WITCH);
            clearDropped(helper);
            eclipse(helper, witch);
            helper.assertTrue(!has(target, BS) && target.receivedOnActionBar("supernaturalbloodlines.rite.sneak_to_evolve"), "an unwilling target was evolved or not asked: " + target.heard());
            helper.assertTrue(witch.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + witch.heard());
            clean(helper, witch);
        } finally {
            clean(helper, notWitch, target);
        }
        helper.succeed();
    }

    /** What the performer hears is the same for a human, a vampire without the skill, and a target who lacks the EP. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePerformerLearnsNothingAboutTheTarget(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer human = onTheCircle(helper, true, RICH);
        try {
            clearDropped(helper);
            eclipse(helper, caster);
            String forHuman = caster.heard();
            clean(helper, human);
            RecordingPlayer vampire = onTheCircle(helper, true, RICH);
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            RecordingPlayer witch2 = performer(helper, Species.WITCH);
            clearDropped(helper);
            eclipse(helper, witch2);
            String forVampire = witch2.heard();
            clean(helper, vampire, witch2);
            RecordingPlayer poor = ready(helper, 4_000_000);
            RecordingPlayer witch3 = performer(helper, Species.WITCH);
            clearDropped(helper);
            eclipse(helper, witch3);
            String forPoor = witch3.heard();
            clean(helper, poor, witch3);
            helper.assertTrue(forHuman.equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + forHuman);
            helper.assertTrue(forHuman.equals(forVampire) && forHuman.equals(forPoor), "the performer's lines differ: human '" + forHuman + "', vampire '" + forVampire + "', poor '" + forPoor + "'");
        } finally {
            clean(helper, caster);
        }
        helper.succeed();
    }
}
