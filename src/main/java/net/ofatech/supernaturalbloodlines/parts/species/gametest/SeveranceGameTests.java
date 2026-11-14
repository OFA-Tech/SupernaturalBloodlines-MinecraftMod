package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/** What a severed bond (R101b) turns off and what it leaves alone. The rite itself is in {@link SeveranceRiteGameTests}. */
@PrefixGameTestTemplate(false)
public final class SeveranceGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SeveranceGameTests() {}

    static void sever(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        BloodlineAccess.set(p, s.toBuilder().severedSire(s.sire()).build());
    }

    private static boolean hurts(ServerPlayer attacker, ServerPlayer victim) {
        float before = victim.getHealth();
        victim.hurt(attacker.damageSources().playerAttack(attacker), 5f);
        boolean hurt = victim.getHealth() < before;
        victim.setHealth(victim.getMaxHealth());
        return hurt;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedHybridCanHurtTheSire(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.createDuelist(helper);
        ServerPlayer hybrid = TestPlayers.createDuelist(helper);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).sire(sire.getUUID()).build());
            helper.assertTrue(!hurts(hybrid, sire), "control: a bound hybrid hurt the sire");
            sever(hybrid);
            helper.assertTrue(hurts(hybrid, sire), "a severed hybrid still cannot hurt the sire");
            helper.assertTrue(sire.getUUID().equals(BloodlineAccess.get(hybrid).sire()), "the sire was forgotten");
        } finally {
            BloodlineAccess.set(hybrid, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedSiredIsNotCalled(GameTestHelper helper) {
        ServerPlayer caller = SirelineCommandGameTests.named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = SirelineCommandGameTests.named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            SirelineCommandGameTests.online(caller, sired);
            SirelineCommandGameTests.species(caller, Species.ORIGINAL);
            SirelineCommandGameTests.Capture bound = new SirelineCommandGameTests.Capture();
            helper.assertTrue(SirelineCommandGameTests.run(caller, 0, bound, "bloodline call") == 1, "control: the call failed: " + bound.text());

            caller.removeData(SpeciesRegistries.LAST_CALL);
            sever(sired);
            SirelineCommandGameTests.Capture severed = new SirelineCommandGameTests.Capture();
            helper.assertTrue(SirelineCommandGameTests.run(caller, 0, severed, "bloodline call") == 0 && severed.text().contains("nobody"),
                    "a severed sired was called: " + severed.text());
            helper.assertTrue(!caller.hasData(SpeciesRegistries.LAST_CALL), "calling nobody cost a cooldown");

            // a request sent before the severance is not honoured either
            BloodlineAccess.set(sired, BloodlineAccess.get(sired).toBuilder().severedSire(null).build());
            String token = SireCall.requests().open(caller.getUUID(), sired.getUUID(), helper.getLevel().getGameTime());
            sever(sired);
            SireCall.Result late = SireCall.accept(sired, token);
            helper.assertTrue(late.error() != null && late.players() == 0, "a severed sired accepted an old call");
        } finally {
            SirelineCommandGameTests.cleanUp(caller, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedKeepsTheTreeAndTheIndex(GameTestHelper helper) {
        ServerPlayer sire = SirelineCommandGameTests.named(helper, helper.getLevel(), "sire");
        ServerPlayer child = SirelineCommandGameTests.named(helper, helper.getLevel(), "child");
        try {
            SireRecorder.record(sire, child);
            sever(child);
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            helper.assertTrue(sire.getUUID().equals(index.sireOf(child.getUUID()).orElse(null)), "the index forgot the sire");
            helper.assertTrue(index.childrenOf(sire.getUUID()).stream().anyMatch(n -> n.id().equals(child.getUUID())), "the tree lost the child");
            SirelineCommandGameTests.Capture tree = new SirelineCommandGameTests.Capture();
            SirelineCommandGameTests.run(child, 0, tree, "bloodline tree");
            helper.assertTrue(tree.text().contains("tree.sired_by"), "the child's tree no longer shows the sire: " + tree.text());
        } finally {
            SirelineCommandGameTests.cleanUp(sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNewSireEndsTheSeverance(GameTestHelper helper) {
        ServerPlayer first = TestPlayers.createDuelist(helper);
        ServerPlayer second = TestPlayers.createDuelist(helper);
        ServerPlayer hybrid = TestPlayers.createDuelist(helper);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(SireRecorder.record(first, hybrid), "the first sire was not recorded");
            sever(hybrid);
            helper.assertTrue(!SireRecorder.record(first, hybrid) || BloodlineAccess.get(hybrid).bondBroken(), "the same sire re-sired a severed player and left the flag");
            BloodlineAccess.set(hybrid, BloodlineAccess.get(hybrid).toBuilder().severedSire(first.getUUID()).build());

            helper.assertTrue(SireRecorder.record(second, hybrid), "a severed player could not be sired again");
            BloodlineState s = BloodlineAccess.get(hybrid);
            helper.assertTrue(second.getUUID().equals(s.sire()) && !s.bondBroken(), "the flag survived a new sire: " + s.sire() + " " + s.bondBroken());
            helper.assertTrue(SirelineIndex.get(helper.getLevel()).sireOf(hybrid.getUUID()).map(second.getUUID()::equals).orElse(false), "the index did not follow");
            helper.assertTrue(!hurts(hybrid, second), "the new sire is not protected");
            helper.assertTrue(hurts(hybrid, first), "the old sire still is");
        } finally {
            SirelineCommandGameTests.cleanUp(first, second, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severedIsNotProtectedFromACompelledCreature(GameTestHelper helper) {
        ServerPlayer caster = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        ServerPlayer grandchild = TestPlayers.create(helper);
        try {
            SireRecorder.record(caster, child);
            SireRecorder.record(child, grandchild);
            helper.assertTrue(Compulsion.protects(caster.getUUID(), child) && Compulsion.protects(caster.getUUID(), grandchild), "control: the line is not protected");
            sever(child);
            helper.assertTrue(!Compulsion.protects(caster.getUUID(), child), "a severed child is still protected");
            helper.assertTrue(Compulsion.protects(caster.getUUID(), grandchild), "the grandchild lost the protection with the child's severance");
            helper.assertTrue(Compulsion.protects(caster.getUUID(), caster), "the caster is not safe from their own creature");
        } finally {
            SirelineCommandGameTests.cleanUp(caster, child, grandchild);
        }
        helper.succeed();
    }
}
