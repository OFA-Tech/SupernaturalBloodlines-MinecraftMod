package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubThemeViews;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.GrimoireBloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookGuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookQuestsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookSkillsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Counters;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Easing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Motion;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TickRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitWidgets;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiMotion;

/**
 * The default theme, a real open book: a leather cover with a gold line, two parchment pages with spine shading and stacked page edges,
 * page numbers, index tabs out of the right edge, wax seals on cords below, a brass clasp that switches the look, SP and EP as a margin note
 * in red ink, and an ink sigil round the live player model. Everything on the pages is the shared kit and the shared page views.
 */
public final class GrimoireTheme implements HubTheme, HubThemeViews {
    private static final int GOLD = 0xFFB8903A;
    private static final int INK_RED = 0xFF7A1218;

    private final HubPageView path = new PathPage(true);
    private final HubPageView skills = new BookSkillsPage();
    private final HubPageView quests = new BookQuestsPage();
    private final HubPageView bloodline = new GrimoireBloodlinePage();
    private final HubPageView pack = new BookPackPage();
    private final HubPageView guide = new BookGuidePage();

    /** The Path page of the book carries the almanac and the sigil emblem; every other page is the shared one. */
    @Override
    public HubPageView view(HubPage page) {
        return switch (page) {
            case PATH -> path;
            case SKILLS -> skills;
            case QUESTS -> quests;
            case BLOODLINE -> bloodline;
            case PACK -> pack;
            case GUIDE -> guide;
            default -> HubThemeViews.super.view(page);
        };
    }

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return GrimoireLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubLayout layout(int width, int height, java.util.List<HubPage> pages, int seals) {
        return GrimoireLayout.of(width, height, pages, seals);
    }

    @Override
    public String kit() {
        return "grimoire";
    }

