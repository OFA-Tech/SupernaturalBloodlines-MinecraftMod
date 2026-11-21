package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TickRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;

/**
 * The default theme, a real open book: a leather cover with a gold line, two parchment pages with spine shading and stacked page edges,
 * page numbers, index tabs out of the right edge, wax seals on cords below, a brass clasp that switches the look, SP and EP as a margin note
 * in red ink, and an ink sigil round the live player model. Everything on the pages is the shared kit and the shared page views.
 */
public final class GrimoireTheme implements HubTheme, HubThemeViews {
    private static final ResourceLocation PARCHMENT = tex("parchment");
    private static final ResourceLocation WIDGETS = tex("widgets");
    private static final int WIDGETS_W = 64;
    private static final int WIDGETS_H = 32;
    private static final int LEATHER = 0xFF4A1717;
    private static final int LEATHER_DARK = 0xFF260A0A;
    private static final int GOLD = 0xFFB8903A;
    private static final int GOLD_DARK = 0xFF5A4010;
    private static final int INK_RED = 0xFF7A1218;
    private static final int[] EDGES = {0xFFD9C9A2, 0xFFC4B088, 0xFFAE9868};

    private final HubPageView path = new PathPage(true);
    private final HubPageView skills = new BookSkillsPage();
    private final HubPageView quests = new BookQuestsPage();
    private final HubPageView bloodline = new GrimoireBloodlinePage();
    private final HubPageView pack = new BookPackPage();
    private final HubPageView guide = new BookGuidePage();

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hub/grimoire/" + name + ".png");
    }

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
    public void drawFrame(GuiGraphics g, HubLayout l) {
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xA0100707);
        Rect b = l.frame();
        g.fill(b.x() - 3, b.y() - 3, b.right() + 3, b.bottom() + 4, 0x80000000);
        g.fill(b.x() - 1, b.y() - 1, b.right() + 1, b.bottom() + 1, LEATHER_DARK);
        g.fill(b.x(), b.y(), b.right(), b.bottom(), LEATHER);
        g.renderOutline(b.x() + 2, b.y() + 2, b.w() - 4, b.h() - 4, GOLD);
        Rect lp = GrimoireLayout.page(l.region(HubLayout.MAIN));
        Rect rp = GrimoireLayout.page(l.region(HubLayout.SIDE));
        stackedEdges(g, lp, true);
        stackedEdges(g, rp, false);
        paper(g, lp);
        paper(g, rp);
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
        g.renderOutline(lp.x(), lp.y(), lp.w(), lp.h(), 0xFF7a5a32);
        g.renderOutline(rp.x(), rp.y(), rp.w(), rp.h(), 0xFF7a5a32);
    }

    /** The stack of leaves under a page: three paper edges peeking out below it and on its outer side. */
    private static void stackedEdges(GuiGraphics g, Rect page, boolean left) {
        for (int k = EDGES.length; k >= 1; k--) {
            int c = EDGES[k - 1];
            g.fill(page.x() + (left ? -k : 0), page.y() + k, page.right() + (left ? 0 : k), page.bottom() + k, c);
        }
    }

    private static void paper(GuiGraphics g, Rect r) {
        for (int y = r.y(); y < r.bottom(); y += 64) {
            for (int x = r.x(); x < r.right(); x += 64) {
                g.blit(PARCHMENT, x, y, 0, 0, Math.min(64, r.right() - x), Math.min(64, r.bottom() - y), 64, 64);
            }
        }
    }

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
        int w = selected ? r.w() + 4 : r.w();
        g.fill(r.x(), r.y() - 1, r.x() + w + 1, r.bottom() + 1, LEATHER_DARK);
        g.fill(r.x(), r.y(), r.x() + w, r.bottom(), tabColor(page.group(), selected));
        if (selected) {
            g.renderOutline(r.x() + 1, r.y() + 1, w - 2, r.h() - 2, GOLD);
        }
        if (page.group() == HubPage.Group.OPERATOR) {
            KitDraw.dashed(g, new Rect(r.x() + 1, r.y(), w - 1, r.h()), GOLD);
        }
        KitDraw.icon(g, Icons.of(page), r.x() + 4 + (w - 4 - 12) / 2, r.y() + (r.h() - 12) / 2, 12, selected ? 0xFFFFE3A0 : 0xFFE8B0B0);
        if (focused) {
            KitDraw.focusRing(g, new Rect(r.x(), r.y(), w, r.h()), focusColor());
        }
    }

    /** A wax seal hanging on a cord from the book's bottom edge. */
    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        int cx = r.x() + r.w() / 2;
        g.fill(cx, r.y() - GrimoireLayout.CORD, cx + 1, r.y(), GOLD);
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x(), r.y(), Math.min(r.w(), r.h()), lit ? 0xFFC03038 : 0xFF9A1820);
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x() + 2, r.y() + 2, Math.min(r.w(), r.h()) - 4, lit ? 0xFFA82028 : 0xFF7A1218);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, 0xFFF0DCB0, false);
    }

    /** The brass clasp that switches the look. */
    @Override
    public void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), GOLD_DARK);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, hot ? 0xFFD4AC4E : GOLD);
        g.fill(r.x() + 1, r.y() + r.h() / 2, r.right() - 1, r.y() + r.h() / 2 + 1, GOLD_DARK);
        KitDraw.icon(g, Icons.THEME, r.x() + (r.w() - 12) / 2, r.y() + (r.h() - 12) / 2, 12, 0xFF2A1808);
        if (focused) {
            KitDraw.focusRing(g, r, focusColor());
        }
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.icon(g, Icons.CLOSE, r.x() + (r.w() - 12) / 2, r.y() + (r.h() - 12) / 2, 12, hot ? 0xFFE8B84A : GOLD);
        if (focused) {
            KitDraw.focusRing(g, r, 0xFFE8B84A);
        }
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
        int x = region.right() + 2;
        g.fill(x, region.y(), x + 2, region.bottom(), 0x40472a10);
        int thumb = Math.max(8, region.h() * region.h() / (region.h() + max));
        int y = region.y() + (region.h() - thumb) * offset / Math.max(1, max);
        g.fill(x, y, x + 2, y + thumb, 0xFF9a1820);
    }

    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, RungStatus status) {
        g.blit(WIDGETS, x, y, status.ordinal() * 10, 0, MARK, MARK, WIDGETS_W, WIDGETS_H);
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, TaskState state) {
        int u = switch (state) {
            case OPEN, FROZEN, UNAVAILABLE -> 30;
            case CLAIMABLE -> 38;
            case CLAIMED -> 46;
        };
        g.blit(WIDGETS, x, y, u, 0, BOX, BOX, WIDGETS_W, WIDGETS_H);
    }

    @Override
    public void drawStamp(GuiGraphics g, int x, int y, boolean lit) {
        g.blit(WIDGETS, x, y, lit ? 15 : 0, 10, STAMP, STAMP, WIDGETS_W, WIDGETS_H);
    }

    @Override
    public void drawRule(GuiGraphics g, int x, int y, int w) {
        int fade = Math.max(1, w / 6);
        KitDraw.batch(g, () -> {
            g.fill(x + fade, y, x + w - fade, y + 1, 0xFF8a6a3a);
            for (int i = 0; i < fade; i++) {
                int a = (0xFF * i / fade) << 24;
                g.fill(x + i, y, x + i + 1, y + 1, a | 0x8a6a3a);
                g.fill(x + w - 1 - i, y, x + w - i, y + 1, a | 0x8a6a3a);
            }
        });
    }

    @Override
    public void drawBar(GuiGraphics g, Rect r, float fraction) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF7a5a32);
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, 0xFFd9c9a2);
        int fill = Math.round((r.w() - 2) * Math.max(0f, Math.min(1f, fraction)));
        g.fill(r.x() + 1, r.y() + 1, r.x() + 1 + fill, r.bottom() - 1, 0xFF9a1820);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        g.renderOutline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, focusColor());
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
