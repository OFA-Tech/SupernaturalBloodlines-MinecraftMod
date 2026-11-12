package net.ofatech.supernaturalbloodlines.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

final class TestPlayers {
    private TestPlayers() {
    }

    /**
     * A player in the test level that never fires the login event. {@code makeMockServerPlayerInLevel} does, and
     * other mods then send packets to its mock connection, which throws.
     */
    static ServerPlayer create(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }
}
