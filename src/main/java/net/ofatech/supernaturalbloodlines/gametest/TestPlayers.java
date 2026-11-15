package net.ofatech.supernaturalbloodlines.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.lang.reflect.Field;
import java.util.UUID;

public final class TestPlayers {
    private TestPlayers() {
    }

    /**
     * A player in the test level that never fires the login event. {@code makeMockServerPlayerInLevel} does, and
     * other mods then send packets to its mock connection, which throws.
     */
    public static ServerPlayer create(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return place(helper, FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))));
    }

    /**
     * A new ServerPlayer is put at a random spot within the spawn radius (10), so two test players could stand 28 blocks
     * apart and fall outside a 24 block range check. Every test player starts at the same spot in its own structure instead.
     */
    private static <P extends ServerPlayer> P place(GameTestHelper helper, P p) {
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        return p;
    }

    /** Like {@link #create}, but it takes damage (a plain FakePlayer is invulnerable, so no damage event ever fires). */
    public static ServerPlayer createVulnerable(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return noSpawnProtection(helper, new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }
        });
    }

    /**
     * Like {@link #createVulnerable}, but its death is announced like a real player's: FakePlayer.die does nothing,
     * so no LivingDeathEvent ever fires for the others. A cancelled event keeps the player alive.
     */
    public static ServerPlayer createMortal(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return noSpawnProtection(helper, new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }

            @Override
            public void die(DamageSource source) {
                CommonHooks.onLivingDeath(this, source);
            }
        });
    }

    /**
     * Like {@link #createMortal}, but other players can hurt it: FakePlayer.canHarmPlayer is always false. The test must
     * also turn the server's PvP on while it hits (the GameTest server has it off).
     */
    public static ServerPlayer createDuelist(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return noSpawnProtection(helper, new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }

            @Override
            public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return true;
            }

            @Override
            public void die(DamageSource source) {
                CommonHooks.onLivingDeath(this, source);
            }
        });
    }

    /** Like {@link #createDuelist}, but {@code canHarmPlayer} follows the server's PvP setting, as a real player's does. */
    public static ServerPlayer createPvpAware(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return noSpawnProtection(helper, new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
            @Override
            public boolean isInvulnerableTo(DamageSource source) {
                return false;
            }

            @Override
            public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return level().getServer().isPvpAllowed();
            }

            @Override
            public void die(DamageSource source) {
                CommonHooks.onLivingDeath(this, source);
            }
        });
    }

    private static ServerPlayer noSpawnProtection(GameTestHelper helper, ServerPlayer p) {
        place(helper, p);
        try {
            // the 60 tick respawn protection only counts down in tick(), which a FakePlayer never runs
            Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(p, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }
}
