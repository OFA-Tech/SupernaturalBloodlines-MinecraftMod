package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** A mortal fake player that keeps every message it is sent whole (key and arguments), for the line tests. */
public final class LineTestPlayer extends FakePlayer {
    private final List<Component> messages = new ArrayList<>();
    private int chat;
    /** A fake player never harms a player (Neo's FakePlayer says no); a test of a rule that asks may turn the vanilla answer back on, and must turn it off. */
    private static volatile boolean realHarm;

    public static void useRealHarm(boolean on) {
        realHarm = on;
    }

    @Override
    public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
        if (!realHarm) {
            return super.canHarmPlayer(other);
        }
        if (getServer() == null || !getServer().isPvpAllowed()) {
            return false;
        }
        var mine = getTeam();
        return mine == null || !mine.isAlliedTo(other.getTeam()) || mine.isAllowFriendlyFire();
    }

    private LineTestPlayer(GameTestHelper helper, UUID id) {
        super(helper.getLevel(), new GameProfile(id, "test-" + id.toString().substring(0, 8)));
    }

    public static LineTestPlayer create(GameTestHelper helper) {
        LineTestPlayer p = new LineTestPlayer(helper, UUID.randomUUID());
        TestPlayers.noSpawnProtection(helper, p);
        return p;
    }

    /** Every key and literal in the messages this player got, arguments included, as one string. */
    public String heard() {
        StringBuilder sb = new StringBuilder();
        messages.forEach(m -> flatten(m, sb));
        return sb.toString();
    }

    /** How many messages had this key at the top. */
    public int count(String key) {
        return (int) messages.stream().filter(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals(key)).count();
    }

    public boolean received(String key) {
        return count(key) > 0;
    }

    public int received() {
        return messages.size();
    }

    private static void flatten(Component c, StringBuilder out) {
        if (c.getContents() instanceof TranslatableContents t) {
            out.append('[').append(t.getKey()).append(']');
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component inner) {
                    flatten(inner, out);
                } else {
                    out.append(arg);
                }
            }
        } else {
            out.append(c.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents p ? p.text() : "");
        }
        c.getSiblings().forEach(s -> flatten(s, out));
    }

    /** Other mods message a species change too (Tensura's learned skills): only this mod's own messages are kept. */
    private void keep(Component message) {
        if (message.getContents() instanceof TranslatableContents t && t.getKey().contains("supernaturalbloodlines")) {
            messages.add(message);
        }
    }

    /** How many of the messages kept went to chat (an action bar message does not count). */
    public int chat() {
        return chat;
    }

    private final List<String> sounds = new ArrayList<>();

    /** The sounds sent to this player alone ({@code playNotifySound}), by registry id: a sound the world plays for everybody near is not here. */
    public List<String> notified() {
        return sounds;
    }

    @Override
    public void playNotifySound(net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        sounds.add(sound.getLocation().toString());
    }

    @Override
    public void sendSystemMessage(Component message) {
        int before = messages.size();
        keep(message);
        chat += messages.size() - before;
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        keep(message);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        CommonHooks.onLivingDeath(this, source);
    }
}
