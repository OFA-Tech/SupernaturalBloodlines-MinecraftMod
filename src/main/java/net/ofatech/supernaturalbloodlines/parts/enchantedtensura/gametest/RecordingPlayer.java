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
public final class RecordingPlayer extends FakePlayer {
    private final List<String> keys = new ArrayList<>();
    private final List<String> actionBarKeys = new ArrayList<>();

    private RecordingPlayer(GameTestHelper helper, UUID id) {
        super(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }

    public static RecordingPlayer create(GameTestHelper helper) {
        return new RecordingPlayer(helper, UUID.randomUUID());
    }

    public boolean received(String key) {
        return keys.contains(key);
    }

    public boolean receivedOnActionBar(String key) {
        return actionBarKeys.contains(key);
    }

    private void record(Component message, boolean actionBar) {
        if (message.getContents() instanceof TranslatableContents t) {
            keys.add(t.getKey());
            if (actionBar) {
                actionBarKeys.add(t.getKey());
            }
        }
    }

    @Override
    public void sendSystemMessage(Component message) {
        record(message, false);
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        record(message, actionBar);
    }
}
