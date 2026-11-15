package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;

/** Path: the left page is your story and your ladders (rungs as chapters), the right page the next rung's tasks with their claim stamps. */
public final class PathPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "path.";

    @Override
    public void render(HubScreen.Frame f) {
        story(f);
        next(f);
    }

    private static void story(HubScreen.Frame f) {
        HubView v = f.view;
        HubTheme t = f.theme();
        Rect r = f.region(0);
        int y = f.begin(0);
        f.g.drawString(f.font(), Component.translatable(KEY + "book_of"), r.x(), y, t.muted(), false);
        y = PageKit.title(f, v.player().getName(), r.x(), y + PageKit.LINE, r.w(), t.accent());
        y = PageKit.wrapped(f, subtitle(v), r.x(), y + 1, r.w(), t.muted());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        boolean many = v.ladders().size() > 1;
        for (HubModel.LadderView ladder : v.ladders()) {
            if (many) {
                f.g.drawString(f.font(), PageKit.race(ladder.race()).copy().withStyle(ChatFormatting.BOLD), r.x(), y, t.accent(), false);
                y += PageKit.LINE + 1;
            }
            for (HubModel.RungRow row : ladder.rungs()) {
                y = rung(f, ladder, row, r, y);
            }
            y += 3;
        }
        t.drawRule(f.g, r.x(), y, r.w());
        y += 4;
        y = PageKit.wrapped(f, Component.translatable(KEY + "sp", v.state().sp(), v.state().spEarned()), r.x(), y, r.w(), t.ink());
        f.end(0, y);
    }

    private static Component subtitle(HubView v) {
        MutableComponent races = Component.empty();
        for (int i = 0; i < v.races().size(); i++) {
            races.append(i == 0 ? Component.empty() : Component.literal(" \u00b7 ")).append(PageKit.race(v.races().get(i)));
        }
        v.snapshot().sire().ifPresent(sire -> races.append(Component.literal(" \u00b7 "))
                .append(Component.translatable(v.snapshot().severed() ? KEY + "sire_severed" : KEY + "sire", sire)));
        return races;
    }

    private static int rung(HubScreen.Frame f, HubModel.LadderView ladder, HubModel.RungRow row, Rect r, int y) {
        HubTheme t = f.theme();
        boolean current = row.status() == HubModel.RungStatus.CURRENT;
        int h = current ? PageKit.LINE + 5 : PageKit.LINE + 1;
        t.drawRungMark(f.g, r.x(), y, row.status());
        int color = row.status() == HubModel.RungStatus.LOCKED ? t.muted() : current ? t.accent() : t.ink();
        Component status = status(ladder, row);
        int sw = f.font().width(status);
        String name = f.font().plainSubstrByWidth(PageKit.rung(row).getString(), r.w() - HubTheme.MARK - 4 - sw - 4);
        f.g.drawString(f.font(), name, r.x() + HubTheme.MARK + 4, y + 1, color, false);
        f.g.drawString(f.font(), status, r.right() - sw, y + 1, current ? t.accent() : t.muted(), false);
        if (current && row.tasks() > 0) {
            t.drawBar(f.g, new Rect(r.x() + HubTheme.MARK + 4, y + PageKit.LINE + 1, Math.min(70, r.w() / 2), 4), row.claimed() / (float) row.tasks());
        }
        Rect hit = new Rect(r.x(), y, r.w(), h - 1);
        f.target(hit, tooltip(row), null);
        if (f.focused(hit)) {
            t.drawFocus(f.g, hit);
        }
        return y + h;
    }

    private static Component status(HubModel.LadderView ladder, HubModel.RungRow row) {
        return switch (row.status()) {
            case REACHED -> Component.translatable(KEY + "reached");
            case CURRENT -> Component.translatable(KEY + "of", row.claimed(), row.tasks());
            case LOCKED -> Component.translatable(row.index() == ladder.rungs().size() - 1 && apexLadder(ladder.race()) ? KEY + "apex" : KEY + "sealed");
        };
    }

    private static boolean apexLadder(SupernaturalRace race) {
        return race != SupernaturalRace.HUMAN && race != SupernaturalRace.GHOUL;
    }

    private static List<Component> tooltip(HubModel.RungRow row) {
        List<Component> out = new ArrayList<>();
        out.add(PageKit.rung(row));
        out.add(Component.translatable(KEY + "rung_state." + row.status().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        if (row.index() > 0) {
            out.add(reward(row).copy().withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    private static Component reward(HubModel.RungRow row) {
        return row.ep() > 0 && ModList.get().isLoaded("tensura")
                ? Component.translatable(KEY + "reward_ep", row.sp(), String.format("%,d", row.ep()))
                : Component.translatable(KEY + "reward", row.sp());
    }

    private static void next(HubScreen.Frame f) {
        HubView v = f.view;
        HubTheme t = f.theme();
        Rect r = f.region(1);
        int y = f.begin(1);
        if (v.ghoul()) {
            y = PageKit.title(f, Component.translatable(KEY + "turning"), r.x(), y, r.w(), t.accent());
            t.drawRule(f.g, r.x(), y + 2, r.w());
            y = PageKit.wrapped(f, Component.translatable(KEY + "turning_text"), r.x(), y + 6, r.w(), t.ink());
            int ticks = v.bloodline().turningTicksLeft();
            if (ticks > 0) {
                int s = ticks / 20;
                y = PageKit.wrapped(f, Component.translatable(KEY + "turning_left", s / 60, String.format("%02d", s % 60)), r.x(), y + 4, r.w(), t.accent());
            }
            f.end(1, y);
            return;
        }
        boolean any = false;
        for (HubModel.LadderView ladder : v.ladders()) {
            if (ladder.atTop()) {
                continue;
            }
            any = true;
            HubModel.RungRow active = ladder.active().orElseThrow();
            f.g.drawString(f.font(), Component.translatable(KEY + "to_become"), r.x(), y, t.muted(), false);
            y = PageKit.title(f, PageKit.rung(active), r.x(), y + PageKit.LINE, r.w(), t.accent());
            t.drawRule(f.g, r.x(), y + 2, r.w());
            y += 6;
            for (HubModel.TaskRow task : ladder.tasks()) {
                y = PageKit.taskRow(f, task, r.x(), y, r.w(), true, null, false);
            }
            y = PageKit.wrapped(f, reward(active), r.x(), y + 2, r.w(), t.muted()) + 6;
        }
        if (!any) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "complete"), r.x(), y, r.w(), t.ink());
        }
        f.end(1, y);
    }
}
