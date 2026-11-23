package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.blood.GiveBloodAction;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** What force-fed blood does to a werewolf (R208 follows R210's drinker rules). Needs Werewolves. */
@PrefixGameTestTemplate(false)
public final class GiveBloodWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GiveBloodWerewolfGameTests() {
    }

    private static TestPlayers.TalkingDuelist giver(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.NONE).build());
        return p;
    }

    private static TestPlayers.TalkingDuelist wolf(GameTestHelper helper) {
        TestPlayers.TalkingDuelist t = TestPlayers.createTalkingDuelist(helper);
        t.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        helper.assertTrue(VampirismAPI.factionPlayerHandler(t).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        t.setShiftKeyDown(true);
        return t;
    }

    private static void clean(TestPlayers.TalkingDuelist... players) {
        for (TestPlayers.TalkingDuelist p : players) {
            p.removeAllEffects();
            p.setShiftKeyDown(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfInTransitionGoesUnstableAndTheGiverHearsTheUsualLine(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = giver(helper);
        TestPlayers.TalkingDuelist plain = wolf(helper);
        TestPlayers.TalkingDuelist inTransition = wolf(helper);
        TestPlayers.TalkingDuelist giverB = giver(helper);
        try {
            BloodlineAccess.set(inTransition, BloodlineState.NONE.toBuilder().inTransition(true).transitionTicksLeft(5000).build());
            helper.assertTrue(GiveBloodAction.INSTANCE.trigger(giver, plain) == null, "a plain werewolf was refused");
            helper.assertTrue(plain.receivedOnActionBar(BloodBottles.NO_EFFECT) && BloodlineAccess.get(plain).equals(BloodlineState.NONE), "a plain werewolf was affected or not told it did nothing");
            helper.assertTrue(GiveBloodAction.INSTANCE.trigger(giverB, inTransition) == null, "a werewolf in transition was refused");
            helper.assertTrue(BloodlineAccess.get(inTransition).unstable() && inTransition.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT), "the transition did not go unstable: " + BloodlineAccess.get(inTransition));
            helper.assertTrue(giver.countOnActionBar(GiveBloodAction.DONE) == 1 && giverB.countOnActionBar(GiveBloodAction.DONE) == 1
                    && cooldown(giver) == cooldown(giverB), "the givers heard or paid different things");
        } finally {
            clean(giver, plain, inTransition, giverB);
        }
        helper.succeed();
    }

    private static int cooldown(TestPlayers.TalkingDuelist p) {
        return net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock.left(p, ExtraActions.GIVE_BLOOD);
    }
}
