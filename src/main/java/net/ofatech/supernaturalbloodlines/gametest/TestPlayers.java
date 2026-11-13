package net.ofatech.supernaturalbloodlines.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }

    /** Like {@link #create}, but it takes damage (a plain FakePlayer is invulnerable, so no damage event ever fires). */
    public static ServerPlayer createVulnerable(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return noSpawnProtection(new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
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
        return noSpawnProtection(new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
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
        return noSpawnProtection(new FakePlayer(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8))) {
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

    private static ServerPlayer noSpawnProtection(ServerPlayer p) {
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
