package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;

/** Rites: the rite tasks of your next rungs (with their claim stamps) on the left; how rites are done, and the witch's bonus, on the right. */
public final class RitesPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "rites.";

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        List<HubModel.TaskRow> rites = new ArrayList<>();
        f.view.ladders().forEach(l -> l.tasks().stream().filter(RitesPage::isRite).forEach(rites::add));
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.accent());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 6;
        for (HubModel.TaskRow row : rites) {
            y = PageKit.taskRow(f, row, r.x(), y, r.w(), true, null, false);
        }
        if (rites.isEmpty()) {
            y = PageKit.wrapped(f, Component.translatable(KEY + "none"), r.x(), y, r.w(), t.muted());
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.wrapped(f, Component.translatable(KEY + "how"), s.x(), sy, s.w(), t.ink()) + 4;
        boolean witch = f.view.races().contains(SupernaturalRace.WITCH) || f.view.races().contains(SupernaturalRace.SIPHONER);
        sy = PageKit.wrapped(f, Component.translatable(KEY + (witch ? "witch" : "not_witch")), s.x(), sy, s.w(), t.muted()) + 4;
        sy = PageKit.wrapped(f, Component.translatable(KEY + "book"), s.x(), sy, s.w(), t.muted());
        f.end(sy);
    }

    private static boolean isRite(HubModel.TaskRow row) {
        return row.task().leaves().stream().anyMatch(l -> l.detector() == TaskSpec.Detector.RITE);
    }
}
