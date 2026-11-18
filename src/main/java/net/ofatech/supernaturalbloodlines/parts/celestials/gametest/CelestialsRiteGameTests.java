package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import net.favouriteless.enchanted.api.altar.SimplePowerPosHolder;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** The altar cost of a rite on a Blue Moon and a super moon. Needs the species part (so Vampirism and Enchanted). */
@PrefixGameTestTemplate(false)
public final class CelestialsRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CelestialsRiteGameTests() {
    }

    private static int altarCost(GameTestHelper helper, ServerPlayer p, RiteType type) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        int cost = RiteCost.begin(helper.getLevel(), p.getUUID(), pos, new SimplePowerPosHolder(pos), 100, true, type);
        RiteCost.discard();
        return cost;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlueMoonAndASuperMoonCutTheAltarCost(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RiteTestSupport.place(helper, p); // the cost hook finds the caster in the level
        RiteType moonRite = RiteTestSupport.type(helper, "rite_of_the_moon"); // needs Werewolves; null without it
        try {
            int plain = altarCost(helper, p, null);
            helper.assertTrue(plain == 100, "control: the plain cost is " + plain);
            CelestialsGameTests.with(new FakeEc().running("blue_moon"), () -> {
                helper.assertTrue(altarCost(helper, p, null) == 80, "a Blue Moon cost " + altarCost(helper, p, null) + ", not 80");
                if (moonRite != null) {
                    helper.assertTrue(altarCost(helper, p, moonRite) == 80, "a Blue Moon cost for the Rite of the Moon: " + altarCost(helper, p, moonRite));
                }
            });
            CelestialsGameTests.with(new FakeEc().running("super_blue_moon"), () -> {
                helper.assertTrue(altarCost(helper, p, null) == 80, "a rite that does not need the full moon got the super discount: " + altarCost(helper, p, null));
                if (moonRite != null) {
                    helper.assertTrue(altarCost(helper, p, moonRite) == 60, "the Super Blue Moon cost for the Rite of the Moon: " + altarCost(helper, p, moonRite));
                }
            });
            CelestialsGameTests.with(new FakeEc().running("super_moon"), () -> {
                helper.assertTrue(altarCost(helper, p, null) == 100, "a plain super moon cut a rite that does not need the full moon");
                if (moonRite != null) {
                    helper.assertTrue(altarCost(helper, p, moonRite) == 75, "the super moon cost for the Rite of the Moon: " + altarCost(helper, p, moonRite));
                }
            });
            CelestialsGameTests.with(new FakeEc().running("blood_moon"), () -> helper.assertTrue(altarCost(helper, p, moonRite) == 100, "a Blood Moon cut the cost"));
            helper.assertTrue(altarCost(helper, p, moonRite) == 100, "the discount stayed after the moon");
        } finally {
            RiteTestSupport.remove(helper, p);
        }
        helper.succeed();
    }
}
