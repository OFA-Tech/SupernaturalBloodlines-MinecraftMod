package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;

/** R20e: a human in plan 11's turning finishes it with a bite (R110 row T). Needs Vampirism and the species part. */
@PrefixGameTestTemplate(false)
public final class TurningBiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TurningBiteGameTests() {
    }

    private static ServerPlayer inTurning(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(9000).build());
        BiteGameTests.place(helper, p, 1.5);
        return p;
    }

    private static void clean(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningBiteCompletesTurning(GameTestHelper helper) {
        ServerPlayer p = inTurning(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        try {
            helper.assertTrue(BiteService.actionFor(p) == BiteAction.TURNING_BITE, "a human in the turning bites with " + BiteService.actionFor(p));
            BiteService.Result r = BiteService.press(p, v.getId());
            helper.assertTrue(r.ok(), "the turning bite was refused: " + r.refusal());
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION, "not a vampire after the bite");
            helper.assertTrue(!BloodlineAccess.get(p).inTurning(), "the turning is still running");
            helper.assertTrue(v.getMaxHealth() - v.getHealth() >= 0.99f, "the villager took no bite");
        } finally {
            v.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningBiteRefusedByAClaim(GameTestHelper helper) throws Exception {
        ServerPlayer p = inTurning(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        try (AutoCloseable claim = BiteGameTests.cancelAttacks()) {
            BiteService.Result r = BiteService.press(p, v.getId());
            helper.assertTrue(!r.ok() && r.refusal().endsWith("cannot_bite_target"), "a protected villager: " + r);
        } finally {
            v.discard();
        }
        helper.assertTrue(BloodlineAccess.get(p).inTurning() && BiteService.cooldownLeft(p) == 0 && v.getHealth() == v.getMaxHealth(),
                "a refused turning bite changed something");
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningBiteRefusesNonHumanLike(GameTestHelper helper) {
        ServerPlayer p = inTurning(helper);
        Mob cow = (Mob) EntityType.COW.create(helper.getLevel());
        cow.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 1.5));
        cow.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(cow);
        Mob vampire = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        vampire.setNoAi(true);
        vampire.moveTo(at.x, at.y, at.z + 0.5);
        helper.getLevel().addFreshEntity(vampire);
        try {
            BiteService.Result r = BiteService.press(p, cow.getId());
            helper.assertTrue(!r.ok() && r.refusal().endsWith("not_human"), "a cow: " + r);
            BiteService.Result r2 = BiteService.press(p, vampire.getId());
            helper.assertTrue(!r2.ok() && r2.refusal().endsWith("not_human"), "a vampire: " + r2);
            helper.assertTrue(BloodlineAccess.get(p).inTurning() && VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == null,
                    "the turning did not continue: " + BloodlineAccess.get(p));
            helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "a refused bite hurt the cow");
        } finally {
            cow.discard();
            vampire.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulInTurningUsesVampirismBite(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).turningTicksLeft(9000).build());
        BiteGameTests.place(helper, p, 1.5);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        try {
            helper.assertTrue(BiteService.actionFor(p) == BiteAction.VAMPIRISM_DRINK, "a ghoul in the turning bites with " + BiteService.actionFor(p));
            helper.assertTrue(BiteService.press(p, v.getId()).ok(), "the ghoul's bite was refused");
            for (int i = 0; i < 80 && BloodlineAccess.get(p).inTurning(); i++) {
                Feeding.tick(p);
            }
            BiteService.release(p);
            helper.assertTrue(!BloodlineAccess.get(p).inTurning() && !BloodlineAccess.get(p).ghoul(), "Vampirism's bite did not finish the turning: " + BloodlineAccess.get(p));
        } finally {
            v.discard();
            clean(p);
        }
        helper.succeed();
    }

}
