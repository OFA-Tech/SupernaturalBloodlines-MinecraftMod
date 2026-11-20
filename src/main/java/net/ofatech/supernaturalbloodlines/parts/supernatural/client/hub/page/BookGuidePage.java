package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.GuideChapters;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;

/**
 * The Grimoire's Guide: contents on the left (roman numerals, dotted leaders to the page), the chapter on the right with a drop cap, and a dog-ear
 * that turns to the next chapter.
 */
public final class BookGuidePage implements HubPageView {
    private static final String KEY = PageKit.HUB + "guide.";
    private static final String BOOK = PageKit.HUB + "book.guide.";
    private String chapter;

    /** The page a chapter is on: every chapter is read on the Guide spread, so the Contents gives the spread's right-hand page, the one the footer shows. */
    static int chapterPage() {
        return 2 * HubPage.GUIDE.ordinal() + 2;
    }

    static String roman(int n) {
        int[] v = {10, 9, 5, 4, 1};
        String[] s = {"X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            while (n >= v[i]) {
                out.append(s[i]);
                n -= v[i];
            }
        }
        return out.toString();
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubPage.Context c = f.view.context();
        List<String> ids = GuideChapters.forRaces(c.creature(), c.magic(), c.werewolves(), c.enchanted(), c.loadout(), net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("bosses"));
        chapter = GuideLinks.chapterOr(chapter);
        if (chapter == null || !ids.contains(chapter)) {
            chapter = ids.get(0);
        }
        HubTheme t = f.theme();
        Rect r = f.region(BookKit.MAIN);
        int y = BookKit.head(f, BookKit.MAIN, Component.translatable(KEY + "title"), Component.translatable(KEY + "subtitle"));
        y = BookKit.caps(f, Component.translatable(BOOK + "contents"), r.x(), y, t.muted());
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            boolean on = id.equals(chapter);
            Rect hit = new Rect(r.x() - 1, y - 1, r.w() + 2, 12);
            if (on) {
                f.g.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), (t.accent() & 0xFFFFFF) | 0x22000000);
            }
            Component name = Component.translatable(GuideChapters.titleKey(id));
            f.g.drawString(f.font(), roman(i + 1) + ".", r.x(), y, on ? t.accent() : t.muted(), false);
            BookKit.leader(f, name, Component.translatable(BOOK + "page", chapterPage()), r.x() + 30, y, r.w() - 30, on ? t.accent() : t.muted());
            String target = "chapter:" + id;
            f.target(target, hit, List.of(name), () -> chapter = id);
            if (f.focused(target)) {
                KitDraw.focusRing(f.g, hit, t.focusColor());
            }
            y += 12;
        }
        f.end(y);

        Rect s = f.region(BookKit.SIDE);
        int sy = BookKit.head(f, BookKit.SIDE, Component.translatable(GuideChapters.titleKey(chapter)), null);
        sy = BookKit.dropCap(f, Component.translatable(GuideChapters.bodyKey(chapter)), s.x(), sy, s.w(), t.ink());
        if (GuideLinks.CHAPTER.equals(chapter)) {
            Component to = Component.translatable(KEY + "bosses.link");
            sy = BookKit.link(f, "guide:bloodline", to, s.x(), sy + 6, List.of(Component.translatable("supernaturalbloodlines.hub.page.bloodline")),
                    () -> f.screen().setPage(net.ofatech.supernaturalbloodlines.shared.hub.HubPage.BLOODLINE));
        }
        int at = ids.indexOf(chapter);
        if (at < ids.size() - 1) {
            String next = ids.get(at + 1);
            Component label = Component.translatable(BOOK + "turn");
            BookKit.link(f, "guide:turn", label, s.right() - f.font().width(label), Math.max(sy + 6, s.bottom() - 12), List.of(Component.translatable(GuideChapters.titleKey(next))),
                    () -> chapter = next);
        }
        f.end(Math.max(sy + 6, s.bottom()));
    }
}
