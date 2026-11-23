package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;

/** The wheel's Werewolves entries. Needs Vampirism and Werewolves. */
@PrefixGameTestTemplate(false)
public final class WheelWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private WheelWerewolfGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfActionsAreSourceWerewolves(GameTestHelper helper) {
        ServerPlayer p = BiteWerewolfGameTests.wolf(helper);
        ResourceLocation id = ActionGameTests.usableLasting(p, de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION);
        helper.assertTrue(WheelCatalogueGameTests.has(p, PowerSource.WEREWOLVES, id), id + " is not listed under Werewolves");
        helper.assertTrue(!WheelCatalogueGameTests.has(p, PowerSource.VAMPIRISM, id), id + " is listed under Vampirism");
        helper.succeed();
    }
}
