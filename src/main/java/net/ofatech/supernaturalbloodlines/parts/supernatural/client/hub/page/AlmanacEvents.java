package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.LunarMoodClient;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * What the almanacs of the three themes add for Enhanced Celestials events: a ring on the day's moon (in the event's colour, which is the only
 * place that colour is used) and one text line per event, in the theme's own ink so it reads on every look. Nothing without events.
 */
final class AlmanacEvents {
    private static final String KEY = PageKit.HUB + "path.";

    private AlmanacEvents() {
    }

    static LunarMoodClient.View view(HubScreen.Frame f) {
        return LunarMoodClient.view(f.view.player().level());
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

    /** The height of all the lines at {@code width}, each wrapped and {@code lineHeight} tall. */
    static int height(HubScreen.Frame f, LunarMoodClient.View view, int width, int lineHeight) {
        int h = 0;
        for (LunarSource.Upcoming e : view.lines()) {
            h += f.font().split(line(e), Math.max(8, width)).size() * lineHeight;
        }
        return h;
    }

    /** The distance between two moon icons: wider when there are rings. */
    static int step(LunarMoodClient.View view, int base, int iconSize) {
        return HubAlmanac.step(base, iconSize, !view.byDay().isEmpty());
    }

    /**
     * The whole row of moons is a door to the moon calendar (R140): registered before the rings, so a ring's tooltip wins the hover while the click
     * (the rings have no action) still reaches this one. {@code step} is the distance between icons, {@code size} their size.
     */
    static void link(HubScreen.Frame f, int x, int y, int step, int size) {
        int reach = HubAlmanac.RING_REACH;
        Rect row = new Rect(x - reach, y - reach, step * HubAlmanac.ROW, size + 2 * reach);
        f.target("lunar:calendar", row, List.of(Component.translatable(KEY + "calendar_open")), () -> f.screen().openMoon());
        if (f.focused("lunar:calendar")) {
            f.theme().drawFocus(f.g, row);
        }
    }

    /** A ring round the moon icon of day {@code day} ({@code size} wide at {@code x}, {@code y}), with the event line as its tooltip: the event's colour, or the theme's accent when that colour would not stand out from the page. */
    static void ring(HubScreen.Frame f, LunarMoodClient.View view, int day, int x, int y, int size) {
        view.onDay(day).ifPresent(e -> {
            int color = HubAlmanac.ringColor(e.color(), f.theme().paper(), f.theme().accent());
            int reach = HubAlmanac.RING_REACH;
            KitDraw.tickRing(f.g, x + size / 2, y + size / 2, size / 2 + reach, 16, 1f, color, color);
            f.target("lunar:" + day, new Rect(x - reach, y - reach, size + 2 * reach, size + 2 * reach), List.of(line(e)), null);
        });
    }
}
