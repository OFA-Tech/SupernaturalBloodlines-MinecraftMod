package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.RiteEp;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery.MasteryChoice;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;

/**
 * Plan 20b Task 5 (R253): the rite's EP is split like the altar bill. A Mastery rite of the Unique tier costs 750 000 EP; the rite starts through the cost
 * hook's own {@code begin / commit} around {@code Rite.start()}, exactly as the chalk's tick does.
 */
@PrefixGameTestTemplate(false)
public final class CovenEpGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String RITE = "mastery_unique";
    private static final double EP = 5_000_000;
    private static final double COST = 750_000;

    private CovenEpGameTests() {
    }

    private static List<net.minecraft.world.item.ItemStack> items() {
        return stacks("minecraft:nether_star", 5, "minecraft:netherite_ingot", 5);
    }

    /** Runs the rite as the circle's anchor: the plan is staged by the hook, the rite starts, the hook commits. */
    private static void cast(GameTestHelper helper, Circle c, int power) {
        atNight(helper, true, () -> {
            var chalk = c.chalkEntity();
            RiteCost.begin(helper.getLevel(), c.anchor().getUUID(), centre(helper), chalk.getPosHolder(), power, true, type(helper, RITE));
            try {
                start(helper, RITE, c.anchor(), items());
            } finally {
                RiteCost.commit();
            }
        });
    }

    private static Circle fiveWitches(GameTestHelper helper) {
        Circle c = CovenCostGameTests.circleAt(helper, new BlockPos(8, 1, 8), 5);
        for (ServerPlayer p : c.array()) {
            EnergyHelper.setBaseMaxEP(p, EP);
        }
        return c;
    }

    private static RecordingPlayer outsideTarget(GameTestHelper helper) {
        RecordingPlayer target = onTheCircle(helper, true, EP);
        give(target, "tensura:anti_skill", 0);
        MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
        return target;
    }

    private static void tidy(GameTestHelper helper, Circle c, ServerPlayer... extra) {
        clearDropped(helper);
        MasteryChoice.clock = null;
        EvolutionRitesConfig.override("mastery_cooldown_days", null);
        CovenCostGameTests.tidy(helper, c.array());
        for (ServerPlayer p : extra) {
            if (p instanceof RecordingPlayer r) {
                MasteryChoice.clear(r);
                MasteryChoice.setLast(r, -1);
                EvoTestSupport.forgetAll(r);
                remove(helper, r);
                BloodlineAccess.set(r, BloodlineState.NONE);
            }
        }
    }

    private static boolean skipped(GameTestHelper helper) {
        return EvoTestSupport.skipped(helper, "mastery_common");
    }

    private static double ep(ServerPlayer p) {
        return EnergyHelper.getBaseMaxEP(p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetOutsideTheCirclePaysNoRiteEp(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        Circle c = fiveWitches(helper);
        RecordingPlayer target = outsideTarget(helper);
        try {
            cast(helper, c, 6000);
            ManasSkillInstance inst = net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills.held(target, ResourceLocation.parse("tensura:anti_skill")).orElseThrow();
            helper.assertTrue(inst.isMastered(target), "the Mastery did not happen; the target heard " + target.heard());
            helper.assertTrue(ep(target) == EP, "a target outside the circle paid rite EP: " + ep(target));
            for (ServerPlayer p : c.array()) {
                helper.assertTrue(ep(p) == EP - COST / 5, "a member's EP is " + ep(p) + ", the share is " + COST / 5);
            }
        } finally {
            tidy(helper, c, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetInsideTheCirclePaysItsShare(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        Circle c = fiveWitches(helper);
        ServerPlayer target = c.others().get(1);
        target.setPos(Vec3.atBottomCenterOf(centre(helper)).add(0, 1, 0));
        give(target, "tensura:anti_skill", 0);
        MasteryChoice.choose(target, ResourceLocation.parse("tensura:anti_skill"));
        target.setShiftKeyDown(true);
        try {
            cast(helper, c, 6000);
            ManasSkillInstance inst = net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills.held(target, ResourceLocation.parse("tensura:anti_skill")).orElseThrow();
            helper.assertTrue(inst.isMastered(target), "the Mastery did not happen");
            for (ServerPlayer p : c.array()) {
                helper.assertTrue(ep(p) == EP - COST / 5, "a member's EP is " + ep(p) + " (the target is a member and pays its share like the rest)");
            }
        } finally {
            MasteryChoice.clear(target);
            MasteryChoice.setLast(target, -1);
            EvoTestSupport.forgetAll(target);
            tidy(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aZeroPowerRiteStillSplitsItsEp(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        Circle c = fiveWitches(helper);
        RecordingPlayer target = outsideTarget(helper);
        try {
            cast(helper, c, 0);
            helper.assertTrue(ep(target) == EP, "the target paid EP");
            for (ServerPlayer p : c.array()) {
                helper.assertTrue(ep(p) == EP - COST / 5, "with a power of 0 a member's EP is " + ep(p));
            }
        } finally {
            tidy(helper, c, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theEpFloorDropsAMemberAndTheSharesAreRecomputed(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        Circle c = fiveWitches(helper);
        ServerPlayer low = c.others().get(2);
        EnergyHelper.setBaseMaxEP(low, 160_000);
        RecordingPlayer target = outsideTarget(helper);
        try {
            cast(helper, c, 6000);
            helper.assertTrue(ep(low) == 160_000, "the member under the floor paid: " + ep(low));
            helper.assertTrue(low instanceof net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist t && t.received("supernaturalbloodlines.coven.link_dropped.ep_floor"),
                    "the dropped member was not told");
            for (ServerPlayer p : c.array()) {
                if (p != low) {
                    helper.assertTrue(ep(p) == EP - COST / 4, "the others pay a quarter each: " + ep(p));
                }
            }
        } finally {
            tidy(helper, c, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEvolutionRiteTakesTensurasPriceFromTheTargetAndItsOwnEpFromTheCircle(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "evolve_great_sage")) {
            return;
        }
        if (!EvoTestSupport.learnOpen(helper, "tensura:great_sage")) {
            helper.succeed(); // an installed addon closes Tensura's own learn: the rite refuses and nothing is split
            return;
        }
        Circle c = fiveWitches(helper);
        RecordingPlayer control = onTheCircle(helper, false, EP);
        double price;
        try {
            var greatSage = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry().get(ResourceLocation.parse("tensura:great_sage"));
            io.github.manasmods.tensura.ability.SkillHelper.learnSkill(control, greatSage);
            price = EP - EnergyHelper.getBaseMaxEP(control);
        } finally {
            EvoTestSupport.forgetAll(control);
            remove(helper, control);
            BloodlineAccess.set(control, BloodlineState.NONE);
        }
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            give(target, "tensura:sage", 500);
            atNight(helper, true, () -> {
                RiteCost.begin(helper.getLevel(), c.anchor().getUUID(), centre(helper), c.chalkEntity().getPosHolder(), 9000, true, type(helper, "evolve_great_sage"));
                try {
                    start(helper, "evolve_great_sage", c.anchor(), stacks("minecraft:amethyst_shard", 4, "minecraft:ender_eye", 2, "enchanted:spirit_of_otherwhere", 1));
                } finally {
                    RiteCost.commit();
                }
            });
            helper.assertTrue(EvoTestSupport.has(target, "tensura:great_sage"), "no Great Sage; the target heard " + target.heard());
            helper.assertTrue(Math.abs(ep(target) - (EP - price)) < 1, "the target paid " + (EP - ep(target)) + ", Tensura's price is " + price + " and nothing of the rite's EP is theirs");
            for (ServerPlayer p : c.array()) {
                helper.assertTrue(Math.abs(ep(p) - (EP - 100_000 / 5.0)) < 1, "a member's EP is " + ep(p) + ", the share is 20000: Tensura's price is never shared");
            }
        } finally {
            tidy(helper, c, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whatTheTargetMustHoldIsItsOwnShare(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        Circle c = fiveWitches(helper);
        RecordingPlayer outsider = onTheCircle(helper, true, EP);
        try {
            helper.assertTrue(RiteEp.owedBy(outsider, COST) == COST, "with no plan the target owes the whole price");
            RiteCost.begin(helper.getLevel(), c.anchor().getUUID(), centre(helper), c.chalkEntity().getPosHolder(), 6000, true, type(helper, RITE));
            helper.assertTrue(CircleStage.current() != null && !CircleStage.current().result().shares().isEmpty(), "no plan was staged");
            helper.assertTrue(RiteEp.owedBy(outsider, COST) == 0, "a target outside the circle owes " + RiteEp.owedBy(outsider, COST));
            helper.assertTrue(RiteEp.owedBy(c.others().get(0), COST) == COST / 5, "a member owes " + RiteEp.owedBy(c.others().get(0), COST));
        } finally {
            RiteCost.discard();
            tidy(helper, c, outsider);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSoloCastStillCostsTheTargetTheWholePrice(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        RecordingPlayer caster = EvoTestSupport.performer(helper, net.ofatech.supernaturalbloodlines.core.bloodline.Species.WITCH);
        RecordingPlayer target = outsideTarget(helper);
        try {
            atNight(helper, true, () -> start(helper, RITE, caster, items()));
            helper.assertTrue(ep(target) == EP - COST, "a solo cast took " + (EP - ep(target)) + " instead of " + COST);
            helper.assertTrue(CircleStage.current() == null, "a plan is staged");
        } finally {
            clearDropped(helper);
            MasteryChoice.clear(target);
            MasteryChoice.setLast(target, -1);
            EvoTestSupport.forgetAll(target);
            remove(helper, caster);
            remove(helper, target);
            BloodlineAccess.set(caster, BloodlineState.NONE);
        }
        helper.succeed();
    }
}