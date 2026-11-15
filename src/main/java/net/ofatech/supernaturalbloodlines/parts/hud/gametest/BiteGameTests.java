package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.IExtendedCreatureVampirism;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;

/** The Bite action's server side for a Vampirism vampire (R110 row 2), and the target rules every bite shares. Needs Vampirism. */
@PrefixGameTestTemplate(false)
public final class BiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BiteGameTests() {
    }

    static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        place(helper, p, 1.5);
        return p;
    }

    static void place(GameTestHelper helper, ServerPlayer p, double x) {
        Vec3 at = helper.absoluteVec(new Vec3(x, 2, 1.5));
        p.moveTo(at.x, at.y, at.z);
    }

    static PathfinderMob villager(GameTestHelper helper, double x) {
        PathfinderMob v = (PathfinderMob) EntityType.VILLAGER.create(helper.getLevel());
        v.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(x, 2, 1.5));
        v.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(v);
        return v;
    }

    static int blood(PathfinderMob mob) {
        return VampirismAPI.getExtendedCreatureVampirism(mob).map(IExtendedCreatureVampirism::getBlood).orElse(0);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireBiteStartsFeeding(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        int villagerBlood = blood(v);
        int mine = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        helper.assertTrue(BiteService.actionFor(p) == BiteAction.VAMPIRISM_DRINK, "a vampire's bite is " + BiteService.actionFor(p));
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(r.ok(), "the bite was refused: " + r.refusal());
        // a FakePlayer is not ticked by the server, so tick Vampirism's own player update by hand
        for (int i = 0; i < 40 && VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() <= mine; i++) {
            Feeding.tick(p);
        }
        BiteService.release(p);
        helper.assertTrue(blood(v) < villagerBlood, "the villager's blood stayed " + blood(v));
        helper.assertTrue(VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() > mine, "the vampire's blood stayed " + mine);
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteOutOfReachRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = villager(helper, 7.5);
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("out_of_reach"), "a far target: " + r);
        helper.assertTrue(BiteService.cooldownLeft(p) == 0, "a refused bite started the cooldown");
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteOtherLevelRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "no nether level in the test server");
        PathfinderMob far = (PathfinderMob) EntityType.VILLAGER.create(nether);
        far.moveTo(p.getX() + 1, p.getY(), p.getZ());
        nether.addFreshEntity(far);
        BiteService.Result r = BiteService.press(p, far.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("no_target"), "a target in another level: " + r);
        far.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nobodyBitesNothing(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        BiteService.Result r = BiteService.press(p, -1);
        helper.assertTrue(!r.ok() && r.refusal().endsWith("no_target"), "no target: " + r);
        BiteService.Result self = BiteService.press(p, p.getId());
        helper.assertTrue(!self.ok() && self.refusal().endsWith("no_target"), "biting oneself: " + self);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rateLimited(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = villager(helper, 2.5);
        helper.assertTrue(BiteService.press(p, v.getId()).ok(), "the first bite was refused");
        BiteService.Result second = BiteService.press(p, v.getId());
        helper.assertTrue(!second.ok() && second.refusal().endsWith("cooldown"), "a second bite in the same tick: " + second);
        helper.assertTrue(BiteService.cooldownLeft(p) > 0, "no cooldown shown");
        BiteService.release(p);
        v.discard();
        helper.succeed();
    }

    /** Without the species part nobody is in the turning here: row T never matches, whatever the player's state says. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningBiteWithoutSpeciesDoesNothing(GameTestHelper helper) {
        if (EnabledParts.isEnabled("species")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(9000).build());
        place(helper, p, 1.5);
        PathfinderMob v = villager(helper, 2.5);
        try {
            helper.assertTrue(BiteService.actionFor(p) == BiteAction.NONE, "a bite without species is " + BiteService.actionFor(p));
            BiteService.Result r = BiteService.press(p, v.getId());
            helper.assertTrue(!r.ok() && v.getHealth() == v.getMaxHealth(), "a bite without species happened: " + r);
        } finally {
            v.discard();
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void plainHumanBiteDoesNothing(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        place(helper, p, 1.5);
        PathfinderMob v = villager(helper, 2.5);
        int before = blood(v);
        helper.assertTrue(BiteService.actionFor(p) == BiteAction.NONE, "a human's bite is " + BiteService.actionFor(p));
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("none") && blood(v) == before && v.getHealth() == v.getMaxHealth(), "a human bit: " + r);
        v.discard();
        helper.succeed();
    }
}
