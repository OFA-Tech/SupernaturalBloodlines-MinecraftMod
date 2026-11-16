package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;

/** Plan 13d H1: Werewolves' own Free Will skill is full moon control, so the moon gate (R96, on by default) does not cancel its owner's shift. */
@PrefixGameTestTemplate(false)
public final class FreeWillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private FreeWillGameTests() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean voluntaryBeastFormCancelled(ServerPlayer p) {
        var event = new ActionEvent.ActionActivatedEvent(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) ModActions.BEAST_FORM.get(), 0, 0);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void freeWillOwnerShiftsAtWillWhileTheMoonGateStaysOn(GameTestHelper helper) {
        helper.assertTrue(MoonControl.moonBound(), "setup: moon_bound_werewolves must be on by default (R96)");
        ServerPlayer plain = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(plain).setFactionAndLevel(WReference.WEREWOLF_FACTION, 14), "could not make a werewolf");
        helper.assertTrue(voluntaryBeastFormCancelled(plain), "control: a werewolf without control must still be bound to the moon");

        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 14), "could not make a werewolf");
        ISkill skill = VampirismRegistries.SKILL.get().get(ResourceLocation.parse("werewolves:free_will"));
        helper.assertTrue(skill != null, "werewolves:free_will is not registered (a Werewolves update?)");
        VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler().enableSkill(skill, true);
        helper.assertTrue(ForcedShift.hasFreeWill(p), "the skill did not turn on");
        helper.assertTrue(!voluntaryBeastFormCancelled(p), "a Free Will owner's voluntary shift was cancelled");
        helper.succeed();
    }
}
