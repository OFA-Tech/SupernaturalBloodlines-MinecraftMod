package net.ofatech.supernaturalbloodlines.shared.rite;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * The one way a rite talks to a player (R357). A refusal goes to the action bar, where it always went, and once to chat, so it is not lost when the
 * bar fades; a success or a notice stays on the action bar. {@code to} may be null or not a player (the caster left): then nobody is told.
 */
public final class RiteTell {
    /** What a rite of the werewolves says when the Werewolves mod is not installed. */
    public static final String NEEDS_WEREWOLVES = "supernaturalbloodlines.rite.needs_werewolves";

    private RiteTell() {
    }

    /** A refusal: the action bar and chat, once each. */
    public static void refuse(@Nullable Entity to, String key, Object... args) {
        refuse(to, Component.translatable(key, args));
    }

    public static void refuse(@Nullable Entity to, Component line) {
        if (to instanceof ServerPlayer p) {
            p.displayClientMessage(line, true);
            p.sendSystemMessage(line);
        }
    }

    /**
     * The one generic line of an oracle refusal (R201, R339: "the rite would change nothing"), the same for every cause; it goes where every refusal
     * goes, and nothing is added to it.
     */
    public static void refuseGeneric(@Nullable Entity to, String key) {
        refuse(to, key);
    }

    /** A success or a notice: the action bar only. */
    public static void say(@Nullable Entity to, String key, Object... args) {
        say(to, Component.translatable(key, args));
    }

    public static void say(@Nullable Entity to, Component line) {
        if (to instanceof ServerPlayer p) {
            p.displayClientMessage(line, true);
        }
    }
}
