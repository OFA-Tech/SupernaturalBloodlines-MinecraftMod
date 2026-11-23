package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhase;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanHostEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MorphCues;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ShiftBody;

/**
 * UI overhaul phase 7: the server's side of the morph (the cues by tick, the hit window, the standing still, what is synced). The client draws a function of the
 * synced form and ticks, so what is pinned here is that those two numbers alone decide everything. The three shifters share one set of tests.
 */
@PrefixGameTestTemplate(false)
public final class MorphGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private MorphGameTests() {
    }

    /** One boss that shifts, the way the director makes it, and how to start its shift. */
    private record Shifter(String name, Mob mob, ShiftBody body, Runnable start) {
    }

    private static boolean skipped(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("bosses")) {
            helper.succeed(); // the bosses part is off: nothing of the morph is loaded
            return true;
        }
        return false;
    }

    private static List<Shifter> shifters(GameTestHelper helper, BossTestRig rig) {
        BossDirector.useMods(b -> true);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        List<Shifter> all = new ArrayList<>();
        KlausEntity klaus = rig.spawnKlaus();
        all.add(new Shifter("klaus", klaus, klaus, klaus::startShift));
        DeucalionEntity d = BossDirector.spawnDeucalion(helper.getLevel(), null, at.offset(0, 0, 12)).orElseThrow(); // far apart: bosses push each other
        all.add(new Shifter("deucalion", d, d, d::startShift));
        PeterHaleEntity p = BossDirector.spawnPeterAt(helper.getLevel(), at.offset(0, 0, -12)).orElseThrow();
        all.add(new Shifter("peter", p, p, p::startShift));
        return all;
    }

    private static void run(Mob e, int ticks) {
        for (int i = 0; i < ticks && !e.isRemoved(); i++) {
            e.tickCount++;
            e.tick();
        }
    }

    private static void clear(List<Shifter> all) {
        MorphCues.observe(null);
        KlausEntity.observeSwap(null);
        for (Shifter s : all) {
            if (s.mob() instanceof DeucalionEntity d) {
                d.dismissPack();
            }
        }
    }

    // ---- T2: the cues ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cuesRunOnTheirTicks(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                List<String> seen = new ArrayList<>();
                MorphCues.observe((boss, tick, cues) -> {
                    if (boss == s.mob()) {
                        seen.add(tick + ":" + cues);
                    }
                });
                s.start().run();
                run(s.mob(), ShiftRules.END_TICK);
                helper.assertTrue(seen.equals(List.of("0:1", "4:2", "7:4", "8:2", "12:2", "14:4", "16:2", "32:8")), s.name() + " played " + seen);
                helper.assertTrue(s.body().form() == BossForm.BEAST, s.name() + " did not finish the shift");
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noCuesAfterDeath(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                List<Integer> ticks = new ArrayList<>();
                MorphCues.observe((boss, tick, cues) -> {
                    if (boss == s.mob()) {
                        ticks.add(tick);
                    }
                });
                s.start().run();
                run(s.mob(), 10);
                helper.assertTrue(ticks.equals(List.of(0, 4, 7, 8)), s.name() + ": control, before the death " + ticks);
                s.mob().setHealth(0.0F);
                run(s.mob(), 30);
                helper.assertTrue(ticks.equals(List.of(0, 4, 7, 8)), s.name() + " played a cue after it died: " + ticks);
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void desiccatedKlausPlaysNoCues(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            List<Integer> ticks = new ArrayList<>();
            MorphCues.observe((boss, tick, cues) -> ticks.add(tick));
            k.startShift();
            run(k, 5);
            int before = ticks.size();
            k.desiccate();
            run(k, 40);
            helper.assertTrue(ticks.size() == before, "a fallen Klaus played cues: " + ticks);
            k.recover();
            run(k, 40);
            helper.assertTrue(ticks.size() > before, "control: he did not go on with the cues once he got up");
        } finally {
            MorphCues.observe(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShiftTwiceInOneLifeIsImpossible(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                List<Integer> ticks = new ArrayList<>();
                MorphCues.observe((boss, tick, cues) -> {
                    if (boss == s.mob()) {
                        ticks.add(tick);
                    }
                });
                s.start().run();
                run(s.mob(), ShiftRules.END_TICK);
                int played = ticks.size();
                s.start().run();
                helper.assertTrue(s.body().form() == BossForm.BEAST, s.name() + ": the form changed on a second start: " + s.body().form());
                run(s.mob(), ShiftRules.END_TICK);
                helper.assertTrue(ticks.size() == played, s.name() + " played the cues of a second shift: " + ticks);
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    // ---- T3: the hit window, the standing still, what is synced ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hittableAtEveryTickOfTheShift(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            ServerPlayer hitter = TestPlayers.create(helper);
            for (Shifter s : all) {
                LivingEntity e = s.mob();
                e.setHealth(e.getMaxHealth());
                s.start().run();
                int at = 0;
                for (int tick : new int[] {0, 19, 20, 39}) {
                    run(s.mob(), tick - at);
                    at = tick;
                    helper.assertTrue(s.body().shiftTicks() == tick, s.name() + ": the counter is " + s.body().shiftTicks() + ", not " + tick);
                    for (net.minecraft.world.damagesource.DamageSource by : List.of(hitter.damageSources().playerAttack(hitter), helper.getLevel().damageSources().magic())) {
                        float before = e.getHealth();
                        e.invulnerableTime = 0;
                        e.hurt(by, 3.0F);
                        helper.assertTrue(e.getHealth() < before, s.name() + " took no damage from " + by.getMsgId() + " at tick " + tick);
                        e.setHealth(e.getMaxHealth());
                    }
                }
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void standsStillForTheWholeShift(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                s.start().run();
                run(s.mob(), 1);
                Vec3 at = s.mob().position();
                s.mob().setDeltaMovement(0.4, 0.0, 0.4); // a push (a hit, a gust): the shift still holds him where he is
                run(s.mob(), ShiftRules.SHIFT_TICKS - 2);
                double along = Math.hypot(s.mob().getX() - at.x, s.mob().getZ() - at.z); // along the ground: a body that is stuck in a block is lifted out of it, which is the game's, not the shift's
                helper.assertTrue(along < 0.5, s.name() + " moved " + along + " blocks along the ground during the shift");
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void clientTwinSeesTheSameBody(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                Mob twin = (Mob) s.mob().getType().create(helper.getLevel());
                s.start().run();
                for (int t = 0; t <= ShiftRules.SHIFT_TICKS; t++) {
                    twin.getEntityData().assignValues(s.mob().getEntityData().getNonDefaultValues());
                    ShiftBody seen = (ShiftBody) twin;
                    helper.assertTrue(seen.form() == s.body().form() && seen.shiftTicks() == s.body().shiftTicks(), s.name() + " tick " + t + ": the twin has " + seen.form() + " " + seen.shiftTicks());
                    helper.assertTrue(ShiftRules.showsBeast(seen.form(), seen.shiftTicks()) == ShiftRules.showsBeast(s.body().form(), s.body().shiftTicks()), s.name() + " tick " + t + ": a different body");
                    helper.assertTrue(twin.getBbWidth() == s.mob().getBbWidth() && twin.getBbHeight() == s.mob().getBbHeight(), s.name() + " tick " + t + ": a different hitbox");
                    run(s.mob(), 1);
                }
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void morphSurvivesSaveAt10And25And39(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                s.start().run();
                int at = 0;
                for (int tick : new int[] {10, 25, 39}) {
                    run(s.mob(), tick - at);
                    at = tick;
                    CompoundTag tag = new CompoundTag();
                    s.mob().saveWithoutId(tag);
                    Mob again = (Mob) s.mob().getType().create(helper.getLevel());
                    again.load(tag);
                    ShiftBody back = (ShiftBody) again;
                    helper.assertTrue(back.form() == BossForm.SHIFTING && back.shiftTicks() == tick, s.name() + " saved at " + tick + " came back as " + back.form() + " " + back.shiftTicks());
                    helper.assertTrue(ShiftRules.showsBeast(back.form(), back.shiftTicks()) == (tick >= ShiftRules.SWAP_TICK), s.name() + " at " + tick + ": the wrong body");
                    helper.assertTrue(tick >= ShiftRules.SWAP_TICK ? again.getBbWidth() > 0.6F : again.getBbWidth() <= 0.6F, s.name() + " at " + tick + ": a man with the wolf's box, or the reverse: " + again.getBbWidth());
                }
            }
            // the swap still happens exactly once when Klaus is loaded before it (his observer sees it)
            KlausEntity k = rig.spawnKlaus();
            k.startShift();
            run(k, 10);
            CompoundTag tag = new CompoundTag();
            k.saveWithoutId(tag);
            KlausEntity again = KlausEntity.create(helper.getLevel());
            again.load(tag);
            helper.getLevel().addFreshEntity(again);
            List<Integer> swaps = new ArrayList<>();
            KlausEntity.observeSwap(e -> {
                if (e == again) {
                    swaps.add(e.shiftTicks());
                }
            });
            run(again, 40);
            again.discard();
            helper.assertTrue(swaps.equals(List.of(ShiftRules.SWAP_TICK)), "the swap after a load happened at " + swaps);
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    // ---- T4: the Beast changes like the others ----

    /** The Host, changed into the Beast the way the director does it, and not yet ticked: the Beast is a man at tick 0. */
    private static BeastEntity changed(GameTestHelper helper, BossTestRig rig) {
        BossTestRig.fixedGround();
        BeastDirector.spawnHost(helper.getLevel(), null, helper.absolutePos(new BlockPos(4, 1, 4))).orElseThrow();
        helper.assertTrue(BeastDirector.change(helper.getLevel()), "the Host did not change");
        return (BeastEntity) BossDirector.anyBossBody(rig.server, BossId.BEAST_OF_GEVAUDAN);
    }

    private static boolean box(BeastEntity b, float width, float height) {
        return Math.abs(b.getBbWidth() - width) < 1e-4 && Math.abs(b.getBbHeight() - height) < 1e-4;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void changeStillMakesTheBeastAtOnce(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            helper.assertTrue(b != null && b.isAlive(), "no Beast");
            var entry = rig.index.of(BossId.BEAST_OF_GEVAUDAN);
            helper.assertTrue(entry.phase == BeastPhase.BEAST, "the phase is " + entry.phase);
            helper.assertTrue(b.getUUID().equals(entry.entity), "the index has not adopted the Beast");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(GevaudanHostEntity.class, b.getBoundingBox().inflate(16)).isEmpty(), "the Host is still there");
            helper.assertTrue(b.form() == BossForm.SHIFTING && b.shiftTicks() == 0, "the change did not begin: " + b.form() + " " + b.shiftTicks());
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastIsAManUntilTheSwap(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            helper.assertTrue(box(b, 0.6F, 1.95F), "at tick 0 the box is " + b.getBbWidth() + " by " + b.getBbHeight());
            run(b, ShiftRules.SWAP_TICK - 1);
            helper.assertTrue(b.shiftTicks() == 19 && box(b, 0.6F, 1.95F), "at tick 19 it is " + b.shiftTicks() + ": " + b.getBbWidth() + " by " + b.getBbHeight());
            run(b, 1);
            helper.assertTrue(b.shiftTicks() == 20 && box(b, 2.4F, 3.0F), "at tick 20 it is " + b.shiftTicks() + ": " + b.getBbWidth() + " by " + b.getBbHeight());
            run(b, ShiftRules.SHIFT_TICKS - 20);
            helper.assertTrue(b.form() == BossForm.BEAST && box(b, 2.4F, 3.0F), "after the shift: " + b.form());
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastHoldsStill(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            run(b, 1);
            Vec3 at = b.position();
            b.setDeltaMovement(0.4, 0.0, 0.4);
            run(b, ShiftRules.SHIFT_TICKS - 2);
            helper.assertTrue(Math.hypot(b.getX() - at.x, b.getZ() - at.z) < 0.5, "the Beast moved " + Math.hypot(b.getX() - at.x, b.getZ() - at.z) + " blocks while it changed");
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noRoarWhileItChanges(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        ServerPlayer victim = TestPlayers.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            victim.setPos(b.getX() + 3.0, b.getY(), b.getZ());
            b.setTarget(victim);
            helper.assertTrue(b.roarReady(helper.getLevel().getGameTime()), "control: a new Beast may roar");
            for (int i = 0; i < ShiftRules.SHIFT_TICKS; i++) {
                run(b, 1);
                b.setTarget(victim);
                helper.assertTrue(b.roarTicks() == 0, "it began to Roar at tick " + b.shiftTicks() + " of the change");
            }
            b.roarNow(helper.getLevel().getGameTime());
            helper.assertTrue(b.roarTicks() == 1, "control: it may Roar when the change is over");
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void roarNowRefusesWhileItChanges(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            b.roarNow(helper.getLevel().getGameTime());
            helper.assertTrue(b.roarTicks() == 0, "a Roar began in the middle of the change");
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastIsHittableWhileItChanges(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            ServerPlayer hitter = TestPlayers.create(helper);
            int at = 0;
            for (int tick : new int[] {0, 19, 20, 39}) {
                run(b, tick - at);
                at = tick;
                for (net.minecraft.world.damagesource.DamageSource by : List.of(hitter.damageSources().playerAttack(hitter), helper.getLevel().damageSources().magic())) {
                    b.setHealth(b.getMaxHealth());
                    float before = b.getHealth();
                    b.invulnerableTime = 0;
                    b.hurt(by, 3.0F);
                    helper.assertTrue(b.getHealth() < before, "the Beast took no damage from " + by.getMsgId() + " at tick " + tick);
                }
            }
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBeastWithNoMorphTagsIsWhole(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            b.completeMorph();
            CompoundTag tag = new CompoundTag();
            b.saveWithoutId(tag);
            tag.remove("form");
            tag.remove("shift_ticks");
            BeastEntity old = BeastEntity.create(helper.getLevel());
            old.load(tag);
            helper.assertTrue(old.form() == BossForm.BEAST && old.shiftTicks() == ShiftRules.END_TICK && box(old, 2.4F, 3.0F), "an old save is " + old.form() + " " + old.shiftTicks() + " " + old.getBbWidth() + " by " + old.getBbHeight());
            BeastEntity fresh = BeastEntity.create(helper.getLevel()); // a /summon or a dev shot
            helper.assertTrue(fresh.form() == BossForm.BEAST && fresh.shiftTicks() == ShiftRules.END_TICK && box(fresh, 2.4F, 3.0F), "a new Beast is " + fresh.form());
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void collapseFinishesTheMorph(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            run(b, 10);
            helper.assertTrue(b.form() == BossForm.SHIFTING, "control: still changing");
            BeastDirector.collapse(helper.getLevel(), b);
            helper.assertTrue(b.collapsing(), "it did not collapse");
            helper.assertTrue(b.form() == BossForm.BEAST && b.shiftTicks() == ShiftRules.END_TICK && box(b, 2.4F, 3.0F), "a collapsing Beast is still a man: " + b.form() + " " + b.shiftTicks());
            helper.assertTrue(b.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the hat is still on");
            run(b, 5);
            helper.assertTrue(b.shiftTicks() == ShiftRules.END_TICK, "the counter went on while it fell");
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostsHatComesOffAtTheSwap(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            helper.assertTrue(!b.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the Host's hat was not copied to the Beast");
            run(b, ShiftRules.SWAP_TICK - 1);
            helper.assertTrue(!b.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the hat came off before the swap");
            run(b, 1);
            helper.assertTrue(b.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the hat is still on after the swap");
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void savedMidChangeResumes(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            int at = 0;
            for (int tick : new int[] {10, 25, 39}) {
                run(b, tick - at);
                at = tick;
                CompoundTag tag = new CompoundTag();
                b.saveWithoutId(tag);
                BeastEntity back = BeastEntity.create(helper.getLevel());
                back.load(tag);
                helper.assertTrue(back.form() == BossForm.SHIFTING && back.shiftTicks() == tick, "saved at " + tick + " it came back as " + back.form() + " " + back.shiftTicks());
                helper.assertTrue(box(back, tick >= ShiftRules.SWAP_TICK ? 2.4F : 0.6F, tick >= ShiftRules.SWAP_TICK ? 3.0F : 1.95F), "saved at " + tick + " the box is " + back.getBbWidth() + " by " + back.getBbHeight());
            }
            // loaded before the swap, the swap still happens exactly once
            BeastEntity early = changed2(helper, rig);
            List<Integer> swaps = new ArrayList<>();
            BeastEntity.observeSwap(e -> swaps.add(e.shiftTicks()));
            run(early, 40);
            helper.assertTrue(swaps.equals(List.of(ShiftRules.SWAP_TICK)), "the swap happened at " + swaps);
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    /** A second change after the first one is done: a returning Host (the phase allows it) or, here, the same Beast saved at tick 10 and loaded. */
    private static BeastEntity changed2(GameTestHelper helper, BossTestRig rig) {
        BeastEntity b = BeastEntity.create(helper.getLevel());
        b.moveTo(helper.absoluteVec(new Vec3(8.5, 1, 8.5)));
        b.beginMorph();
        helper.getLevel().addFreshEntity(b);
        run(b, 10);
        CompoundTag tag = new CompoundTag();
        b.saveWithoutId(tag);
        b.discard();
        BeastEntity back = BeastEntity.create(helper.getLevel());
        back.load(tag);
        helper.getLevel().addFreshEntity(back);
        return back;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastPlaysTheSameCuesAndAClientTwinSeesTheSameBody(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            List<String> seen = new ArrayList<>();
            MorphCues.observe((boss, tick, cues) -> {
                if (boss == b) {
                    seen.add(tick + ":" + cues);
                }
            });
            BeastEntity twin = BeastEntity.create(helper.getLevel());
            // what a client gets: the values that are not the defaults when it first sees the entity, then what changed each tick (a form that goes back to the default, BEAST, only comes this way)
            twin.getEntityData().assignValues(b.getEntityData().getNonDefaultValues());
            for (int t = 0; t <= ShiftRules.SHIFT_TICKS; t++) {
                var changed = b.getEntityData().packDirty();
                if (changed != null) {
                    twin.getEntityData().assignValues(changed);
                }
                helper.assertTrue(twin.form() == b.form() && twin.shiftTicks() == b.shiftTicks(), "tick " + t + ": the twin has " + twin.form() + " " + twin.shiftTicks());
                helper.assertTrue(twin.getBbWidth() == b.getBbWidth() && twin.getBbHeight() == b.getBbHeight(), "tick " + t + ": a different box");
                run(b, 1);
            }
            helper.assertTrue(seen.equals(List.of("0:1", "4:2", "7:4", "8:2", "12:2", "14:4", "16:2", "32:8")), "the Beast played " + seen);
        } finally {
            MorphCues.observe(null);
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }

    // ---- the boss bar follows the form (phase 6 sends it; this phase does not add a field) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarShowsTheForm(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        List<Shifter> all = List.of();
        try (BossTestRig rig = new BossTestRig(helper)) {
            all = shifters(helper, rig);
            for (Shifter s : all) {
                var boss = (net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc) s.mob();
                BossBars.update(boss, List.of());
                helper.assertTrue(BossBars.sentOf(boss.getUUID()).look().equals(BossBarForms.shifter(BossForm.HUMAN)), s.name() + ": a man before the shift: " + BossBars.sentOf(boss.getUUID()));
                s.start().run();
                BossBars.update(boss, List.of());
                helper.assertTrue(BossBars.sentOf(boss.getUUID()).look().shifting(), s.name() + ": the chip is not Shifting at tick 0: " + BossBars.sentOf(boss.getUUID()));
                run(s.mob(), ShiftRules.SHIFT_TICKS - 1);
                BossBars.update(boss, List.of());
                helper.assertTrue(BossBars.sentOf(boss.getUUID()).look().shifting(), s.name() + ": the chip left Shifting before tick 40");
                run(s.mob(), 1);
                BossBars.update(boss, List.of());
                helper.assertTrue(BossBars.sentOf(boss.getUUID()).look().equals(BossBarForms.shifter(BossForm.BEAST)), s.name() + ": the chip is not Beast at tick 40: " + BossBars.sentOf(boss.getUUID()));
            }
        } finally {
            clear(all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void barChipsHostThenBeastThenSpirit(GameTestHelper helper) {
        if (skipped(helper)) {
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            BeastEntity b = changed(helper, rig);
            helper.assertTrue(b.barLook().equals(BossBarForms.host(0)), "the man is the Host: " + b.barLook());
            run(b, ShiftRules.SWAP_TICK - 1);
            helper.assertTrue(b.barLook().equals(BossBarForms.host(0)), "tick 19 is still the Host: " + b.barLook());
            run(b, 1);
            helper.assertTrue(b.barLook().equals(BossBarForms.host(1)), "tick 20 is the Beast: " + b.barLook());
            BeastDirector.collapse(helper.getLevel(), b);
            helper.assertTrue(b.barLook().equals(BossBarForms.host(2)), "a falling Beast is the Spirit: " + b.barLook());
            // and the bar notices the change on the tick it happens, not on the next pass
            BeastEntity again = changed2(helper, rig);
            BossBars.update(again, List.of());
            helper.assertTrue(BossBars.sentOf(again.getUUID()).look().equals(BossBarForms.host(0)), "the sent look is " + BossBars.sentOf(again.getUUID()));
        } finally {
            BeastEntity.observeSwap(null);
        }
        helper.succeed();
    }
}