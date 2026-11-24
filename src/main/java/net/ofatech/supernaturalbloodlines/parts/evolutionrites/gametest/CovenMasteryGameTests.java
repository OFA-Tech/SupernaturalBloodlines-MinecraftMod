package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atEclipse;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circleAt;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.tidy;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenRiteGameTests.cast;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** Plan 20b Task 10 (R267): Mastery's Ultimate needs seven linked members in the circle when it starts; the other tiers need none. */
@PrefixGameTestTemplate(false)
public final class CovenMasteryGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EP = 5_000_000;
    private static final double MEMBER_EP = 10_000_000;
    private static final String CIRCLE = "supernaturalbloodlines.refusal.mastery.circle";
    private static final String ID = "tensura:anti_skill";

    private CovenMasteryGameTests() {
    }

    private static List<ItemStack> ultimateItems() {
        return stacks("minecraft:nether_star", 5, "minecraft:dragon_breath", 5, "minecraft:netherite_ingot", 10);
    }

    private static List<ItemStack> commonItems() {
        return stacks("minecraft:diamond_block", 5, "minecraft:echo_shard", 5);
    }

    private static ManasSkillInstance ready(RecordingPlayer target) {
        EvoSkills.tierHook = skill -> skill.getRegistryName() != null && skill.getRegistryName().toString().equals(ID) ? Tier.ULTIMATE : null;
        ManasSkillInstance inst = give(target, ID, 0);
        MasteryChoice.choose(target, ResourceLocation.parse(ID));
        return inst;
    }

    private static void finish(GameTestHelper helper, Circle c, RecordingPlayer target) {
        EvoSkills.tierHook = null;
        GroupsConfig.override("mastery_ultimate_circle", null);
        GroupsConfig.override("link_enabled", null);
        GroupsConfig.override("covens_enabled", null);
        MasteryChoice.clear(target);
        MasteryChoice.setLast(target, -1);
        EvoTestSupport.forgetAll(target);
        RiteTestSupport.remove(helper, target);
        clearDropped(helper);
        tidy(helper, c.array());
    }

    private static Circle sevenWithTarget(GameTestHelper helper, int n, RecordingPlayer[] target) {
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), n);
        for (TalkingDuelist m : c.all()) {
            EnergyHelper.setBaseMaxEP(m, MEMBER_EP);
        }
        target[0] = onTheCircle(helper, true, EP);
        return c;
    }

    private static double spent(Circle c) {
        return c.all().stream().mapToDouble(m -> MEMBER_EP - EnergyHelper.getBaseMaxEP(m)).sum();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfSevenCanDoUltimateMastery(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 7, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(inst.isMastered(t[0]), "the Ultimate was not mastered by a circle of seven: " + t[0].heard() + " / " + c.anchor().receivedOnActionBar(CIRCLE));
            helper.assertTrue(Math.abs(spent(c) - 2_000_000) < 8, "the members paid " + spent(c) + " EP between them, not 2,000,000");
            helper.assertTrue(c.all().stream().allMatch(m -> EnergyHelper.getBaseMaxEP(m) < MEMBER_EP), "a member paid nothing");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(t[0]) > EP - 1000, "the target paid the circle's EP: " + EnergyHelper.getBaseMaxEP(t[0]));
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfSixIsRefusedGenerically(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 6, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            String before = t[0].heard();
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(!inst.isMastered(t[0]) && c.anchor().receivedOnActionBar(CIRCLE), "six were not refused with the circle line");
            helper.assertTrue(c.anchor().countOnActionBar(CIRCLE) == 1 && t[0].heard().equals(before), "the target heard something: " + t[0].heard());
            helper.assertTrue(spent(c) == 0 && EnergyHelper.getBaseMaxEP(t[0]) == EP, "somebody was charged");
            helper.assertTrue(dropped(helper, "minecraft:nether_star") == 5, "the items did not come back");
            // the same line for a target with nothing chosen
            clearDropped(helper);
            MasteryChoice.clear(t[0]);
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(c.anchor().countOnActionBar(CIRCLE) == 2 && t[0].heard().equals(before), "a target with no choice was answered differently");
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleShortOfSevenAfterTheEpFloorIsRefusedGenerically(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 7, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            // a member whose share would leave them under the floor is dropped from the plan: the circle is six when the rite starts
            EnergyHelper.setBaseMaxEP(c.others().get(3), 100_000);
            String before = t[0].heard();
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(!inst.isMastered(t[0]) && c.anchor().receivedOnActionBar(CIRCLE), "a circle of seven with one member below the EP floor was not refused");
            helper.assertTrue(t[0].heard().equals(before) && c.all().stream().allMatch(m -> EnergyHelper.getBaseMaxEP(m) == (m == c.others().get(3) ? 100_000 : MEMBER_EP)),
                    "somebody was charged: " + c.all().stream().map(EnergyHelper::getBaseMaxEP).toList() + " " + t[0].heard());
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void soloUltimateMasteryIsRefused(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 7, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            GroupsConfig.override("link_enabled", true);
            net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.forget(c.anchor().getUUID());
            for (TalkingDuelist m : c.others()) {
                net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.forget(m.getUUID());
            }
            atEclipse(helper, () -> net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start(helper, "mastery_ultimate", c.anchor(), ultimateItems()));
            helper.assertTrue(!inst.isMastered(t[0]) && c.anchor().receivedOnActionBar(CIRCLE), "a solo cast of the Ultimate was not refused");
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLowerTiersNeedNoCircle(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_common")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = circleAt(helper, new BlockPos(8, 1, 8), 2);
        t[0] = onTheCircle(helper, true, EP);
        try {
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules.circleOk(Tier.ULTIMATE, 2, 7, false), "the Ultimate gate is open at two");
            for (Tier tier : List.of(Tier.COMMON, Tier.EXTRA, Tier.UNIQUE)) {
                helper.assertTrue(net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules.circleOk(tier, 0, 7, false), tier + " is gated by the circle");
            }
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start(helper, "mastery_common", c.anchor(), commonItems());
            helper.assertTrue(!c.anchor().receivedOnActionBar(CIRCLE), "a Common mastery was refused for its circle");
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGateIsWaivedWhenTheLinkIsOff(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 2, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            GroupsConfig.override("link_enabled", false);
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(!c.anchor().receivedOnActionBar(CIRCLE) && inst.isMastered(t[0]), "with the link off the Ultimate still wanted a circle: " + t[0].heard());
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGateIsWaivedWhenTheValueIsZero(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 2, t);
        try {
            ManasSkillInstance inst = ready(t[0]);
            GroupsConfig.override("mastery_ultimate_circle", 0);
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(!c.anchor().receivedOnActionBar(CIRCLE) && inst.isMastered(t[0]), "a requirement of 0 still wanted a circle: " + t[0].heard());
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRefusedUltimateGivesTheItemsBackAndChargesNobody(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_ultimate")) {
            return;
        }
        RecordingPlayer[] t = new RecordingPlayer[1];
        Circle c = sevenWithTarget(helper, 3, t);
        try {
            ready(t[0]);
            atEclipse(helper, () -> cast(helper, c, "mastery_ultimate", ultimateItems()));
            helper.assertTrue(c.anchor().receivedOnActionBar(CIRCLE), "three were not refused");
            helper.assertTrue(dropped(helper, "minecraft:nether_star") == 5 && dropped(helper, "minecraft:dragon_breath") == 5 && dropped(helper, "minecraft:netherite_ingot") == 10, "the items did not all come back");
            helper.assertTrue(spent(c) == 0 && EnergyHelper.getBaseMaxEP(t[0]) == EP, "somebody paid EP");
        } finally {
            finish(helper, c, t[0]);
        }
        helper.succeed();
    }
}