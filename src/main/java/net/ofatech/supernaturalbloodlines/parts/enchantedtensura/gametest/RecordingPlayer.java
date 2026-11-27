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
    private final List<String> chatKeys = new ArrayList<>();

    private RecordingPlayer(GameTestHelper helper, UUID id) {
        super(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }

    private boolean vulnerable;

    public static RecordingPlayer create(GameTestHelper helper) {
        return new RecordingPlayer(helper, UUID.randomUUID());
    }

    /** One that can be hurt (a FakePlayer cannot), for a test that needs a wielder to take a backlash. */
    public static RecordingPlayer createVulnerable(GameTestHelper helper) {
        RecordingPlayer p = create(helper);
        p.vulnerable = true;
        return (RecordingPlayer) net.ofatech.supernaturalbloodlines.gametest.TestPlayers.noSpawnProtection(helper, p); // the respawn protection only ticks down in tick()
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !vulnerable && super.isInvulnerableTo(source);
    }

    public boolean received(String key) {
        return keys.contains(key);
    }

    /** How many times this key was sent. */
    public int count(String key) {
        return (int) keys.stream().filter(key::equals).count();
    }

    /** Every distinct key sent. */
    public java.util.Set<String> heardKeys() {
        return new java.util.LinkedHashSet<>(keys);
    }

    /** Every key sent, in order: for a failure message. */
    public String heard() {
        return String.join(", ", keys);
    }

    public boolean receivedOnActionBar(String key) {
        return actionBarKeys.contains(key);
    }

    /** How many times this key came on the action bar. */
    public int countOnActionBar(String key) {
        return (int) actionBarKeys.stream().filter(key::equals).count();
    }

    /** How many times this key came in chat. */
    public int countInChat(String key) {
        return (int) chatKeys.stream().filter(key::equals).count();
    }

    /** A rite refusal as R357 wants it: once on the action bar and once in chat. */
    public boolean refusedOnBothChannels(String key) {
        return countOnActionBar(key) == 1 && countInChat(key) == 1;
    }

    private boolean lastWasBar;

    /**
     * A rite refusal comes on the action bar and then, the same line, in chat ({@code RiteTell}): in {@link #heard()}, {@link #count} and
     * {@link #received} that is one line heard, the way a player reads it; {@link #countInChat} sees the copy.
     */
    private void record(Component message, boolean actionBar) {
        if (message.getContents() instanceof TranslatableContents t) {
            boolean copyOfBar = !actionBar && lastWasBar && !keys.isEmpty() && keys.get(keys.size() - 1).equals(t.getKey());
            if (!copyOfBar) {
                keys.add(t.getKey());
            }
            (actionBar ? actionBarKeys : chatKeys).add(t.getKey());
            lastWasBar = actionBar;
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
