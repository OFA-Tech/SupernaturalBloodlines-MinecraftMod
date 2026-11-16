package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubAlmanac;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPages;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubThemeViews;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookBloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookSkillsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageGuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageLoadoutPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StagePathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageQuestsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageRitesPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Counters;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;
import org.jetbrains.annotations.Nullable;

/**
 * The Tensura expansion, a status window: a deep blue frame with a title bar ("《 Status · Path 》", the stats line, the Theme button), a list of pages and
 * of other mods on the left, a teal magic circle with hexagon rings and a hexagon ladder in the middle, and an info pane on the right that holds the
 * selection, its task rows and its buttons stacked at the bottom. Drawn with fills: our own art in Tensura's colours, nothing of Tensura's shipped or
 * drawn. Registered by {@code supernatural_tensura} only, so without Tensura {@code hub_theme = TENSURA} shows the Grimoire.
 */
public final class TensuraHubTheme implements HubTheme, HubThemeViews {
    private static final int BG = 0xFF0d2035;
    private static final int PANEL = 0xFF12293f;
    private static final int PANEL_LIT = 0xFF17374e;
    private static final int EDGE = 0xFF2a5370;
    private static final int TEAL = 0xFF3fd0d6;
    private static final int TEAL_DIM = 0xFF1f6a74;
    private static final int TEXT = 0xFFe6f3fb;
    private static final int GOLD = 0xFFe8b33a;
    private static final int GREEN = 0xFF5ee08a;
    private static final int MUTED = 0xFF8fb2c4;

    private final HubPageView path = new StagePathPage();
    private final HubPageView skills = new BookSkillsPage(true, Shapes.Shape.HEX);
    private final HubPageView quests = new StageQuestsPage();
    private final HubPageView rites = new StageRitesPage();
    private final HubPageView bloodline = new BookBloodlinePage(true);
    private final HubPageView pack = new BookPackPage(BookPackPage.Style.TENSURA);
    private final HubPageView guide = new StageGuidePage();
    private final HubPageView loadout = new StageLoadoutPage();
    @Nullable
    private Component title;

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return TensuraHubLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubPageView view(HubPage page) {
        return switch (page) {
            case PATH -> path;
            case SKILLS -> skills;
            case QUESTS -> quests;
            case RITES -> rites;
            case BLOODLINE -> bloodline;
            case PACK -> pack;
            case GUIDE -> guide;
            case LOADOUT -> loadout;
            default -> HubPages.shared(page);
        };
    }

    @Override
    public boolean takesHead(Component title, @Nullable Component subtitle) {
        this.title = title;
        return true;
    }

