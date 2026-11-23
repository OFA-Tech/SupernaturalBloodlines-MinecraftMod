package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks.VampirismTweaks;

/** The skeleton tweak needs Werewolves for its target: registered only when it is loaded. */
@PrefixGameTestTemplate(false)
public final class TweaksWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TweaksWerewolfGameTests() {}

    private static VampirismTweaks.TweakGoal goal(Mob mob, Tweak t) {
        return mob.targetSelector.getAvailableGoals().stream().map(w -> w.getGoal()).filter(g -> g instanceof VampirismTweaks.TweakGoal tg && tg.tweak == t)
                .map(g -> (VampirismTweaks.TweakGoal) g).findFirst().orElse(null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void skeletonTargetsAWerewolf(GameTestHelper helper) {
        ServerPlayer wolf = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        ServerPlayer vampire = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        ServerPlayer human = TestPlayers.createDuelist(helper);
        try {
            Mob off = (Mob) helper.spawn(EntityType.SKELETON, 1, 1, 1);
            helper.assertTrue(goal(off, Tweak.SKELETONS_ATTACK_WEREWOLVES) == null, "a goal was added with the switch off");
            off.discard();

            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, true);
            for (EntityType<? extends Mob> type : java.util.List.of(EntityType.SKELETON, EntityType.STRAY)) {
                Mob mob = (Mob) helper.spawn(type, 1, 1, 1);
                VampirismTweaks.TweakGoal g = goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES);
                helper.assertTrue(g != null, type + ": no werewolf goal");
                helper.assertTrue(g.picks(wolf), type + ": does not pick a werewolf");
                helper.assertTrue(!g.picks(vampire) && !g.picks(human), type + ": picks someone who is no werewolf");
                helper.assertTrue(goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) == null, type + ": got the vampire goal with that switch off");
                mob.discard();
            }
            Mob zombie = (Mob) helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
            helper.assertTrue(goal(zombie, Tweak.SKELETONS_ATTACK_WEREWOLVES) == null, "a zombie got the skeleton goal");
            zombie.discard();
        } finally {
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothSwitchesGiveASkeletonBothGoals(GameTestHelper helper) {
        try {
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, true);
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, true);
            Mob mob = (Mob) helper.spawn(EntityType.SKELETON, 1, 1, 1);
            helper.assertTrue(goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES) != null && goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) != null, "not both goals");
            mob.discard();
        } finally {
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, null);
            SupernaturalConfig.overrideTweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, null);
        }
        helper.succeed();
    }
}
