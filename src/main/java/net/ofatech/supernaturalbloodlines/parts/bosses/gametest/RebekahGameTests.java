package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.CureMadeEvent;
import net.ofatech.supernaturalbloodlines.core.boss.RebekahRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Rebekah and the True Cure (plan 17d, R169 and R171): the Cure that sends her, her hunt, and the cure an Original can drink. */
@PrefixGameTestTemplate(false)
public final class RebekahGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String UNKNOWN = "supernaturalbloodlines.boss.notice.rebekah.unknown";
    private static final String HINT = "supernaturalbloodlines.boss.notice.rebekah.hint";
    private static final String GONE = "supernaturalbloodlines.boss.rebekah.gone";

    private RebekahGameTests() {
    }

    private static LineTestPlayer caster(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5)));
        return p;
    }

    private static void world(LineTestPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static RebekahEntity rebekahOf(BossTestRig rig) {
        return BossDirector.bossEntity(rig.server, BossId.REBEKAH) instanceof RebekahEntity r ? r : null;
    }

    private static void cureMadeBy(GameTestHelper helper, LineTestPlayer p) {
        NeoForge.EVENT_BUS.post(new CureMadeEvent(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), p.getUUID()));
    }

    // ---- the Cure that sends her ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCureMadeAtTheRiteSendsHerAfterTheDelayForWhoeverMadeItWithoutHerName(GameTestHelper helper) {
        LineTestPlayer p = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(p);
            cureMadeBy(helper, p);
            BossIndex17d e = new BossIndex17d(rig);
            helper.assertTrue(e.waiting() && rig.index.of(BossId.REBEKAH).target.equals(p.getUUID()), "the Cure did not put her on her way");
            BossDirector.check(rig.server);
            helper.assertTrue(rebekahOf(rig) == null, "she came before the delay");
            BossClock.offsetForTests(BossesConfig.rebekahDelaySeconds() * 20L + 5);
            BossDirector.check(rig.server);
            RebekahEntity r = rebekahOf(rig);
            helper.assertTrue(r != null && r.isAlive() && p.getUUID().equals(r.prey()), "she did not come for whoever made the Cure");
            helper.assertTrue(rig.index.state(BossId.REBEKAH) == BossState.ACTIVE && !e.waiting(), "the index says " + rig.index.state(BossId.REBEKAH));
            double d = Math.sqrt(r.distanceToSqr(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5))));
            helper.assertTrue(d <= RebekahRules.RADIUS + 2, "she came from " + d + " blocks from the circle");
            helper.assertTrue(p.received(UNKNOWN) && p.received(HINT), "the one she hunts was not told, or was told her name: " + p.heard());
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    /** Reads the arrival of the index. */
    private record BossIndex17d(BossTestRig rig) {
        boolean waiting() {
            return rig.index.of(BossId.REBEKAH).arriveAt >= 0;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCureThatWasGivenOrDroppedIsNoEventSoShesNotSent(GameTestHelper helper) {
        LineTestPlayer p = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(p);
            p.getInventory().add(new ItemStack(net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems.THE_CURE.get()));
            helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems.THE_CURE.get())));
            BossClock.offsetForTests(100_000);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt == -1 && rebekahOf(rig) == null, "a Cure that was given sent her");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sheDoesNotAnswerACureWhileFallenDeadOutThereSwitchedOffOrInAPeacefulWorld(GameTestHelper helper) {
        LineTestPlayer p = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(p);
            for (BossState state : new BossState[] {BossState.OPEN, BossState.DEAD, BossState.ACTIVE}) {
                rig.index.of(BossId.REBEKAH).state = state;
                cureMadeBy(helper, p);
                helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt == -1, "a Cure while she is " + state + " sent her");
            }
            rig.index.of(BossId.REBEKAH).state = BossState.UNSEEN;
            BossesConfig.override("rebekah_enabled", false);
            cureMadeBy(helper, p);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt == -1, "a Cure sent her although she is switched off");
            BossesConfig.override("rebekah_enabled", null);
            cureMadeBy(helper, p);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt >= 0, "control: the Cure did not send her");
            BossDirector.useDifficulty(l -> Difficulty.PEACEFUL);
            BossClock.offsetForTests(100_000);
            BossDirector.check(rig.server);
            helper.assertTrue(rebekahOf(rig) == null && rig.index.of(BossId.REBEKAH).arriveAt == -1, "she came into a peaceful world");
        } finally {
            BossesConfig.override("rebekah_enabled", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondCureWhileSheIsOnHerWayDoesNotMoveHerAndALostRebekahMayComeAgain(GameTestHelper helper) {
        LineTestPlayer first = caster(helper);
        LineTestPlayer second = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(first, second);
            cureMadeBy(helper, first);
            long at = rig.index.of(BossId.REBEKAH).arriveAt;
            cureMadeBy(helper, second);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt == at && first.getUUID().equals(rig.index.of(BossId.REBEKAH).target), "a second Cure moved her");
            rig.index.of(BossId.REBEKAH).arriveAt = -1;
            rig.index.of(BossId.REBEKAH).state = BossState.LOST;
            cureMadeBy(helper, second);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt >= 0 && second.getUUID().equals(rig.index.of(BossId.REBEKAH).target), "a lost Rebekah does not answer a Cure");
        } finally {
            forget(helper, first, second);
        }
        helper.succeed();
    }

    // ---- her hunt ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sheWithdrawsWhenNobodyIsLeftToHuntAndStaysWhileThePreyIsHere(GameTestHelper helper) {
        LineTestPlayer prey = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(prey);
            RebekahEntity r = rig.spawnRebekah();
            r.setNoAi(true);
            r.setInvulnerable(true); // thousands of ticks in a test structure: she must not suffocate in its floor
            r.hunt(prey.getUUID());
            for (int i = 0; i < RebekahRules.WITHDRAW_AFTER + 40; i++) {
                r.tickCount++;
                r.tick();
            }
            helper.assertTrue(!r.isRemoved(), "she gave up on a prey who is right there");
            r.hunt(UUID.randomUUID());
            for (int i = 0; i < RebekahRules.WITHDRAW_AFTER + 40 && !r.isRemoved(); i++) {
                r.tickCount++;
                r.tick();
            }
            helper.assertTrue(r.isRemoved() && rig.index.state(BossId.REBEKAH) == BossState.LOST, "she did not withdraw: " + rig.index.state(BossId.REBEKAH));
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whenSheGivesUpTheOneSheHuntedIsToldAndSheMayComeAgain(GameTestHelper helper) {
        LineTestPlayer prey = caster(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(prey);
            RebekahEntity r = rig.spawnRebekah();
            r.hunt(prey.getUUID());
            r.withdraw();
            helper.assertTrue(r.isRemoved() && rig.index.state(BossId.REBEKAH) == BossState.LOST, "she did not go: " + rig.index.state(BossId.REBEKAH));
            helper.assertTrue(prey.received(GONE), "the one she hunted was not told: " + prey.heard());
            cureMadeBy(helper, prey);
            helper.assertTrue(rig.index.of(BossId.REBEKAH).arriveAt >= 0, "a Rebekah who withdrew does not answer a new Cure");
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void herPreyAndHerWithdrawalSurviveASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            RebekahEntity r = rig.spawnRebekah();
            UUID prey = UUID.randomUUID();
            r.hunt(prey);
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            r.saveWithoutId(tag);
            RebekahEntity again = RebekahEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(prey.equals(again.prey()), "the prey was forgotten");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rebekahPaysTheTrueCureOnceWhenSheIsBroughtDown(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            RebekahEntity r = rig.spawnRebekah();
            r.desiccate();
            helper.assertTrue(rig.dropsNear(r, BossItems.THE_TRUE_CURE.get()) == 1, "no True Cure, or more than one: " + rig.dropsNear(r, BossItems.THE_TRUE_CURE.get()));
            r.recover();
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, r.getBoundingBox().inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
            r.desiccate();
            helper.assertTrue(rig.dropsNear(r, BossItems.THE_TRUE_CURE.get()) == 0, "a second True Cure was paid");
        }
        helper.succeed();
    }

    // ---- the True Cure ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTrueCureMakesAnOriginalHumanWithNoDesiccationAndTheirLineStays(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer p = TestPlayers.createMortal(helper);
            ServerPlayer child = TestPlayers.createMortal(helper);
            LineIndex index = LineIndex.get(helper.getLevel().getServer());
            try {
                helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
                index.setRoot(p.getUUID(), p.getUUID(), true);
                index.setRoot(child.getUUID(), p.getUUID(), true);
                ItemStack left = new ItemStack(BossItems.THE_TRUE_CURE.get()).finishUsingItem(helper.getLevel(), p);
                helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, s + " was not cured: " + BloodlineAccess.get(p));
                helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == null, s + " is still in " + VampirismAPI.factionPlayerHandler(p).getCurrentFaction());
                helper.assertTrue(left.is(Items.GLASS_BOTTLE), "the empty bottle did not come back: " + left);
                helper.assertTrue(p.getUUID().equals(index.rootOf(child.getUUID()).orElse(null)), "the ones they sired lost their line");
                helper.assertTrue(!index.isDead(p.getUUID()), "a cured Original was taken for a line death");
            } finally {
                index.remove(p.getUUID());
                index.remove(child.getUUID());
                BloodlineAccess.set(p, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTrueCureOnAHumanOnlyLosesThePotion(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ItemStack left = new ItemStack(BossItems.THE_TRUE_CURE.get()).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE && left.is(Items.GLASS_BOTTLE), "a human was changed or got no bottle back");
        helper.succeed();
    }
}