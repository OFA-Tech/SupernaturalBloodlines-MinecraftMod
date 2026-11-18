package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.LunarMoodClient;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/** What the almanacs of the three themes add for Enhanced Celestials events: a ring on the day's moon and a "Blood Moon in 3 days" line. Nothing without events. */
final class AlmanacEvents {
    private static final String KEY = PageKit.HUB + "path.";
    /** A ring is drawn this many pixels outside the icon. */
    private static final int REACH = 3;

    private AlmanacEvents() {
    }

    static List<LunarSource.Upcoming> events(HubScreen.Frame f) {
        return LunarMoodClient.upcoming(f.view.player().level());
    }

    /** The nearest scheduled event, when there is one. */
    static Optional<LunarSource.Upcoming> next(List<LunarSource.Upcoming> events) {
        return HubAlmanac.next(events);
    }

    /** The event's name from its translation key; an event whose language file is missing shows its id, titled ("blood_moon" is "Blood Moon"). */
    static Component name(LunarSource.Upcoming e) {
        String id = e.nameKey().substring(e.nameKey().lastIndexOf('.') + 1);
        StringBuilder title = new StringBuilder();
        for (String word : id.split("_")) {
            title.append(title.isEmpty() ? "" : " ").append(word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return Component.translatableWithFallback(e.nameKey(), title.toString());
    }

    /** "Blood Moon in 3 days." */
    static Component line(LunarSource.Upcoming e) {
        Component name = name(e);
        return e.days() == 0 ? Component.translatable(KEY + "event_today", name)
                : e.days() == 1 ? Component.translatable(KEY + "event_tomorrow", name) : Component.translatable(KEY + "event_in", name, e.days());
    }

    static int color(LunarSource.Upcoming e, int fallback) {
        return e.color() >= 0 ? 0xFF000000 | e.color() : fallback;
    }

    /** A ring in the event's colour round the moon icon of day {@code day} ({@code size} wide at {@code x}, {@code y}), with the event's name as its tooltip. */
    static void ring(HubScreen.Frame f, List<LunarSource.Upcoming> events, int day, int x, int y, int size, int fallback) {
        HubAlmanac.eventOn(events, day).ifPresent(e -> {
            int color = color(e, fallback);
            KitDraw.tickRing(f.g, x + size / 2, y + size / 2, size / 2 + REACH, 16, 1f, color, color);
            f.target("lunar:" + day, new Rect(x - REACH, y - REACH, size + 2 * REACH, size + 2 * REACH),
                    List.of(line(e)), null);
        });
    }
}