    @Override
    public String titleFont() {
        return "title_gothic";
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        KitDraw.activeTheme(this);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xA0100707);
        Rect b = l.frame();
        g.fill(b.x() - 3, b.y() - 3, b.right() + 3, b.bottom() + 4, 0x80000000);
        // the cover's own band (13 px) is half under the pages (the pages start 6 px inside the book): draw it 6 px out
        UiKit.draw(g, this, Part.COVER, b.x() - COVER_OUT, b.y() - COVER_OUT, b.w() + 2 * COVER_OUT, b.h() + 2 * COVER_OUT);
        Rect lp = GrimoireLayout.page(l.region(HubLayout.MAIN));
        Rect rp = GrimoireLayout.page(l.region(HubLayout.SIDE));
        UiKit.draw(g, this, Part.PANEL, lp);
        UiKit.draw(g, this, Part.PANEL, rp);
        // the gutter: the paper darkens towards the spine
        KitDraw.batch(g, () -> {
            for (int i = 0; i < 8; i++) {
                int a = (0x50 - i * 10) << 24;
                g.fill(lp.right() - 1 - i, lp.y(), lp.right() - i, lp.bottom(), a | 0x46280a);
                g.fill(rp.x() + i, rp.y(), rp.x() + i + 1, rp.bottom(), a | 0x46280a);
            }
        });
        g.fill(lp.right(), b.y() + 2, rp.x(), b.bottom() - 2, 0xFF2a1708);
        g.fill(lp.right() + 1, b.y() + 2, lp.right() + 2, b.bottom() - 2, 0xFF5a3a1c);
        g.fill(rp.x() - 2, b.y() + 2, rp.x() - 1, b.bottom() - 2, 0xFF5a3a1c);
    }

    private static final int COVER_OUT = 6;

    @Override
    public void drawFooter(UiContext ctx, HubLayout layout, HubPage page) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        int left = 2 * page.ordinal() + 1;
        for (int i = 0; i < 2; i++) {
            Rect pg = GrimoireLayout.page(layout.region(i == 0 ? HubLayout.MAIN : HubLayout.SIDE));
            String n = "~ " + (left + i) + " ~";
            Rect r = GrimoireLayout.number(pg);
            g.drawString(font, n, r.x() + (r.w() - font.width(n)) / 2, r.y(), muted(), false);
        }
    }

    /** The index tab colour family of a nav group: the same ink on each, so the colour tells the group and never carries the meaning alone. */
    public static int tabColor(HubPage.Group group, boolean selected) {
        return switch (group) {
            case NONE, ME -> selected ? 0xFF9A1820 : 0xFF6A1A20;
            case PEOPLE -> selected ? 0xFF8A5A14 : 0xFF5E3C10;
            case HELP -> selected ? 0xFF2E7A4A : 0xFF1F5232;
            case SYSTEM -> selected ? 0xFF5A5058 : 0xFF3C343C;
            case OPERATOR -> selected ? 0xFF2A2024 : 0xFF1C1416;
        };
    }

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        // the chosen tab slides 4 texels out of the edge (reduce motion: it is out at once)
        int w = r.w() + UiMotion.slide("grimoire:tab:" + page.name(), selected, 4, Motion.PAGE_CHANGE, Easing.OUT_QUAD);
        UiKit.draw(g, this, selected ? Part.TAB_SELECTED : Part.TAB, r.x(), r.y(), w, r.h());
        // the group is told by the marker as well as the colour of the tab
        g.fill(r.x() + 4, r.y() + 3, r.x() + 6, r.bottom() - 3, tabColor(page.group(), selected) | 0xFF000000);
        if (page.group() == HubPage.Group.OPERATOR) {
            KitDraw.dashed(g, new Rect(r.x() + 1, r.y(), w - 1, r.h()), GOLD);
        }
        KitDraw.icon(g, Icons.of(page), r.x() + 8 + (w - 8 - 14) / 2, r.y() + (r.h() - 14) / 2, 14, selected ? 0xFFFFFFFF : 0xC0FFFFFF);
        if (focused) {
            KitDraw.focusRing(g, new Rect(r.x(), r.y(), w, r.h()), focusColor());
        }
    }

    /** A wax seal hanging on a cord from the book's bottom edge. */
    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        int cx = r.x() + r.w() / 2;
        g.fill(cx, r.y() - GrimoireLayout.CORD, cx + 1, r.y(), GOLD);
        int size = Math.min(r.w(), r.h());
        UiKit.draw(g, this, lit ? Part.SEAL_HOVER : Part.SEAL, r.x() + (size - SEAL_PX) / 2, r.y() + (size - SEAL_PX) / 2, SEAL_PX, SEAL_PX);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, 0xFFF0DCB0, false);
    }

    private static final int SEAL_PX = 20;

    /** The brass clasp that switches the look. */
    @Override
    public void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.button(g, this, r, Icons.THEME, hot, focused);
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.button(g, this, r, Icons.CLOSE, hot, focused);
    }

    /** SP and EP as a red ink margin note in the header of the left page. */
    @Override
    public void drawCounters(UiContext ctx, Rect r, Counters c) {
        String ep = c.epText();
        String text = c.sp() + " SP" + (ep == null ? "" : "  \u00b7  " + ep);
        Font font = ctx.font();
        int x = r.right() - font.width(text);
        ctx.g().drawString(font, text, x, r.y(), INK_RED, false);
        ctx.g().fill(x, r.y() + 9, r.right(), r.y() + 10, INK_RED);
    }

    /** The ink sigil: two circles and a five-pointed star round the live model, with the ladder's progress as a ring of ticks outside. */
    @Override
    public void drawEmblem(UiContext ctx, Rect r, EmblemData data) {
        GuiGraphics g = ctx.g();
        int cx = r.x() + r.w() / 2;
        int cy = r.y() + r.h() / 2;
        int radius = Math.min(r.w(), r.h()) / 2 - 2;
        int sr = radius - 9;
        // the whole sigil is plain fills: one batch, not one draw call per pixel
        KitDraw.batch(g, () -> {
            KitDraw.tickRing(g, cx, cy, radius, 36, data.progress(), INK_RED, 0xFFA89870);
            ring(g, cx, cy, radius - 5, ink());
            ring(g, cx, cy, radius - 9, 0xFF8A7A5A);
            int[] px = new int[5];
            int[] py = new int[5];
            for (int i = 0; i < 5; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / 5;
                px[i] = cx + (int) Math.round(sr * Math.cos(a));
                py[i] = cy + (int) Math.round(sr * Math.sin(a));
            }
            for (int i = 0; i < 5; i++) {
                KitDraw.line(g, px[i], py[i], px[(i + 2) % 5], py[(i + 2) % 5], 0xFF8A7A5A);
            }
        });
        int inner = (int) (sr * 1.25);
        KitDraw.model(g, new Rect(cx - inner / 2, cy - inner / 2 - 2, inner, inner), data.entity(), data.drag());
    }

    private static void ring(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (TickRing.Tick t : TickRing.ticks(cx, cy, radius, Math.max(24, radius * 6), 0f)) {
            g.fill(t.x(), t.y(), t.x() + 1, t.y() + 1, color);
        }
    }

    @Override
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        KitWidgets.scrollbar(g, this, region, offset, max);
    }

    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, RungStatus status) {
        KitWidgets.rungMark(g, this, x, y, status);
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, TaskState state) {
        KitWidgets.taskBox(g, this, x, y, BOX, state);
    }

    @Override
    public void drawStamp(GuiGraphics g, int x, int y, boolean lit) {
        KitWidgets.stamp(g, this, x, y, lit);
    }

    @Override
    public void drawRule(GuiGraphics g, int x, int y, int w) {
        KitWidgets.rule(g, this, x, y, w);
    }

    @Override
    public void drawBar(GuiGraphics g, Rect r, float fraction) {
        KitWidgets.bar(g, this, r, fraction);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        UiKit.focus(g, this, r);
    }

    @Override
    public int paper() {
        return 0xFFe9dcbc;
    }

    @Override
    public int ink() {
        return 0xFF3b2414;
    }

    @Override
    public int muted() {
        return 0xFF5e4228; // 6.9:1 on the parchment (#e9dcbc), readable small and at GUI scale 1
    }

    @Override
    public int accent() {
        return 0xFF7a1218;
    }

    @Override
    public int good() {
        return 0xFF245a3a;
    }

    @Override
    public int sourceColor(String source) {
        return switch (source) {
            case "vampirism" -> 0xFF9a1820;
            case "werewolves" -> 0xFF7a4508;
            case "enchanted" -> 0xFF245a3a;
            case "tensura" -> 0xFF16566a;
            default -> 0xFF5a2f9a;
        };
    }
}
