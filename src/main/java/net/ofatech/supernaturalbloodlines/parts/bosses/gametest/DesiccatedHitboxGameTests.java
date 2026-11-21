package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.LyingBody;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;

/**
 * R184: the box of a desiccated NPC Original is the one the lying body fills. The owner's playtest: the body was drawn lying down but the stake had to be
 * aimed where the Original would have been standing.
 */
@PrefixGameTestTemplate(false)
public final class DesiccatedHitboxGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final float[] YAWS = {0F, 30F, 45F, 90F, 135F, 200F, 270F, 315F};

    private DesiccatedHitboxGameTests() {
    }

    private static Map<String, Supplier<NpcOriginalBoss>> all(BossTestRig rig) {
        Map<String, Supplier<NpcOriginalBoss>> m = new LinkedHashMap<>();
        m.put("mikael", rig::spawn);
        m.put("klaus", rig::spawnKlaus);
        m.put("elijah", rig::spawnElijah);
        m.put("rebekah", rig::spawnRebekah);
        m.put("kol", rig::spawnKol);
        m.put("finn", rig::spawnFinn);
        return m;
    }

    /** A point of the lying body: {@code along} blocks from its middle toward the head, half way up the box. The renderer lays the head along (cos, sin) of the body yaw. */
    private static Vec3 onBody(NpcOriginalBoss e, double along) {
        double yaw = Math.toRadians(e.yBodyRot);
        return new Vec3(e.getX() + along * Math.cos(yaw), e.getY() + LyingBody.HEIGHT / 2.0, e.getZ() + along * Math.sin(yaw));
    }

    private static boolean hit(GameTestHelper helper, NpcOriginalBoss e, Vec3 aimAt) {
        Vec3 from = new Vec3(e.getX() + 3.5, e.getY() + 1.62, e.getZ() + 3.5);
        Vec3 to = from.add(aimAt.subtract(from).normalize().scale(10.0));
        // what ProjectileUtil.getEntityHitResult and the client's crosshair pick do with an entity's box
        return e.getBoundingBox().inflate(e.getPickRadius()).clip(from, to).isPresent();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedOriginalsBoxIsLowAndCoversTheBodyThatLies(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            for (var entry : all(rig).entrySet()) {
                NpcOriginalBoss e = entry.getValue().get();
                String who = entry.getKey();
                helper.assertTrue(e.getBbHeight() > 1.4F && e.getBbWidth() < 1.0F, who + ": control, the standing box is " + e.getBbWidth() + " x " + e.getBbHeight());
                e.desiccate();
                helper.assertTrue(e.isDesiccated(), who + ": setup, not desiccated");
                AABB box = e.getBoundingBox();
                helper.assertTrue(box.getYsize() < 0.8, who + ": the box is " + box.getYsize() + " tall, the body lies about half a block high");
                for (float yaw : YAWS) {
                    e.yBodyRot = yaw;
                    for (double along : new double[] {-0.85, 0.0, 0.85}) {
                        Vec3 p = onBody(e, along);
                        helper.assertTrue(box.contains(p), who + ": the body at " + along + " along, turned " + yaw + ", is outside the box " + box + " at " + p);
                    }
                }
                helper.assertTrue(e.getEyeHeight() < LyingBody.HEIGHT, who + ": the eyes are " + e.getEyeHeight() + " above the floor");
                e.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowAimedAtTheLyingBodyHitsAndOneAimedWhereHeStoodMisses(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            for (var entry : all(rig).entrySet()) {
                NpcOriginalBoss e = entry.getValue().get();
                String who = entry.getKey();
                Vec3 standingHead = e.position().add(0.0, 1.7, 0.0);
                helper.assertTrue(hit(helper, e, standingHead), who + ": control, a blow at a standing Original's head missed, box " + e.getBoundingBox() + " position " + e.position());
                e.desiccate();
                e.yBodyRot = 60F;
                for (double along : new double[] {-0.8, 0.0, 0.8}) {
                    helper.assertTrue(hit(helper, e, onBody(e, along)), who + ": a blow at the body " + along + " from its middle missed");
                }
                helper.assertTrue(!hit(helper, e, standingHead), who + ": a blow where he would have stood hit a body that lies");
                e.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBoxIsStandingAgainWhenTheOriginalGetsUp(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            for (var entry : all(rig).entrySet()) {
                NpcOriginalBoss e = entry.getValue().get();
                String who = entry.getKey();
                float width = e.getBbWidth();
                float height = e.getBbHeight();
                float eyes = e.getEyeHeight();
                e.desiccate();
                helper.assertTrue(e.getBbHeight() < 0.8F, who + ": setup, the box is " + e.getBbHeight() + " tall");
                e.recover();
                helper.assertTrue(!e.isDesiccated(), who + ": did not get up");
                helper.assertTrue(e.getBbWidth() == width && e.getBbHeight() == height && e.getEyeHeight() == eyes,
                        who + ": the box is " + e.getBbWidth() + " x " + e.getBbHeight() + " (eyes " + e.getEyeHeight() + "), standing was " + width + " x " + height + " (eyes " + eyes + ")");
                helper.assertTrue(hit(helper, e, e.position().add(0.0, 1.7, 0.0)), who + ": a blow at his head missed after he got up");
                e.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLyingBoxSurvivesASaveAndAFallenWolfGetsUpAsAWolf(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.desiccate();
            CompoundTag tag = new CompoundTag();
            m.saveWithoutId(tag);
            MikaelEntity again = MikaelEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.isDesiccated() && again.getBbHeight() < 0.8F, "a desiccated Mikael loaded standing: " + again.getBbHeight());

            KlausEntity k = rig.spawnKlaus();
            k.lookForm(BossForm.BEAST, ShiftRules.END_TICK);
            ShiftRules.Size wolf = ShiftRules.dimensionsFor(BossForm.BEAST, ShiftRules.END_TICK);
            helper.assertTrue(k.getBbWidth() == wolf.width() && k.getBbHeight() == wolf.height(), "setup: the wolf is " + k.getBbWidth() + " x " + k.getBbHeight());
            k.desiccate();
            helper.assertTrue(k.getBbHeight() < 0.8F, "a desiccated wolf is " + k.getBbHeight() + " tall");
            k.recover();
            helper.assertTrue(k.getBbWidth() == wolf.width() && k.getBbHeight() == wolf.height(), "the wolf got up as " + k.getBbWidth() + " x " + k.getBbHeight());
        }
        helper.succeed();
    }
}
