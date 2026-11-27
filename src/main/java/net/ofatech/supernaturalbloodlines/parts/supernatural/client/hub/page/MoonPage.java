package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubDev;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.client.EclipseClient;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.MoonCalendar;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelRow;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;

/**
 * The moon calendar (R140): the next 16 days as moons (two cycles, full moons in the accent colour, a ring where Enhanced Celestials has an event),
 * and beside it what tonight is, when the full moons come and the events. A route over the page it was opened from, drawn the same in every theme;
 * Esc or its Back button return. It extends the almanac of the Path pages, which stays as it was.
 */
public final class MoonPage implements HubPageView {
    static final String KEY = PageKit.HUB + "moon.";
    private static final int COLUMNS = 8;

    /** What tonight is for this client: the moon, the time, Enhanced Celestials' mood and the eclipse the server announced. */
    public static MoonCalendar.Night tonight(Level level) {
        return MoonCalendar.tonight(level.dimensionType().hasFixedTime(), HubDev.moonPhase(level), level.getDayTime(), LunarMoods.now(level), EclipseClient.running());
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        Level level = f.view.player().level();
        int phase = Math.floorMod(HubDev.moonPhase(level), 8);
        var events = AlmanacEvents.view(f);

        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        Component back = Component.translatable(ProfilePage.KEY + "back", Component.translatable(f.screen().page().langKey()));
        Rect b = new Rect(r.x(), y, Math.min(r.w(), PageWidgets.widthOf(f.font(), back)), PageWidgets.H);
        PageWidgets.button(f, "stage:moon:back", b, back, true, false, null, f.screen()::back);
        y = b.bottom() + 4;
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 8;

        int cell = Math.max(12, r.w() / COLUMNS);
        int icon = Math.max(8, Math.min(16, cell - 4));
        int rowH = icon + PageKit.LINE + 6;
        boolean[] thin = new boolean[2];
        for (int row = 0; row < 2; row++) {
            int widest = 0;
            for (int col = 0; col < COLUMNS; col++) {
                widest = Math.max(widest, f.font().width(dayLabel(row * COLUMNS + col)));
            }
            thin[row] = LabelRow.thin(widest, cell);
        }
        for (MoonCalendar.Day day : MoonCalendar.days(phase, MoonCalendar.DAYS)) {
            int col = day.offset() % COLUMNS;
            int row = day.offset() / COLUMNS;
            int x = r.x() + col * cell + (cell - icon) / 2;
            int gy = y + row * rowH + 2;
            Rect box = new Rect(r.x() + col * cell, gy - 2, cell, rowH - 2);
            String id = "moon:day:" + day.offset();
            events.onCalendarDay(day.offset()).ifPresent(e -> {
                int color = net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac.ringColor(e.color(), t.paper(), t.accent());
                KitDraw.tickRing(f.g, x + icon / 2, gy + icon / 2, icon / 2 + 2, 16, 1f, color, color);
            });
            KitDraw.icon(f.g, Icons.moon(day.phase()), x, gy, icon, day.full() ? t.accent() : t.ink());
            if (LabelRow.shown(col, thin[row], MoonCalendar.daysToFull(phase) % COLUMNS)) {
                Component label = dayLabel(day.offset());
                f.g.drawString(f.font(), label, LabelRow.x(r.x(), col, cell, COLUMNS, f.font().width(label)), gy + icon + 2, day.full() ? t.accent() : t.muted(), false);
            }
            f.target(id, box, dayTip(day, events), null);
            if (f.focused(id)) {
                t.drawFocus(f.g, box);
            }
        }
        y += 2 * rowH + 4;
        y = PageKit.wrapped(f, Component.translatable(KEY + "legend"), r.x(), y, r.w(), t.muted());
        f.end(y);
        PageWidgets.info(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "info"));

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = side(f, s, sy, level, phase, events);
        f.end(sy);
    }

    private static Component dayLabel(int offset) {
        return offset == 0 ? Component.translatable(KEY + "now") : Component.literal("+" + offset);
    }

    private static List<Component> dayTip(MoonCalendar.Day day, net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.LunarMoodClient.View events) {
        List<Component> tip = new ArrayList<>();
        Component phase = Component.translatable(KEY + "phase." + day.phase());
        tip.add(day.offset() == 0 ? Component.translatable(KEY + "day_today", phase)
                : day.offset() == 1 ? Component.translatable(KEY + "day_tomorrow", phase) : Component.translatable(KEY + "day_in", day.offset(), phase));
        events.onCalendarDay(day.offset()).ifPresent(e -> tip.add(AlmanacEvents.line(e)));
        return tip;
    }

    private int side(HubScreen.Frame f, Rect s, int y, Level level, int phase, net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.LunarMoodClient.View events) {
        HubTheme t = f.theme();
        y = PageKit.wrapped(f, Component.translatable(KEY + "tonight"), s.x(), y, s.w(), t.accent());
        MoonCalendar.Night night = tonight(level);
        y = PageKit.wrapped(f, Component.translatable(KEY + "night." + night.name().toLowerCase(java.util.Locale.ROOT)), s.x(), y + 1, s.w(), t.ink());
        WorldNowHook.Flags world = f.view.snapshot().world();
        boolean fullNight = night == MoonCalendar.Night.FULL_MOON || night == MoonCalendar.Night.ECLIPSE;
        if (fullNight && world.sleepRule()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "no_sleep"), s.x(), y + 2, s.w(), t.ink());
        }
        if (world.awakeningPending()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "stirs"), s.x(), y + 2, s.w(), t.ink());
        }
        y = PageKit.wrapped(f, Component.translatable(KEY + "full_moons"), s.x(), y + 6, s.w(), t.accent());
        for (int days : MoonCalendar.fullMoons(phase, MoonCalendar.DAYS)) {
            y = PageKit.wrapped(f, days == 0 ? Component.translatable(KEY + "full_today") : Component.translatable(KEY + "full_in", days), s.x(), y + 1, s.w(), t.ink());
        }
        List<net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming> listed = new ArrayList<>(events.calendar().values());
        if (!listed.isEmpty()) {
            listed.sort(Comparator.comparingInt(net.ofatech.supernaturalbloodlines.core.moon.LunarSource.Upcoming::days));
            y = PageKit.wrapped(f, Component.translatable(KEY + "events"), s.x(), y + 6, s.w(), t.accent());
            for (var e : listed) {
                y = PageKit.wrapped(f, AlmanacEvents.line(e), s.x(), y + 1, s.w(), t.ink());
            }
        }
        return y;
    }
}
