package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.HudTargets;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/** Plan 18d Task 1: an extra action is handed the creature the crosshair named, resolved by the server (same level, alive, in reach, in sight), and no action runs for a player who cannot act. */
@PrefixGameTestTemplate(false)
public final class ActionTargetGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NS, "test_target_action");

    private ActionTargetGameTests() {
    }

    /** Records what it was given. */
    private static final class Recorder implements ExtraActions.Action {
        final AtomicReference<LivingEntity> seen = new AtomicReference<>();
        int runs;
        boolean throughOldMethod;

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public Component name() {
            return Component.literal("test");
        }

        @Override
        public Component note() {
            return Component.literal("test");
        }

        @Override
        public ResourceLocation icon() {
            return ResourceLocation.fromNamespaceAndPath(NS, "textures/gui/hud/unknown.png");
        }

        @Override
        public boolean available(Player p) {
            return true;
        }

        @Nullable
        @Override
        public Component trigger(ServerPlayer p) {
            throughOldMethod = true;
            runs++;
            return null;
        }

        @Nullable
        @Override
        public Component trigger(ServerPlayer p, @Nullable LivingEntity target) {
            throughOldMethod = false;
            runs++;
            seen.set(target);
            return null;
        }

        @Override
        public int cooldownTicks(ServerPlayer p) {
            return 0;
        }

        @Override
        public int cooldownLeft(ServerPlayer p) {
            return 0;
        }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        return p;
    }

    private static Zombie zombieAt(GameTestHelper helper, double x, double z) {
        Zombie z1 = EntityType.ZOMBIE.create(helper.getLevel());
        z1.setPos(helper.absoluteVec(new Vec3(x, 1, z)));
        z1.setNoAi(true);
        helper.getLevel().addFreshEntity(z1);
        return z1;
    }

    private static Recorder install(ServerPlayer p) {
        Recorder r = new Recorder();
        ExtraActions.install(r);
        LoadoutService.set(p, 2, ID);
        return r;
    }

    private static void remove() {
        ExtraActions.remove(ID);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anExtraActionReceivesTheResolvedTarget(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        Zombie z = zombieAt(helper, 1.5, 3.5);
        try {
            helper.assertTrue(ActionService.trigger(p, 2, z.getId(), false) == null, "the trigger was refused");
            helper.assertTrue(r.runs == 1 && r.seen.get() == z && !r.throughOldMethod, "the action was given " + r.seen.get() + " (runs " + r.runs + ")");
            helper.assertTrue(HudTargets.resolve(p, z.getId()).orElse(null) == z, "HudTargets does not resolve a zombie in reach");
        } finally {
            z.discard();
            remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noTargetIsNullAndTheActionStillRuns(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        try {
            helper.assertTrue(ActionService.trigger(p, 2, -1, false) == null, "the trigger was refused");
            helper.assertTrue(r.runs == 1 && r.seen.get() == null, "an action with no crosshair target was given " + r.seen.get());
            helper.assertTrue(ActionService.trigger(p, 2, 123456789, false) == null && r.seen.get() == null, "an id that is no entity was resolved");
        } finally {
            remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFarTargetIsNull(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        Zombie z = zombieAt(helper, 1.5, 3.5);
        try {
            z.setPos(p.position().add(0, 0, 40));
            helper.assertTrue(ActionService.trigger(p, 2, z.getId(), false) == null, "the trigger was refused");
            helper.assertTrue(r.seen.get() == null, "a target 40 blocks away was resolved");
        } finally {
            z.discard();
            remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetBehindAWallIsNull(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        Zombie z = zombieAt(helper, 1.5, 4.5);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 3));
        try {
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    helper.getLevel().setBlockAndUpdate(at.offset(dx, dy, 0), Blocks.STONE.defaultBlockState());
                }
            }
            helper.assertTrue(ActionService.trigger(p, 2, z.getId(), false) == null, "the trigger was refused");
            helper.assertTrue(r.seen.get() == null, "a target behind a wall was resolved");
        } finally {
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    helper.getLevel().setBlockAndUpdate(at.offset(dx, dy, 0), Blocks.AIR.defaultBlockState());
                }
            }
            z.discard();
            remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeadTargetAndTheUserThemselvesAreNull(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        Zombie z = zombieAt(helper, 1.5, 3.5);
        try {
            helper.assertTrue(HudTargets.resolve(p, p.getId()).isEmpty(), "the user was their own target");
            z.kill();
            helper.assertTrue(HudTargets.resolve(p, z.getId()).isEmpty(), "a dead creature was a target");
            helper.assertTrue(r.runs == 0, "nothing ran");
        } finally {
            z.discard();
            remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noExtraActionRunsForAPlayerWhoCannotAct(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Recorder r = install(p);
        java.util.function.Predicate<ServerPlayer> mine = who -> who == p;
        ExtraActions.addBlocker(mine);
        try {
            helper.assertTrue(ActionService.trigger(p, 2, -1, false) != null && r.runs == 0, "an action ran for a player a blocker names");
        } finally {
            ExtraActions.removeBlocker(mine);
        }
        helper.assertTrue(ActionService.trigger(p, 2, -1, false) == null && r.runs == 1, "the action did not run once the blocker was gone");
        remove();
        helper.succeed();
    }
}
