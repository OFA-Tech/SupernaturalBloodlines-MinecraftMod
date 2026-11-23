package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The wheel's strings, looked up once and not once per frame: a translation, the text of a power's name, a key's label, a bold line. Each table is bounded and starts over
 * when it fills; {@link #clear} runs when the screen starts or ends and when resources reload (a language change is a reload). Client, render thread only.
 */
final class WheelStrings {
    private static final int MAX = 2048;
    private static final Map<String, String> TEXT = new HashMap<>();
    private static final Map<Component, String> COMPONENTS = new IdentityHashMap<>();
    private static final Map<String, String> KEYS = new HashMap<>();
    private static final Map<String, FormattedCharSequence> BOLD = new HashMap<>();

    private WheelStrings() {
    }

    static String text(String key, Object... args) {
        if (TEXT.size() > MAX) {
            TEXT.clear();
        }
        return TEXT.computeIfAbsent(args.length == 0 ? key : key + '\u0000' + Arrays.toString(args), k -> Component.translatable("supernaturalbloodlines.wheel." + key, args).getString());
    }

    /** The text of a component of a catalogue entry (the entry list is the same object from one revision to the next, so identity is the key). */
    static String of(Component c) {
        if (COMPONENTS.size() > MAX) {
            COMPONENTS.clear();
        }
        return COMPONENTS.computeIfAbsent(c, Component::getString);
    }

    /** How a key reads, looked up by the key it is bound to now (a rebinding is another key). */
    static String key(KeyMapping km) {
        if (KEYS.size() > MAX) {
            KEYS.clear();
        }
        return KEYS.computeIfAbsent(km.getName() + '|' + km.getKey().getName() + '|' + km.getKeyModifier(), k -> km.getTranslatedKeyMessage().getString());
    }

    static FormattedCharSequence bold(String s) {
        if (BOLD.size() > MAX) {
            BOLD.clear();
        }
        return BOLD.computeIfAbsent(s, k -> Language.getInstance().getVisualOrder(Component.literal(k).withStyle(ChatFormatting.BOLD)));
    }

    private static int epoch;

    /** Changes whenever the strings were thrown away (a screen started or ended, resources reloaded): what a layout built from them has to be checked against. */
    static int epoch() {
        return epoch;
    }

    static void clear() {
        epoch++;
        TEXT.clear();
        COMPONENTS.clear();
        KEYS.clear();
        BOLD.clear();
    }
}
