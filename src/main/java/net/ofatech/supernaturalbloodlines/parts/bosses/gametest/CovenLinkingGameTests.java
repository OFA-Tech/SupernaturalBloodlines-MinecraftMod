package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circleAt;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import net.ofatech.supernaturalbloodlines.core.group.circle.ChannelMeter;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.link.LinkIndex;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenRiteGameTests;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CovenPotency;

/** Plan 20b Task 9 (R254): the Linking Spell lasts longer with a circle behind it. A solo cast is unchanged. */
@PrefixGameTestTemplate(false)
public final class CovenLinkingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CovenLinkingGameTests() {
    }

    private static List<ItemStack> items() {
        return List.of(new ItemStack(BossItems.ESTHER_GRIMOIRE.get()), new ItemStack(Items.LEAD, 2), new ItemStack(Items.AMETHYST_SHARD, 2));
    }

    private static long ticksOfLink(GameTestHelper helper, UUID who) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        return index.of(who).orElseThrow(() -> new IllegalStateException("not linked")).until() - helper.getLevel().getGameTime();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLinkingSpellScalesWithPotency(GameTestHelper helper) {
        LinkIndex index = LinkIndex.get(helper.getLevel().getServer());
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        Circle solo = circleAt(helper, new BlockPos(8, 1, 8), 2);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(9, 1, 9));
        try {
            index.clear();
            CircleLink.dropMember(helper.getLevel().getServer(), solo.others().get(0).getUUID(), false);
            solo.others().get(0).setPos(solo.others().get(0).getX() + 30, solo.others().get(0).getY(), solo.others().get(0).getZ());
            CovenRiteGameTests.cast(helper, solo, "rite_of_linking", items());
            helper.assertTrue(index.linked(solo.anchor().getUUID(), helper.getLevel().getGameTime()), "a solo cast did not link");
            long soloTicks = ticksOfLink(helper, solo.anchor().getUUID());
            helper.assertTrue(soloTicks == 60 * 20, "a solo cast lasts 60 seconds, not " + soloTicks / 20.0);
        } finally {
            cow.discard();
            index.clear();
            CovenCostGameTests.tidy(helper, solo.array());
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped(helper);
        }
        Circle pair = circleAt(helper, new BlockPos(8, 1, 8), 2);
        try {
            index.clear();
            var partner = pair.others().get(0);
            partner.setShiftKeyDown(true);
            partner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
            CovenRiteGameTests.cast(helper, pair, "rite_of_linking", items());
            helper.assertTrue(index.linked(pair.anchor().getUUID(), helper.getLevel().getGameTime()), "a circle of two did not link: the partner sneaked with a lead");
            long ticks = ticksOfLink(helper, pair.anchor().getUUID());
            helper.assertTrue(ticks == 72 * 20, "60 seconds at the potency of two (1.2) is 72, not " + ticks / 20.0);
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
            index.clear();
            CovenCostGameTests.tidy(helper, pair.array());
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped(helper);
        }
        helper.succeed();
    }

    /** Every member is inside the circle, so the Linking Spell (which links exactly two creatures) can only ever have a circle of two; the scale is still defined for three. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfThreeScalesSixtySecondsToEightyTwo(GameTestHelper helper) {
        Map<UUID, CirclePlan.Share> shares = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            shares.put(new UUID(1, i), new CirclePlan.Share(0, 0, ChannelMeter.Zone.CLEAN, 0));
        }
        CircleStage.set(new CircleStage.Staged(new UUID(1, 0), helper.getLevel().dimension(), BlockPos.ZERO, new CirclePlan.Result(0, shares, List.of(), 1)));
        try {
            helper.assertTrue(Math.round(CovenPotency.scale(60)) == 82, "60 seconds at the potency of three is " + CovenPotency.scale(60));
        } finally {
            CircleStage.set(null);
        }
        helper.assertTrue(CovenPotency.scale(60) == 60, "with no plan the potency is 1");
        helper.succeed();
    }
}