package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayer;

/** A FakePlayer drops every message it is sent; this one keeps the translation keys so a test can read them. */
final class RecordingPlayer extends FakePlayer {
    private final List<String> keys = new ArrayList<>();

    private RecordingPlayer(GameTestHelper helper, UUID id) {
        super(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }

    static RecordingPlayer create(GameTestHelper helper) {
        return new RecordingPlayer(helper, UUID.randomUUID());
    }

    boolean received(String key) {
        return keys.contains(key);
    }

    private void record(Component message) {
        if (message.getContents() instanceof TranslatableContents t) {
            keys.add(t.getKey());
        }
    }

    @Override
    public void sendSystemMessage(Component message) {
        record(message);
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        record(message);
    }
}
