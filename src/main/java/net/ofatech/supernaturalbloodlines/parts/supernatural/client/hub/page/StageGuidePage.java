package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.GuideChapters;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * The Guide for the Blood Moon and Tensura: a chapter list card (the first row says what you are) and a chapter card, with Previous and Next in the
 * theme's detail. The text is the same language-file chapters as the Grimoire's.
 */
public final class StageGuidePage implements HubPageView {
    private static final String KEY = PageKit.HUB + "guide.";
    private static final String BOOK = PageKit.HUB + "book.guide.";
    private String chapter;

    @Override
    public void render(HubScreen.Frame f) {
        HubPage.Context c = f.view.context();
        List<String> ids = GuideChapters.forRaces(c.creature(), c.magic(), c.werewolves(), c.enchanted(), c.loadout(), net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses"));
        if (chapter == null || !ids.contains(chapter)) {
            chapter = ids.get(0);
        }
        HubTheme t = f.theme();
        BookKit.moonHead(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "subtitle"));
        Rect r = f.region(BookKit.MAIN);
        int y = f.begin(BookKit.MAIN);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            boolean on = id.equals(chapter);
            Rect hit = new Rect(r.x(), y, r.w(), 15);
            if (on) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
                f.g.fill(hit.x(), hit.y(), hit.x() + 2, hit.bottom(), t.accent());
            }
            f.g.drawString(f.font(), String.format("%02d", i + 1), r.x() + 6, y + 4, t.muted(), false);
            Component name = Component.translatable(GuideChapters.titleKey(id));
            f.g.drawString(f.font(), LabelFit.cut(name.getString(), r.w() - 34, f.font()::width), r.x() + 26, y + 4, on ? t.ink() : t.muted(), false);
            String target = "chapter:" + id;
            f.target(target, hit, List.of(name), () -> chapter = id);
            if (f.focused(target)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += 17;
        }
        f.end(y);

        Rect s = f.region(BookKit.SIDE);
        int at = ids.indexOf(chapter);
        int sy = f.begin(BookKit.SIDE);
        sy = BookKit.caps(f, Component.translatable(BOOK + "chapter_of", at + 1, ids.size()), s.x(), sy, t.muted());
        sy = PageKit.title(f, Component.translatable(GuideChapters.titleKey(chapter)), s.x(), sy, s.w(), t.ink());
        t.drawRule(f.g, s.x(), sy + 2, s.w());
        sy = PageKit.wrapped(f, Component.translatable(GuideChapters.bodyKey(chapter)), s.x(), sy + 8, s.w(), t.ink());
        f.end(sy);

        String prev = at > 0 ? ids.get(at - 1) : null;
        String next = at < ids.size() - 1 ? ids.get(at + 1) : null;
        Detail.Builder d = Detail.of(Component.translatable(GuideChapters.titleKey(chapter))).body(Component.translatable(BOOK + "updates"));
        d.action("prev", Component.translatable(BOOK + "previous"), false, prev != null, () -> chapter = prev);
        d.action("next", Component.translatable(BOOK + "next"), true, next != null, () -> chapter = next);
        f.detail(d.build());
    }
}
