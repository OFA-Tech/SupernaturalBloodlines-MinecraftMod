package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity;
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
}
