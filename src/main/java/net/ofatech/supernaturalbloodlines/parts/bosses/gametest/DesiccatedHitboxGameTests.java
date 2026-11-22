package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
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
 * aimed where the Original would have stood. Every test stands the boss on a floor and ticks it once first: vanilla only looks for room after a change of
 * size once the entity has ticked, and without a floor the boss falls and hides where it ends up.
 */
@PrefixGameTestTemplate(false)
public final class DesiccatedHitboxGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final float[] YAWS = {0F, 30F, 45F, 90F, 135F, 200F, 270F, 315F};
    private static final double SAME = 1.0E-3;

    private DesiccatedHitboxGameTests() {
    }

    /** A stone floor and open air above it: the template has a barrier ceiling one block up, and under it vanilla finds no room to move a box, which hides the float. */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static Map<String, Supplier<NpcOriginalBoss>> all(BossTestRig rig) {
        Map<String, Supplier<NpcOriginalBoss>> m = new LinkedHashMap<>();
        m.put("mikael", () -> settled(rig, rig.spawn()));
        m.put("klaus", () -> settled(rig, rig.spawnKlaus()));
        m.put("elijah", () -> settled(rig, rig.spawnElijah()));
        m.put("rebekah", () -> settled(rig, rig.spawnRebekah()));
        m.put("kol", () -> settled(rig, rig.spawnKol()));
        m.put("finn", () -> settled(rig, rig.spawnFinn()));
        return m;
    }

    /** One tick: the entity is no longer in its first. */
    private static <T extends NpcOriginalBoss> T settled(BossTestRig rig, T e) {
        BlockPos at = rig.helper.absolutePos(new BlockPos(2, 1, 2));
        e.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        e.tick();
        return e;
    }

    /** A point of the lying body: {@code along} blocks from its middle toward the head, half way up the box. */
    private static Vec3 onBody(NpcOriginalBoss e, double along) {
        double[] d = LyingBody.onBody(e.yBodyRot, along);
        return new Vec3(e.getX() + d[0], e.getY() + LyingBody.HEIGHT / 2.0, e.getZ() + d[1]);
    }

    private static boolean hit(NpcOriginalBoss e, Vec3 aimAt) {
        Vec3 from = new Vec3(e.getX() + 3.5, e.getY() + 1.62, e.getZ() + 3.5);
        Vec3 to = from.add(aimAt.subtract(from).normalize().scale(10.0));
        // what ProjectileUtil.getEntityHitResult and the client's crosshair pick do with an entity's box
        return e.getBoundingBox().inflate(e.getPickRadius()).clip(from, to).isPresent();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedOriginalsBoxIsLowAndCoversTheBodyThatLies(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
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
            floor(helper);
            for (var entry : all(rig).entrySet()) {
                NpcOriginalBoss e = entry.getValue().get();
                String who = entry.getKey();
                Vec3 standingHead = e.position().add(0.0, 1.7, 0.0);
                helper.assertTrue(hit(e, standingHead), who + ": control, a blow at a standing Original's head missed, box " + e.getBoundingBox() + " position " + e.position());
                e.desiccate();
                e.yBodyRot = 60F;
                for (double along : new double[] {-0.8, 0.0, 0.8}) {
                    helper.assertTrue(hit(e, onBody(e, along)), who + ": a blow at the body " + along + " from its middle missed");
                }
                helper.assertTrue(!hit(e, standingHead), who + ": a blow where he would have stood hit a body that lies");
                e.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBoxIsStandingAgainWhenTheOriginalGetsUp(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
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
                helper.assertTrue(hit(e, e.position().add(0.0, 1.7, 0.0)), who + ": a blow at his head missed after he got up");
                e.discard();
            }
        }
        helper.succeed();
    }

    /** The float of the first review: vanilla's search for room put a 0.5 high body 0.725 above the floor, and with no AI nothing brought it down. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedOriginalStaysOnTheFloorAndGetsUpOnIt(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            for (var entry : all(rig).entrySet()) {
                NpcOriginalBoss e = entry.getValue().get();
                String who = entry.getKey();
                double y = e.getY();
                helper.assertTrue(Math.abs(y - helper.absolutePos(new BlockPos(0, 1, 0)).getY()) < SAME, who + ": control, he is not on the floor: " + y);
                e.desiccate();
                helper.assertTrue(Math.abs(e.getY() - y) < SAME, who + ": desiccating moved him from " + y + " to " + e.getY());
                for (int i = 0; i < 5; i++) {
                    e.tick();
                }
                helper.assertTrue(Math.abs(e.getY() - y) < SAME, who + ": he drifted to " + e.getY());
                e.recover();
                helper.assertTrue(Math.abs(e.getY() - y) < SAME, who + ": getting up moved him from " + y + " to " + e.getY());
                e.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedOriginalIsNotShovedAndShovesNobody(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            MikaelEntity m = settled(rig, rig.spawn());
            Zombie z = EntityType.ZOMBIE.create(helper.getLevel());
            z.setNoAi(true);
            z.setPos(m.getX() + 0.5, m.getY(), m.getZ());
            helper.getLevel().addFreshEntity(z);
            m.desiccate();
            helper.assertTrue(!m.isPushable(), "a desiccated Original can be pushed");
            for (int i = 0; i < 3; i++) {
                m.tick();
                z.tick();
            }
            helper.assertTrue(z.getDeltaMovement().horizontalDistanceSqr() == 0.0, "he shoved a zombie: " + z.getDeltaMovement());
            helper.assertTrue(m.getDeltaMovement().horizontalDistanceSqr() == 0.0, "a zombie shoved him: " + m.getDeltaMovement());
            m.recover();
            helper.assertTrue(m.isPushable(), "control: he is not pushable once he stands");
            m.tick();
            z.tick();
            helper.assertTrue(z.getDeltaMovement().horizontalDistanceSqr() > 0.0 || m.getDeltaMovement().horizontalDistanceSqr() > 0.0, "control: two bodies that overlap did not push each other");
            z.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLyingBoxSurvivesASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            MikaelEntity m = settled(rig, rig.spawn());
            m.desiccate();
            CompoundTag tag = new CompoundTag();
            m.saveWithoutId(tag);
            MikaelEntity again = MikaelEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.isDesiccated() && again.getBbHeight() < 0.8F, "a desiccated Mikael loaded standing: " + again.getBbHeight());
        }
        helper.succeed();
    }

    /** The wolf lies on its side with its head up: a box of its own, measured from its death pose, with the middle of the head in it whichever way he faces. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFallenWolfHasItsOwnBoxWithTheHeadInItAndGetsUpAsAWolf(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            floor(helper);
            KlausEntity k = rig.spawnKlaus();
            k.lookForm(BossForm.BEAST, ShiftRules.END_TICK);
            settled(rig, k);
            ShiftRules.Size wolf = ShiftRules.dimensionsFor(BossForm.BEAST, ShiftRules.END_TICK);
            helper.assertTrue(k.getBbWidth() == wolf.width() && k.getBbHeight() == wolf.height(), "setup: the wolf is " + k.getBbWidth() + " x " + k.getBbHeight());
            double y = k.getY();
            k.desiccate();
            helper.assertTrue(k.getBbWidth() == LyingBody.BEAST_LENGTH && k.getBbHeight() == LyingBody.BEAST_HEIGHT, "a desiccated wolf is " + k.getBbWidth() + " x " + k.getBbHeight());
            helper.assertTrue(Math.abs(k.getY() - y) < SAME, "the wolf floated from " + y + " to " + k.getY());
            for (float yaw : YAWS) {
                k.yBodyRot = yaw;
                double[] d = LyingBody.inFrame(yaw, LyingBody.BEAST_HEAD[0], LyingBody.BEAST_HEAD[2]);
                Vec3 head = new Vec3(k.getX() + d[0], k.getY() + LyingBody.BEAST_HEAD[1], k.getZ() + d[1]);
                helper.assertTrue(k.getBoundingBox().contains(head), "the wolf's head, turned " + yaw + ", is outside its box " + k.getBoundingBox() + " at " + head);
                helper.assertTrue(hit(k, head), "a blow at the wolf's head, turned " + yaw + ", missed");
            }
            k.recover();
            helper.assertTrue(k.getBbWidth() == wolf.width() && k.getBbHeight() == wolf.height(), "the wolf got up as " + k.getBbWidth() + " x " + k.getBbHeight());
            helper.assertTrue(Math.abs(k.getY() - y) < SAME, "the wolf got up from " + y + " to " + k.getY());
        }
        helper.succeed();
    }
}
