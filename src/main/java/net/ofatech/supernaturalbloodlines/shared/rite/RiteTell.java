package net.ofatech.supernaturalbloodlines.shared.rite;

import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar;
import org.jetbrains.annotations.Nullable;

/**
 * The one way a rite talks to a player (R357). A refusal goes to the action bar, where it always went, and once to chat, so it is not lost when the
 * bar fades; a success or a notice stays on the action bar. {@code to} may be null or not a player (the caster left): then nobody is told.
 * <p>
 * A refusal about the night is a short line on the action bar and the whole of it in chat ({@code <key>.detail}): what a full moon or an eclipse
 * is, and, for the full moon, when the next one comes.
 */
public final class RiteTell {
    /** What a rite of the werewolves says when the Werewolves mod is not installed. */
    public static final String NEEDS_WEREWOLVES = "supernaturalbloodlines.rite.needs_werewolves";
    /** The one generic line of an oracle refusal (R201, R339). */
    public static final String NO_EFFECT = "supernaturalbloodlines.rite.no_effect";
    /** The refusals that want a full moon, each with the whole line for chat; that line also gets when the next full moon comes. */
    public static final Map<String, String> FULL_MOON_DETAIL = Map.of(
            "supernaturalbloodlines.rite.needs_full_moon", "supernaturalbloodlines.rite.needs_full_moon.detail",
            "supernaturalbloodlines.trial.not_night", "supernaturalbloodlines.trial.not_night.detail",
            "supernaturalbloodlines.rite.frequency.not_night", "supernaturalbloodlines.rite.frequency.not_night.detail");
    /** The refusals that want a lunar eclipse, each with the whole line for chat. */
    public static final Map<String, String> ECLIPSE_DETAIL = Map.of(
            "supernaturalbloodlines.rite.needs_eclipse", "supernaturalbloodlines.rite.needs_eclipse.detail",
            "supernaturalbloodlines.refusal.line_no_eclipse", "supernaturalbloodlines.refusal.line_no_eclipse.detail");
    public static final Set<String> FULL_MOON = FULL_MOON_DETAIL.keySet();
    public static final Set<String> ECLIPSE = ECLIPSE_DETAIL.keySet();
    public static final String FULL_MOON_TONIGHT = "supernaturalbloodlines.rite.full_moon_tonight";
    public static final String FULL_MOON_IN = "supernaturalbloodlines.rite.full_moon_in";

    private RiteTell() {
    }

    /** A refusal: the action bar and chat, once each. */
    public static void refuse(@Nullable Entity to, String key, Object... args) {
        if (to instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable(key, args), true);
            p.sendSystemMessage(chatLine(p, key, args));
        }
    }

    /** The chat copy of a refusal: the same line, or the whole of a night refusal. */
    static Component chatLine(ServerPlayer p, String key, Object... args) {
        if (FULL_MOON.contains(key)) {
            int days = MoonCalendar.daysToFull(p.level().getMoonPhase());
            return Component.translatable(FULL_MOON_DETAIL.get(key)).append(" ")
                    .append(days == 0 ? Component.translatable(FULL_MOON_TONIGHT) : Component.translatable(FULL_MOON_IN, days));
        }
        if (ECLIPSE.contains(key)) {
            return Component.translatable(ECLIPSE_DETAIL.get(key));
        }
        return Component.translatable(key, args);
    }

    /**
     * The one generic line of an oracle refusal, the same for every cause; it goes where every refusal goes, and nothing is added to it. Only
     * {@link #NO_EFFECT} is such a line.
     */
    public static void refuseGeneric(@Nullable Entity to, String key) {
        if (!NO_EFFECT.equals(key)) {
            throw new IllegalArgumentException("not the generic line: " + key);
        }
        refuse(to, key);
    }

    /** A success, a notice or a request (sneak to accept): the action bar only. */
    public static void say(@Nullable Entity to, String key, Object... args) {
        say(to, Component.translatable(key, args));
    }

    public static void say(@Nullable Entity to, Component line) {
        if (to instanceof ServerPlayer p) {
            p.displayClientMessage(line, true);
        }
    }

    /** A refusal whose words are already made (a line with its own arguments). */
    public static void refuse(@Nullable Entity to, Component line) {
        if (to instanceof ServerPlayer p) {
            p.displayClientMessage(line, true);
            p.sendSystemMessage(line);
        }
    }
}
