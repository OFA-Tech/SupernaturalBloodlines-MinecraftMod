package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.GuideChapters;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;

/** The Guide: the chapters about what you are (your creature, your magic) and how the hub and the game around it work. Left the list, right the chapter. */
public final class GuidePage implements HubPageView {
    private static final String KEY = PageKit.HUB + "guide.";
    private String chapter;

    @Override
    public void render(HubScreen.Frame f) {
        HubPage.Context c = f.view.context();
        List<String> ids = GuideChapters.forRaces(c.creature(), c.magic(), c.werewolves(), c.enchanted(), c.loadout());
        if (chapter == null || !ids.contains(chapter)) {
            chapter = ids.get(0);
        }
        HubTheme t = f.theme();
        Rect r = f.region(HubLayout.MAIN);
        int y = f.begin(HubLayout.MAIN);
        y = PageKit.title(f, Component.translatable(KEY + "title"), r.x(), y, r.w(), t.ink());
        y = PageKit.wrapped(f, Component.translatable(KEY + "subtitle"), r.x(), y, r.w(), t.muted());
        t.drawRule(f.g, r.x(), y + 2, r.w());
        y += 8;
        for (String id : ids) {
            Component name = Component.translatable(GuideChapters.titleKey(id));
            boolean on = id.equals(chapter);
            Rect hit = new Rect(r.x(), y - 1, r.w(), PageKit.LINE + 1);
            f.g.drawString(f.font(), on ? Component.literal("\u203a ").append(name) : name, r.x() + (on ? 0 : 6), y, on ? t.accent() : t.ink(), false);
            String target = "chapter:" + id;
            f.target(target, hit, List.of(name), () -> chapter = id);
            if (f.focused(target)) {
                t.drawFocus(f.g, hit);
            }
            y += PageKit.LINE + 2;
        }
        f.end(y);

        Rect s = f.region(HubLayout.SIDE);
        int sy = f.begin(HubLayout.SIDE);
        sy = PageKit.title(f, Component.translatable(GuideChapters.titleKey(chapter)), s.x(), sy, s.w(), t.accent());
        t.drawRule(f.g, s.x(), sy + 2, s.w());
        sy = PageKit.wrapped(f, Component.translatable(GuideChapters.bodyKey(chapter)), s.x(), sy + 6, s.w(), t.ink());
        f.end(sy);
    }
}
