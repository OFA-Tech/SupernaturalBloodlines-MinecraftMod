package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;

/** Werewolves' actions through the same slots (the werewolf player is an {@code IFactionPlayer} too). Needs Vampirism and Werewolves. */
@PrefixGameTestTemplate(false)
public final class ActionWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ActionWerewolfGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfActionTriggers(GameTestHelper helper) {
        ServerPlayer p = BiteWerewolfGameTests.wolf(helper);
        ResourceLocation action = ActionGameTests.usableLasting(p, WReference.WEREWOLF_FACTION);
        helper.assertTrue(LoadoutService.set(p, 1, action) == null, "a werewolf's own action was refused");
        String refusal = ActionService.trigger(p, 1, -1, false);
        helper.assertTrue(refusal == null, action + " was refused: " + refusal);
        helper.assertTrue(ActionGameTests.active(p, action), action + " is not active");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridGetsWerewolfActions(GameTestHelper helper) {
        ServerPlayer p = BiteHybridGameTests.hybrid(helper, Species.HYBRID, 0);
        ResourceLocation action = ActionGameTests.usableLasting(p, WReference.WEREWOLF_FACTION);
        helper.assertTrue(FactionActions.unlocked(p, action), "a hybrid does not have the werewolf action unlocked");
        helper.assertTrue(LoadoutService.set(p, 2, action) == null, "a hybrid was refused a werewolf action");
        helper.assertTrue(ActionService.trigger(p, 2, -1, false) == null && ActionGameTests.active(p, action), "a hybrid's werewolf action did not run");
        helper.assertTrue(LoadoutService.get(p).get(0).equals(java.util.Optional.of(Loadout.BITE)), "a hybrid lost Bite in slot 1");
        helper.succeed();
    }
}
