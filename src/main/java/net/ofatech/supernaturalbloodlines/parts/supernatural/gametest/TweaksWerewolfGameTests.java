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
            for (EntityType<? extends Mob> type : java.util.List.of(EntityType.SKELETON, EntityType.STRAY)) {
                Mob mob = (Mob) helper.spawn(type, 1, 1, 1);
                VampirismTweaks.TweakGoal g = goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES);
                helper.assertTrue(g != null, type + ": no werewolf goal (it is always added, the switch is asked at use)");
                helper.assertTrue(g.picks(wolf), type + ": does not pick a werewolf");
                helper.assertTrue(!g.picks(vampire) && !g.picks(human), type + ": picks someone who is no werewolf");
                mob.discard();
            }
            Mob zombie = (Mob) helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
            helper.assertTrue(goal(zombie, Tweak.SKELETONS_ATTACK_WEREWOLVES) == null, "a zombie got the skeleton goal");
            zombie.discard();
            for (EntityType<? extends Mob> type : java.util.List.of(EntityType.WITHER_SKELETON, EntityType.BOGGED)) {
                Mob other = (Mob) helper.spawn(type, 1, 1, 1);
                helper.assertTrue(goal(other, Tweak.SKELETONS_ATTACK_WEREWOLVES) == null, type + " got the skeleton goal");
                other.discard();
            }
        } finally {
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, null);
        }
        helper.succeed();
    }

    /** The werewolf switch flips live on a skeleton that is already loaded, both ways (a fake player is put in the level's list for the calls only). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWerewolfSwitchFlipsLiveBothWays(GameTestHelper helper) {
        ServerPlayer wolf = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        wolf.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 1.5)));
        helper.getLevel().players().add(wolf);
        Runnable ceiling = TweaksGameTests.openCeiling(helper);
        Mob mob = (Mob) helper.spawn(EntityType.SKELETON, 1, 1, 1);
        try {
            VampirismTweaks.TweakGoal g = goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES);
            helper.assertTrue(!uses(g), "used with the switch off");
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, true);
            helper.assertTrue(uses(g), "turning it on did not reach a loaded skeleton");
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, false);
            helper.assertTrue(!uses(g), "turning it off did not stop a loaded skeleton");
        } finally {
            ceiling.run();
            helper.getLevel().players().remove(wolf);
            SupernaturalConfig.overrideTweak(Tweak.SKELETONS_ATTACK_WEREWOLVES, null);
            mob.discard();
        }
        helper.succeed();
    }

    private static boolean uses(VampirismTweaks.TweakGoal g) {
        for (int i = 0; i < 300; i++) {
            if (g.canUse()) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothSwitchesGiveASkeletonBothGoals(GameTestHelper helper) {
        Mob mob = (Mob) helper.spawn(EntityType.SKELETON, 1, 1, 1);
        helper.assertTrue(goal(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES) != null && goal(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) != null, "not both goals");
        mob.discard();
        helper.succeed();
    }
}
