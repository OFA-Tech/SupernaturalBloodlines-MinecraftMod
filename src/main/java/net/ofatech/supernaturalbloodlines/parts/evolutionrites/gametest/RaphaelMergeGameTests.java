package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoGrant;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.merge.RaphaelMerge;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.FoxAblazeProbe;

/**
 * The Raphael merge listener and the login sweep (plan 19b task 4), on Tensura skills standing in for the two Raphaels and the two skills a refused evolution
 * gave up, so the logic runs in the normal matrix. The real ids and jars are {@link RaphaelMergeAddonGameTests}.
 */
@PrefixGameTestTemplate(false)
public final class RaphaelMergeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String SOURCE = "tensura:ranged_barrier";
    private static final String TARGET = "tensura:multilayer_barrier";
    private static final String RESTORE_A = "tensura:degenerate";
    private static final String RESTORE_B = "tensura:corrosion_resistance";
    private static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    private static final String RESTORED = "supernaturalbloodlines.raphael.restored";
    private static final double EP = 5_000_000;

    private RaphaelMergeGameTests() {
    }

    private static ResourceLocation id(String s) {
        return ResourceLocation.parse(s);
    }

    private static ManasSkill skill(String s) {
        return SkillAPI.getSkillRegistry().get(id(s));
    }

    private static RaphaelMerge.Merge stand(java.util.function.BooleanSupplier active) {
        return new RaphaelMerge.Merge(id(SOURCE), id(TARGET), List.of(id(SOURCE), id(TARGET)), List.of(id(RESTORE_A), id(RESTORE_B)), active);
    }

    /** An installed addon can close Tensura's own learn of a stand-in by its own rule: then the test has nothing to say. */
    private static boolean open(GameTestHelper helper) {
        for (String s : List.of(SOURCE, TARGET, RESTORE_A, RESTORE_B)) {
            if (!EvoTestSupport.learnOpen(helper, s)) {
                helper.succeed();
                return false;
            }
        }
        return true;
    }

    private static RecordingPlayer player(GameTestHelper helper) {
        return onTheCircle(helper, false, EP);
    }

    private static void clean(GameTestHelper helper, ServerPlayer... ps) {
        RaphaelMerge.override(null);
        FoxAblazeProbe.ownedHook = null;
        for (ServerPlayer p : ps) {
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static SkillEvents.UnlockSkillEvent veto(String skillId) {
        SkillEvents.UnlockSkillEvent v = (inst, who, message) -> inst.getSkillId().equals(id(skillId)) ? EventResult.interruptFalse() : EventResult.pass();
        SkillEvents.UNLOCK_SKILL.register(v);
        return v;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLearnOfTheSourceBecomesTheTarget(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        try {
            RaphaelMerge.override(stand(() -> true));
            helper.assertTrue(!EvoGrant.learnFree(p, skill(SOURCE)), "the learn of the source reported success");
            helper.assertTrue(!has(p, SOURCE), "the source was stored");
            helper.assertTrue(has(p, TARGET), "the target was not learned");
            var inst = SkillAPI.getSkillsFrom(p).getSkill(skill(TARGET)).orElseThrow();
            helper.assertTrue(inst instanceof TensuraSkillInstance t && t.getOrCreateTag().getBoolean(EvoGrant.GIFTED), "the target is not a gifted instance");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(p) == EP, "the redirect charged " + (EP - EnergyHelper.getBaseMaxEP(p)));
            // ManasCore's own "you learned a skill" line is the player's; what must not appear is a refusal or a restore
            helper.assertTrue(!p.received(NOT_READY) && !p.received(RESTORED), "a clean redirect said it refused: " + p.heard());
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ownedByAnotherPlayerRefusesAndRestoresTheRestoreSkills(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        try {
            RaphaelMerge.override(stand(() -> true));
            FoxAblazeProbe.ownedHook = x -> true;
            helper.assertTrue(!EvoGrant.learnFree(p, skill(SOURCE)), "a refused learn reported success");
            helper.assertTrue(!has(p, SOURCE) && !has(p, TARGET), "a refused learn left a Raphael: source " + has(p, SOURCE) + ", target " + has(p, TARGET));
            helper.assertTrue(has(p, RESTORE_A) && has(p, RESTORE_B), "the two skills were not given back");
            helper.assertTrue(p.receivedOnActionBar(NOT_READY), "the refusal line: " + p.heard());
            helper.assertTrue(p.received(RESTORED), "the player was not told what came back: " + p.heard());
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusalGivesBackOnlyWhatIsMissing(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer partly = player(helper);
        RecordingPlayer fully = player(helper);
        try {
            RaphaelMerge.override(stand(() -> true));
            FoxAblazeProbe.ownedHook = x -> true;
            give(partly, RESTORE_A, 50);
            EvoGrant.learnFree(partly, skill(SOURCE));
            helper.assertTrue(SkillAPI.getSkillsFrom(partly).getSkill(skill(RESTORE_A)).orElseThrow().getMastery() == 50, "a skill that was still held lost its mastery");
            helper.assertTrue(has(partly, RESTORE_B), "the missing one was not given back");
            helper.assertTrue(partly.received(RESTORED), "something came back and nothing was said");
            give(fully, RESTORE_A, 50);
            give(fully, RESTORE_B, 70);
            EvoGrant.learnFree(fully, skill(SOURCE));
            helper.assertTrue(SkillAPI.getSkillsFrom(fully).getSkill(skill(RESTORE_B)).orElseThrow().getMastery() == 70, "a held skill lost its mastery");
            helper.assertTrue(!fully.received(RESTORED), "nothing came back but the player was told it did: " + fully.heard());
        } finally {
            clean(helper, partly, fully);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVetoedRedirectReadsTheSameAsAnOwnedOne(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer owned = player(helper);
        RecordingPlayer vetoed = player(helper);
        SkillEvents.UnlockSkillEvent v = null;
        try {
            RaphaelMerge.override(stand(() -> true));
            FoxAblazeProbe.ownedHook = x -> x == owned;
            EvoGrant.learnFree(owned, skill(SOURCE));
            v = veto(TARGET);
            EvoGrant.learnFree(vetoed, skill(SOURCE));
            helper.assertTrue(!has(vetoed, TARGET) && !has(vetoed, SOURCE), "a vetoed redirect left a Raphael");
            helper.assertTrue(has(vetoed, RESTORE_A) && has(vetoed, RESTORE_B), "a vetoed redirect did not give the two skills back");
            helper.assertTrue(owned.heard().equals(vetoed.heard()), "the two causes read differently: owned [" + owned.heard() + "] vetoed [" + vetoed.heard() + "]");
        } finally {
            if (v != null) {
                SkillEvents.UNLOCK_SKILL.unregister(v);
            }
            clean(helper, owned, vetoed);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void holdingEitherRaphaelBlocksTheLearnWithoutAWord(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer holder = player(helper);
        RecordingPlayer holderOfSource = player(helper);
        AtomicBoolean on = new AtomicBoolean(false);
        try {
            RaphaelMerge.override(stand(on::get));
            give(holder, TARGET, 10);
            on.set(true);
            String before = holder.heard();
            helper.assertTrue(!EvoGrant.learnFree(holder, skill(SOURCE)), "the second Raphael was learned");
            helper.assertTrue(!has(holder, SOURCE) && has(holder, TARGET), "after the blocked learn: source " + has(holder, SOURCE) + ", target " + has(holder, TARGET));
            helper.assertTrue(holder.heard().equals(before), "a blocked learn said something: " + holder.heard());
            on.set(false);
            helper.assertTrue(EvoGrant.learnFree(holderOfSource, skill(SOURCE)), "setup: the source was not learned with the merge off");
            on.set(true);
            FoxAblazeProbe.ownedHook = x -> true;
            helper.assertTrue(!EvoGrant.learnFree(holderOfSource, skill(SOURCE)), "a learn of what is already held was reported as a success");
            helper.assertTrue(!holderOfSource.received(RESTORED) && !holderOfSource.receivedOnActionBar(NOT_READY), "the owner's answer was asked of a player who holds a Raphael: " + holderOfSource.heard());
        } finally {
            clean(helper, holder, holderOfSource);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutTheOtherAddonNothingIsRedirected(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        try {
            RaphaelMerge.override(stand(() -> false));
            FoxAblazeProbe.ownedHook = x -> true;
            helper.assertTrue(EvoGrant.learnFree(p, skill(SOURCE)), "the source was not learned as itself");
            helper.assertTrue(has(p, SOURCE) && !has(p, TARGET), "source " + has(p, SOURCE) + ", target " + has(p, TARGET));
            helper.assertTrue(!has(p, RESTORE_A) && !has(p, RESTORE_B), "something was given back with the merge off");
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetThatDoesNotExistPassesTheLearnThrough(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        try {
            RaphaelMerge.override(new RaphaelMerge.Merge(id(SOURCE), id("tensura:there_is_no_such_skill"), List.of(id(SOURCE)), List.of(id(RESTORE_A)), () -> true));
            helper.assertTrue(EvoGrant.learnFree(p, skill(SOURCE)), "a merge with no target blocked the learn");
            helper.assertTrue(has(p, SOURCE) && !has(p, RESTORE_A), "source " + has(p, SOURCE));
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginConvertsAHeldSource(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        AtomicBoolean on = new AtomicBoolean(false);
        try {
            RaphaelMerge.override(stand(on::get));
            helper.assertTrue(EvoGrant.learnFree(p, skill(SOURCE)), "setup: the source was not learned with the merge off");
            on.set(true);
            RaphaelMerge.sweep(p);
            helper.assertTrue(has(p, TARGET) && !has(p, SOURCE), "after the sweep: source " + has(p, SOURCE) + ", target " + has(p, TARGET));
            helper.assertTrue(EnergyHelper.getBaseMaxEP(p) == EP, "the conversion charged EP");
        } finally {
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginForgetsTheOldOneOnlyAfterTheNewOneIsLearned(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer p = player(helper);
        AtomicBoolean on = new AtomicBoolean(false);
        SkillEvents.UnlockSkillEvent v = null;
        try {
            RaphaelMerge.override(stand(on::get));
            EvoGrant.learnFree(p, skill(SOURCE));
            on.set(true);
            v = veto(TARGET);
            RaphaelMerge.sweep(p);
            helper.assertTrue(has(p, SOURCE) && !has(p, TARGET), "a vetoed conversion left the player without a Raphael: source " + has(p, SOURCE) + ", target " + has(p, TARGET));
        } finally {
            if (v != null) {
                SkillEvents.UNLOCK_SKILL.unregister(v);
            }
            clean(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginWithBothHeldDropsTheSourceAndLeavesASourceWhoseTargetIsOwned(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer both = player(helper);
        RecordingPlayer blocked = player(helper);
        AtomicBoolean on = new AtomicBoolean(false);
        try {
            RaphaelMerge.override(stand(on::get));
            give(both, SOURCE, 10);
            give(both, TARGET, 10);
            give(blocked, SOURCE, 10);
            on.set(true);
            FoxAblazeProbe.ownedHook = x -> true;
            RaphaelMerge.sweep(both);
            helper.assertTrue(has(both, TARGET) && !has(both, SOURCE), "both held: source " + has(both, SOURCE) + ", target " + has(both, TARGET));
            RaphaelMerge.sweep(blocked);
            helper.assertTrue(has(blocked, SOURCE) && !has(blocked, TARGET), "the target is owned elsewhere: source " + has(blocked, SOURCE) + ", target " + has(blocked, TARGET));
        } finally {
            clean(helper, both, blocked);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRedirectIsPerPlayer(GameTestHelper helper) {
        if (!open(helper)) {
            return;
        }
        RecordingPlayer first = player(helper);
        RecordingPlayer second = player(helper);
        try {
            RaphaelMerge.override(stand(() -> true));
            FoxAblazeProbe.ownedHook = x -> x == second;
            EvoGrant.learnFree(first, skill(SOURCE));
            EvoGrant.learnFree(second, skill(SOURCE));
            helper.assertTrue(has(first, TARGET) && !has(first, RESTORE_A), "the free player: target " + has(first, TARGET));
            helper.assertTrue(!has(second, TARGET) && has(second, RESTORE_A), "the refused player: target " + has(second, TARGET));
        } finally {
            clean(helper, first, second);
        }
        helper.succeed();
    }
}
