package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.CovenForgeRiteFactory;
import net.ofatech.supernaturalbloodlines.shared.supernatural.CircleQuery;

/** Plan 20b Task 10: the forge factory belongs to the artifacts part and names no species class; with no circle behind the seam it refuses (run 4: species off). */
@PrefixGameTestTemplate(false)
public final class CovenForgeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CovenForgeGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theForgeFactoryLoadsAndRefusesWithNoCircle(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("species")) {
            // the rite files need the species part: without it they are not loaded at all, so there is nothing to start (and the factory class still loads)
            helper.assertTrue(CovenForgeRiteFactory.ID != null && CircleQuery.staged() == 0, "the seam answers a plan with the species part off");
            helper.succeed();
            return;
        }
        var caster = TestPlayers.createTalkingDuelist(helper);
        place(helper, caster);
        try {
            helper.assertTrue(CircleQuery.staged() == 0, "a plan is staged outside a rite");
            start(helper, "coven_sun_ring", caster, List.of(new ItemStack(item("minecraft:sunflower"), 2), new ItemStack(item("minecraft:glowstone_dust"), 4), new ItemStack(item("minecraft:gold_ingot"), 2)));
            helper.assertTrue(caster.receivedOnActionBar(CovenForgeRiteFactory.CIRCLE) || caster.receivedOnActionBar(CovenForgeRiteFactory.OFF), "a solo cast was not refused: " + caster.received());
            helper.assertTrue(dropped(helper, "supernaturalbloodlines:sun_ring") == 0 && dropped(helper, "minecraft:gold_ingot") == 2, "a ring was made, or the items did not come back");
        } finally {
            clearDropped(helper);
            remove(helper, caster);
        }
        helper.succeed();
    }
}