    @Override
    public void drawOverlay(HubScreen.Frame f, HubPage page) {
        title = null;
        boolean panels = page == HubPage.BLOODLINE || page == HubPage.GUIDE || page.locked(f.view.context());
        if (panels) {
            for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                Rect r = TensuraHubLayout.panel(f.region(region));
                box(f.g, r, PANEL, EDGE);
            }
        }
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        Rect f = l.frame();
        g.fill(f.x() - 2, f.y() - 2, f.right() + 2, f.bottom() + 2, 0xFF02080f);
        g.fill(f.x(), f.y(), f.right(), f.bottom(), BG);
        g.renderOutline(f.x(), f.y(), f.w(), f.h(), EDGE);
        g.fill(f.x(), f.y() + TensuraHubLayout.TITLE, f.right(), f.y() + TensuraHubLayout.TITLE + 1, EDGE);
        g.fill(f.x() + 1, f.y() + 1, f.right() - 1, f.y() + TensuraHubLayout.TITLE, 0xFF11304a);
        Rect pane = l.chrome().detail();
        if (pane != null) {
            box(g, pane, PANEL, EDGE);
        }
    }

    private static void box(GuiGraphics g, Rect r, int face, int edge) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), face);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), edge);
    }

    /** The window's title, and the two captions of the list. */
    @Override
    public void drawFooter(UiContext ctx, HubLayout layout, HubPage page) {
        Font font = ctx.font();
        GuiGraphics g = ctx.g();
        Rect f = layout.frame();
        Component name = title != null ? title : Component.translatable(page.langKey());
        String text = "\u300a Status \u00b7 " + font.plainSubstrByWidth(name.getString(), Math.max(40, f.w() / 4)) + " \u300b";
        var pose = g.pose();
        pose.pushPose();
        pose.translate(f.x() + 10, f.y() + 6, 0);
        pose.scale(1.25f, 1.25f, 1f);
        g.drawString(font, text, 0, 0, TEAL, true);
        pose.popPose();
        if (!layout.tabs().isEmpty()) {
            Rect first = layout.tabs().get(0);
            caption(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.pages"), first.x(), first.y() - 9);
        }
        if (!layout.seals().isEmpty()) {
            Rect first = layout.seals().get(0);
            caption(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.mods"), first.x(), first.y() - 9);
        }
    }

    private static void caption(GuiGraphics g, Font font, Component text, int x, int y) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.75f, 0.75f, 1f);
        g.drawString(font, text.getString().toUpperCase(java.util.Locale.ROOT), 0, 0, MUTED, false);
        pose.popPose();
    }

    private static void row(GuiGraphics g, Rect r, boolean selected, boolean lit, String label, int labelColor, Runnable icon) {
        Font font = Minecraft.getInstance().font;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), selected ? PANEL_LIT : lit ? PANEL_LIT : PANEL);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), selected ? TEAL : EDGE);
        icon.run();
        String text = TensuraHubLayout.fit(label, r.w() - 22, font::width);
        g.drawString(font, text, r.x() + 18, r.y() + (r.h() - 8) / 2, labelColor, false);
    }

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        row(g, r, selected, false, Component.translatable(page.langKey()).getString(), selected ? TEAL : TEXT, () ->
                KitDraw.icon(g, Icons.of(page), r.x() + 4, r.y() + (r.h() - 10) / 2, 10, selected ? TEAL : TEXT));
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        row(g, r, false, lit, letter, TEXT, () -> { });
    }

    @Override
    public void drawLauncher(GuiGraphics g, Rect r, HubHooks.Launcher launcher, boolean lit) {
        // the mod's short name (the full one does not fit the list; the tooltip has it)
        Component label = Component.translatableWithFallback("supernaturalbloodlines.hub.launcher.short." + launcher.id(), launcher.id());
        row(g, r, false, lit, label.getString(), GOLD, () -> KitDraw.icon(g, Icons.LAUNCH, r.x() + 4, r.y() + (r.h() - 10) / 2, 10, TEXT));
    }

    @Override
    public void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        Font font = Minecraft.getInstance().font;
        box(g, r, hot ? PANEL_LIT : PANEL, TEAL);
        String label = Component.translatable("supernaturalbloodlines.hub.tensura.theme").getString();
        g.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + (r.h() - 8) / 2, TEAL, false);
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.icon(g, Icons.CLOSE, r.x() + (r.w() - 12) / 2 + 1, r.y() + (r.h() - 12) / 2, 12, hot ? TEAL : TEXT);
        if (focused) {
            drawFocus(g, r);
        }
    }

    /** The stats line in the middle of the title bar: race, SP, EP and the days to the full moon. */
    @Override
    public void drawCounters(UiContext ctx, Rect r, Counters c) {
        Font font = ctx.font();
        StringBuilder sb = new StringBuilder();
        HubHooks.tensura().ifPresent(info -> sb.append(info.race().getString()).append(" \u00b7 "));
        sb.append(c.spText());
        if (c.epText() != null) {
            sb.append(" \u00b7 ").append(c.epText());
        }
        var level = Minecraft.getInstance().level;
        int days = level == null ? -1 : HubAlmanac.daysToFull(level.getMoonPhase());
        String line = font.plainSubstrByWidth(sb.toString(), r.w() - (days >= 0 ? 52 : 0));
        int w = font.width(line) + (days >= 0 ? 52 : 0);
        int x = r.x() + (r.w() - w) / 2;
        ctx.g().drawString(font, line, x, r.y(), TEXT, false);
        if (days >= 0) {
            int dx = x + font.width(line) + 8;
            ctx.g().fill(dx, r.y() + 1, dx + 6, r.y() + 7, GOLD);
            ctx.g().drawString(font, days == 0 ? "tonight" : days + (days == 1 ? " day" : " days"), dx + 10, r.y(), TEXT, false);
        }
    }

    /** The magic circle: a ring of ticks that fill to your progress, two circles, two nested hexagons and the live model. */
    @Override
    public void drawEmblem(UiContext ctx, Rect area, EmblemData data) {
        GuiGraphics g = ctx.g();
        int cx = area.x() + area.w() / 2;
        int cy = area.y() + area.h() / 2;
        int r = Math.max(14, Math.min(area.w(), area.h()) / 2 - 2);
        KitDraw.tickRing(g, cx, cy, r, 56, data.progress(), TEAL, 0xFF1d5a68);
        ring(g, cx, cy, r * 82 / 100, TEAL);
        ring(g, cx, cy, r * 74 / 100, TEAL_DIM);
        hexagon(g, cx, cy, r * 66 / 100, TEAL_DIM);
        hexagon(g, cx, cy, r * 50 / 100, 0xFF7a6a2c);
        int m = (int) (r * 0.95);
        KitDraw.model(g, new Rect(cx - m / 2, cy - m / 2 - 2, m, m), data.entity(), data.drag());
    }

    private static void ring(GuiGraphics g, int cx, int cy, int radius, int color) {
        int steps = Math.max(24, radius * 4);
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            g.fill(cx + (int) Math.round(radius * Math.cos(a)), cy + (int) Math.round(radius * Math.sin(a)),
                    cx + (int) Math.round(radius * Math.cos(a)) + 1, cy + (int) Math.round(radius * Math.sin(a)) + 1, color);
        }
    }

    private static void hexagon(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int i = 0; i < 6; i++) {
            double a0 = Math.PI / 6 + Math.PI / 3 * i;
            double a1 = a0 + Math.PI / 3;
            KitDraw.line(g, cx + (int) Math.round(radius * Math.cos(a0)), cy + (int) Math.round(radius * Math.sin(a0)),
                    cx + (int) Math.round(radius * Math.cos(a1)), cy + (int) Math.round(radius * Math.sin(a1)), color);
        }
    }

    /** The info pane: the selection's caption, its name in teal, its pips, its text, fields and reward, its task rows, and its buttons stacked at the bottom. */
    @Override
    public void drawDetail(UiContext ctx, Rect r, Detail d) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        int x = r.x() + 8;
        int w = r.w() - 16;
        List<Rect> buttons = new ArrayList<>();
        int by = r.bottom() - 8;
        for (int i = d.actions().size() - 1; i >= 0; i--) {
            Detail.Action a = d.actions().get(i);
            Rect b = new Rect(x, by - 16, w, 16);
            by -= 20;
            buttons.add(0, b);
            boolean hot = a.enabled() && (ctx.hovered(b) || ctx.focused("detail:" + a.id()));
            int fill = a.primary() ? (a.enabled() ? (hot ? 0xFF6be4ea : TEAL) : 0xFF244a55) : (hot ? PANEL_LIT : BG);
            g.fill(b.x(), b.y(), b.right(), b.bottom(), a.primary() ? fill : EDGE);
            g.fill(b.x() + 1, b.y() + 1, b.right() - 1, b.bottom() - 1, fill);
            String label = font.plainSubstrByWidth(a.label().getString(), b.w() - 8);
            g.drawString(font, label, b.x() + (b.w() - font.width(label)) / 2, b.y() + 4, a.primary() ? (a.enabled() ? 0xFF05202a : 0xFF6a8d97) : a.enabled() ? TEXT : 0xFF6a8d97, false);
        }
        int limit = by - 2;
        int y = r.y() + 7;
        caption(g, font, d.kicker() != null ? d.kicker() : Component.translatable("supernaturalbloodlines.hub.tensura.info"), x, y);
        y += 11;
        var pose = g.pose();
        for (var line : font.split(d.title(), (int) (w / 1.4f))) {
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(1.4f, 1.4f, 1f);
            g.drawString(font, line, 0, 0, TEAL, false);
            pose.popPose();
            y += 13;
        }
        if (d.subtitle() != null) {
            y = text(g, font, d.subtitle(), x, y, w, MUTED, limit);
        }
        if (d.pips() > 0) {
            y += 2;
            KitDraw.pips(g, x, y, d.pips(), d.pipsFilled(), TEAL, 0xFF24506a);
            y += 8;
        }
        y += 3;
        for (Component line : d.body()) {
            y = text(g, font, line, x, y, w, TEXT, limit) + 3;
        }
        for (Detail.Field fl : d.fields()) {
            if (y + 18 > limit) {
                break;
            }
            caption(g, font, fl.label(), x, y);
            y = text(g, font, fl.value(), x, y + 9, w, TEXT, limit) + 3;
        }
        if (d.reward() != null && y + 12 <= limit) {
            y = text(g, font, d.reward(), x, y, w, GOLD, limit) + 3;
        }
        for (Detail.Row row : d.rows()) {
            int tagW = row.source() == null ? 0 : KitDraw.tagWidth(font, row.source()) + 4;
            var lines = font.split(row.label(), w - tagW - 12);
            int n = Math.min(2, Math.max(1, lines.size()));
            int h = 16 + 10 * n;
            if (y + h + 2 > limit) {
                break;
            }
            Rect card = new Rect(x, y, w, h);
            box(g, card, PANEL_LIT, row.complete() ? GREEN : EDGE);
            if (row.source() != null) {
                KitDraw.sourceTag(g, font, this, row.source(), card.x() + 4, card.y() + 4);
            }
            for (int i = 0; i < n && i < lines.size(); i++) {
                g.drawString(font, lines.get(i), card.x() + 6 + tagW, card.y() + 5 + 10 * i, row.complete() ? MUTED : TEXT, false);
            }
            String count = row.total() > 1 ? row.done() + "/" + row.total() : row.complete() ? "\u2713" : "";
            int cw = font.width(count);
            int by2 = card.y() + 7 + 10 * n;
            drawBar(g, new Rect(card.x() + 6, by2 + 2, card.w() - 16 - cw, 4), row.total() <= 0 ? 0f : Math.min(1f, row.done() / (float) row.total()));
            g.drawString(font, count, card.right() - cw - 4, by2, row.complete() ? GREEN : MUTED, false);
            y += h + 4;
        }
        KitDraw.detailActions(ctx, this, buttons, d);
    }

    private static int text(GuiGraphics g, Font font, Component c, int x, int y, int w, int color, int limit) {
        for (var line : font.split(c, w)) {
            if (y + 9 > limit) {
                break;
            }
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, RungStatus status) {
        int edge = switch (status) {
            case REACHED -> GREEN;
            case CURRENT -> TEAL;
            case LOCKED -> 0xFF3f6a80;
        };
        KitDraw.shape(g, Shapes.Shape.HEX, x - 1, y - 1, 10, edge);
        KitDraw.shape(g, Shapes.Shape.HEX, x + 1, y + 1, 6, BG);
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, TaskState state) {
        g.renderOutline(x, y, 10, 10, state == TaskState.CLAIMABLE ? good() : muted());
        if (state == TaskState.CLAIMABLE) {
            g.fill(x + 1, y + 1, x + 9, y + 9, 0xFF2f8f5a);
        } else if (state == TaskState.CLAIMED) {
            g.fill(x + 2, y + 2, x + 8, y + 8, muted());
        }
    }

    @Override
    public void drawStamp(GuiGraphics g, int x, int y, boolean lit) {
        Rect r = new Rect(x, y, 14, 14);
        box(g, r, lit ? TEAL : TEAL_DIM, EDGE);
        g.drawString(Minecraft.getInstance().font, "\u2713", x + 4, y + 3, 0xFF05202a, false);
    }

    @Override
    public void drawRule(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, EDGE);
    }

    @Override
    public void drawBar(GuiGraphics g, Rect r, float fraction) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF1b3a50);
        int w = Math.round(r.w() * Math.max(0f, Math.min(1f, fraction)));
        if (w > 0) {
            g.fill(r.x(), r.y(), r.x() + w, r.bottom(), fraction >= 1f ? GREEN : TEAL);
        }
    }

    @Override
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        int x = region.right() + 2;
        g.fill(x, region.y(), x + 2, region.bottom(), BG);
        int thumb = Math.max(8, region.h() * region.h() / (region.h() + max));
        int y = region.y() + (region.h() - thumb) * offset / Math.max(1, max);
        g.fill(x, y, x + 2, y + thumb, TEAL);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        g.renderOutline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, GOLD);
    }

    @Override
    public int paper() {
        return PANEL;
    }

    @Override
    public List<TextPair> textPairs() {
        return List.of(new TextPair("page labels", TEXT, PANEL), new TextPair("the chosen page", TEAL, PANEL_LIT), new TextPair("mod names", GOLD, PANEL),
                new TextPair("the primary button", 0xFF05202a, TEAL), new TextPair("muted text", MUTED, PANEL));
    }

    @Override
    public int ink() {
        return TEXT;
    }

    @Override
    public int muted() {
        return MUTED;
    }

    @Override
    public int accent() {
        return TEAL;
    }

    @Override
    public int good() {
        return GREEN;
    }

    @Override
    public int sourceColor(String source) {
        return switch (source) {
            case "vampirism" -> 0xFFc9a0ff;
            case "werewolves" -> 0xFFffbe5c;
            case "enchanted" -> 0xFF7ee08f;
            case "tensura" -> 0xFF5fd3e6;
            default -> 0xFFff8a7a;
        };
    }
}
