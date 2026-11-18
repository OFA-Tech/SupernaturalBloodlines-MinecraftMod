package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.celestials.CelestialsConfig;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/** A Super Blue Moon and the Werewolves forced shift. Needs Werewolves. */
@PrefixGameTestTemplate(false)
public final class CelestialsWolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CelestialsWolfGameTests() {
    }

    /** Whether the forced full-moon shift (a form action activated inside the player's own tick) is cancelled. The gate is the part's own: nothing is registered here. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean forcedShiftCancelled(ServerPlayer p) {
        ForcedShift.enterTick(p);
        try {
            var event = new ActionEvent.ActionActivatedEvent(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) ModActions.BEAST_FORM.get(), 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    /** For the part-switch test: with a Super Blue Moon installed, the forced shift is spared when the part is on and not when it is off. */
    public static void forcedShiftHook(GameTestHelper helper, boolean partOn) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        boolean spared = forcedShiftCancelled(p);
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.assertTrue(spared == partOn, "forced-shift hook: part " + (partOn ? "on" : "off") + ", spared " + spared);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSuperBlueMoonSparesTheForcedShiftAndNothingElseDoes(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        try {
            helper.assertFalse(forcedShiftCancelled(p), "control: the full moon's shift was cancelled without a mood");
            CelestialsGameTests.with(new FakeEc().running("super_blue_moon"), () -> {
                helper.assertTrue(forcedShiftCancelled(p), "the Super Blue Moon did not spare the forced shift");
                CelestialsConfig.SUPER_BLUE_SKIPS_FORCED_SHIFT.set(false);
                try {
                    helper.assertFalse(forcedShiftCancelled(p), "the switch is off and the shift was still spared");
                } finally {
                    CelestialsConfig.SUPER_BLUE_SKIPS_FORCED_SHIFT.set(true);
                }
            });
            CelestialsGameTests.with(new FakeEc().running("blue_moon"), () -> helper.assertFalse(forcedShiftCancelled(p), "a plain Blue Moon spared the forced shift"));
            CelestialsGameTests.with(new FakeEc().running("super_blood_moon"), () -> helper.assertFalse(forcedShiftCancelled(p), "a Super Blood Moon spared the forced shift"));
            helper.assertFalse(forcedShiftCancelled(p), "the spare stayed after the moon");
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        }
        helper.succeed();
    }
}
