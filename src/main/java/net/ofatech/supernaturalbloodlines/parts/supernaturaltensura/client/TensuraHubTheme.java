package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubDev;
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
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageBloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookSkillsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageGuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageLoadoutPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StagePathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageQuestsPage;
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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitFaces;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitWidgets;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
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
    private final HubPageView bloodline = new StageBloodlinePage(StageBloodlinePage.Look.TENSURA);
    private final HubPageView pack = new BookPackPage(BookPackPage.Style.TENSURA);
    private final HubPageView guide = new StageGuidePage();
    private final HubPageView loadout = new StageLoadoutPage();
    @Nullable
    private Component title;
    @Nullable
    private Counters counters;
    private boolean detailDrawn;
    private boolean lockedNow;

    @Override
    public String kit() {
        return "tensura";
    }

    @Override
    public String titleFont() {
        return "title";
    }

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return TensuraHubLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubLayout layout(int width, int height, List<HubPage> pages, int seals) {
        return TensuraHubLayout.of(width, height, pages, seals);
    }

    @Override
    public void drawGroupHeading(GuiGraphics g, Rect r, HubPage.Group group) {
        String text = Component.translatable(group.langKey()).getString().toUpperCase(java.util.Locale.ROOT);
        g.drawString(Minecraft.getInstance().font, text, r.x() + 2, r.y() + 1, group == HubPage.Group.OPERATOR ? GOLD : MUTED, false);
    }

    @Override
    public HubPageView view(HubPage page) {
        return switch (page) {
            case PATH -> path;
            case SKILLS -> skills;
            case QUESTS -> quests;
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
        detailDrawn = false;
        lockedNow = page.locked(f.view.context());
        boolean panels = page == HubPage.GUIDE || page.locked(f.view.context());
        if (panels) {
            for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                UiKit.draw(f.g, this, Part.PANEL, TensuraHubLayout.panel(f.region(region)));
            }
        }
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        KitDraw.activeTheme(this);
        Rect f = l.frame();
        g.fill(f.x() - 2, f.y() - 2, f.right() + 2, f.bottom() + 2, 0xFF02080f);
        UiKit.draw(g, this, Part.COVER, f);
        // the title bar picture: its left 16 px are fixed, the rest repeats
        UiKit.draw(g, this, Part.COVER_ART, f.x() + 4, f.y() + 4, f.w() - 8, 13);
        Rect pane = l.chrome().detail();
        if (pane != null) {
            UiKit.draw(g, this, Part.PANEL, pane);
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
        Rect theme = layout.chrome().themeButton();
        int right = (theme == null ? f.right() - 30 : theme.x()) - 8;
        String name = title != null ? title.getString() : Component.translatable(page.langKey()).getString();
        String shortName = Component.translatable(page.langKey()).getString();
        LabelFit.Header header = LabelFit.header(List.of("\u300a Status \u00b7 " + name + " \u300b", "\u300a Status \u00b7 " + shortName + " \u300b", "\u300a " + shortName + " \u300b"),
                stats(), right - (f.x() + 10), 14, s -> (int) (font.width(s) * 1.25f), s -> font.width(s.replace("\u0001", "")) + (s.contains("\u0001") ? 10 : 0));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(f.x() + 10, f.y() + 6, 0);
        pose.scale(1.25f, 1.25f, 1f);
        g.drawString(font, header.title(), 0, 0, TEAL, true);
        pose.popPose();
        if (!header.stats().isEmpty()) {
            String[] parts = header.stats().split("\u0001", -1);
            int x = right - (font.width(header.stats().replace("\u0001", "")) + (parts.length > 1 ? 10 : 0));
            g.drawString(font, parts[0], x, f.y() + 7, TEXT, false);
            if (parts.length > 1) {
                int dx = x + font.width(parts[0]);
                g.fill(dx, f.y() + 8, dx + 6, f.y() + 14, GOLD);
                g.drawString(font, parts[1], dx + 10, f.y() + 7, TEXT, false);
            }
        }
        Rect pane = layout.chrome().detail();
        if (pane != null && !detailDrawn) {
            caption(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.info"), pane.x() + 8, pane.y() + 7);
            text(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.empty." + (lockedNow ? "locked" : page.name().toLowerCase(java.util.Locale.ROOT))), pane.x() + 8, pane.y() + 20, pane.w() - 16,
                    MUTED, pane.bottom() - 8);
        }
        boolean wide = !layout.tabs().isEmpty() && layout.tabs().get(0).w() > 40;
        if (wide && layout.headings().isEmpty()) {
            Rect first = layout.tabs().get(0);
            caption(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.pages"), first.x(), first.y() - 9);
        }
        if (wide && !layout.seals().isEmpty()) {
            Rect first = layout.seals().get(0);
            caption(g, font, Component.translatable("supernaturalbloodlines.hub.tensura.mods"), first.x(), first.y() - 9);
        }
    }

    /** The stats line from the fullest to the barest: race, SP, EP and the days to the full moon (after the \u0001 mark, drawn with a gold square), dropped in turn. */
    private List<String> stats() {
        if (counters == null) {
            return List.of("");
        }
        String race = HubHooks.tensura().map(info -> info.race().getString()).orElse(null);
        String ep = counters.epText();
        var level = Minecraft.getInstance().level;
        int days = level == null ? -1 : HubAlmanac.daysToFull(HubDev.moonPhase(level));
        String moon = days < 0 ? null : "\u0001" + (days == 0 ? "tonight" : days + (days == 1 ? " day" : " days"));
        String base = counters.spText() + (ep == null ? "" : " \u00b7 " + ep);
        String front = race == null ? "" : race + " \u00b7 ";
        List<String> out = new ArrayList<>();
        out.add(front + base + (moon == null ? "" : " \u00b7 " + moon));
        if (moon != null) {
            out.add(front + base);
        }
        if (race != null) {
            out.add(base);
        }
        out.add(counters.spText());
        return out;
    }

    private static void caption(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text.getString().toUpperCase(java.util.Locale.ROOT), x, y, MUTED, false);
    }

    private static void row(GuiGraphics g, HubTheme theme, Rect r, boolean selected, boolean lit, String label, int labelColor, Runnable icon) {
        Font font = Minecraft.getInstance().font;
        Part face = selected ? Part.TAB_SELECTED : lit ? Part.TAB_HOVER : Part.TAB;
        UiKit.draw(g, theme, face, r);
        labelColor = UiKit.labelOver(theme, face, labelColor);
        icon.run();
        if (r.w() > 40) { // an icon-only row on a narrow window (the tooltip has the name)
            String text = TensuraHubLayout.fit(label, r.w() - ICON_ROOM - 8, font::width);
            g.drawString(font, text, r.x() + ICON_ROOM, r.y() + (r.h() - 8) / 2, labelColor, false);
        }
    }

    /** The room an icon takes at the left of a row: a 16 px picture (3 px in), then a gap. */
    private static final int ICON_ROOM = 22;

    /** The icon of a row, as big as the row lets it be (the 16 px picture when the row is 14 px tall or more, else the halved one). */
    private static void rowIcon(GuiGraphics g, Rect r, Icons icon, int alpha) {
        int box = Math.min(16, r.h() - 2);
        KitDraw.icon(g, icon, r.x() + 3 + (16 - box) / 2, r.y() + (r.h() - box) / 2, box, alpha);
    }

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        row(g, this, r, selected, false, Component.translatable(page.langKey()).getString(), selected ? TEAL : TEXT, () ->
                rowIcon(g, r, Icons.of(page), selected ? 0xFFFFFFFF : 0xD0FFFFFF));
        if (page.group() == HubPage.Group.OPERATOR) {
            KitDraw.dashed(g, r, GOLD);
        }
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        row(g, this, r, false, lit, letter, TEXT, () -> { });
    }

    @Override
    public void drawLauncher(GuiGraphics g, Rect r, HubHooks.Launcher launcher, boolean lit) {
        // the mod's short name (the full one does not fit the list; the tooltip has it)
        Component label = Component.translatableWithFallback("supernaturalbloodlines.hub.launcher.short." + launcher.id(), launcher.id());
        row(g, this, r, false, lit, label.getString(), GOLD, () -> {
            if (r.w() > 40) {
                rowIcon(g, r, Icons.LAUNCH, 0xFFFFFFFF);
            } else {
                g.drawString(Minecraft.getInstance().font, launcher.letter(), r.x() + (r.w() - 6) / 2, r.y() + (r.h() - 8) / 2, GOLD, false);
            }
        });
    }

    @Override
    public void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        Font font = Minecraft.getInstance().font;
        UiKit.draw(g, this, hot ? Part.BUTTON_HOVER : Part.BUTTON_NORMAL, r);
        String label = Component.translatable("supernaturalbloodlines.hub.tensura.theme").getString();
        g.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + (r.h() - 8) / 2, TEAL, false);
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.button(g, this, r, Icons.CLOSE, hot, focused);
    }
    /** The stats are drawn with the title, in {@link #drawFooter}, so the two are fitted together; this only keeps the numbers. */
    @Override
    public void drawCounters(UiContext ctx, Rect r, Counters c) {
        counters = c;
    }

    /** The magic circle: a ring of ticks that fill to your progress, two circles, two nested hexagons and the live model. */
    @Override
    public void drawEmblem(UiContext ctx, Rect area, EmblemData data) {
        GuiGraphics g = ctx.g();
        int cx = area.x() + area.w() / 2;
        int cy = area.y() + area.h() / 2;
        int r = Math.max(14, Math.min(area.w(), area.h()) / 2 - 2);
        // the rings and hexagons are plain fills: one batch
        KitDraw.batch(g, () -> {
            KitDraw.tickRing(g, cx, cy, r, 56, data.progress(), TEAL, 0xFF1d5a68);
            ring(g, cx, cy, r * 82 / 100, TEAL);
            ring(g, cx, cy, r * 74 / 100, TEAL_DIM);
            hexagon(g, cx, cy, r * 66 / 100, TEAL_DIM);
            hexagon(g, cx, cy, r * 50 / 100, 0xFF7a6a2c);
        });
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
        detailDrawn = true;
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
            Part part = KitFaces.button(a.primary(), a.enabled(), hot, false);
            UiKit.draw(g, this, part, b);
            String label = LabelFit.cut(a.label().getString(), b.w() - 8, font::width);
            g.drawString(font, label, b.x() + (b.w() - font.width(label)) / 2, b.y() + (b.h() - 8) / 2, UiKit.label(this, part), false);
        }
        int limit = by - 2;
        int y = r.y() + 7;
        caption(g, font, d.kicker() != null ? d.kicker() : Component.translatable("supernaturalbloodlines.hub.tensura.info"), x, y);
        y += 11;
        var pose = g.pose();
        // as many title lines as fit above the buttons; a title that does not all fit ends its last line with an ellipsis
        for (String line : LabelFit.wrap(d.title().getString(), (int) (w / 1.4f), Math.max(0, (limit - y) / 13), font::width)) {
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
        // the fields are what the reader looks up: they keep their room and the text above them gives way (it ends with an ellipsis)
        int fieldsH = 0;
        for (Detail.Field fl : d.fields()) {
            fieldsH += fieldHeight(font, fl, w);
        }
        int bodyLimit = d.fields().isEmpty() ? limit : Math.max(y, limit - fieldsH);
        for (Component line : d.body()) {
            y = text(g, font, line, x, y, w, TEXT, bodyLimit) + 3;
        }
        for (Detail.Field fl : d.fields()) {
            int h = fieldHeight(font, fl, w);
            if (y + h - 3 > limit) {
                break;
            }
            String label = fl.label().getString().toUpperCase(java.util.Locale.ROOT);
            if (inline(font, fl, w)) {
                g.drawString(font, label, x, y, MUTED, false);
                g.drawString(font, fl.value().getString(), x + w - font.width(fl.value()), y, TEXT, false);
                y += h;
            } else {
                caption(g, font, fl.label(), x, y);
                y = text(g, font, fl.value(), x, y + 9, w, TEXT, limit) + 3;
            }
        }
        if (d.reward() != null && y + 12 <= limit) {
            y = text(g, font, d.reward(), x, y, w, GOLD, limit) + 3;
        }
        for (Detail.Row row : d.rows()) {
            int tagW = row.source() == null ? 0 : KitDraw.tagWidth(font, row.source()) + 4;
            // two lines at most; a task that does not fit ends its second line with an ellipsis
            var lines = LabelFit.wrap(row.label().getString(), w - tagW - 12, 2, font::width);
            int n = lines.size();
            int h = 16 + 10 * n;
            if (y + h + 2 > limit) {
                break;
            }
            Rect card = new Rect(x, y, w, h);
            UiKit.draw(g, this, Part.INSET, card);
            if (row.complete()) {
                UiKit.draw(g, this, Part.TASK_CLAIMED, card.right() - 14, card.y() + 4, 10, 10);
            }
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

    /** A field whose label and value fit side by side takes one line. */
    private static boolean inline(Font font, Detail.Field fl, int w) {
        return font.width(fl.label().getString().toUpperCase(java.util.Locale.ROOT)) + font.width(fl.value()) + 8 <= w;
    }

    private static int fieldHeight(Font font, Detail.Field fl, int w) {
        return inline(font, fl, w) ? 13 : 9 + 10 * Math.max(1, font.split(fl.value(), w).size()) + 3;
    }

    /** The text wrapped to {@code w}, as many lines as fit above {@code limit}; when it does not all fit the last line ends with an ellipsis instead of stopping silently. */
    private static int text(GuiGraphics g, Font font, Component c, int x, int y, int w, int color, int limit) {
        int fit = Math.max(0, (limit - y + 1) / 10);
        for (String line : LabelFit.wrap(c.getString(), w, fit, font::width)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, RungStatus status) {
        KitWidgets.rungMark(g, this, x, y, status);
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, TaskState state) {
        KitWidgets.taskBox(g, this, x, y, 10, state);
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
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        KitWidgets.scrollbar(g, this, region, offset, max);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        UiKit.focus(g, this, r);
    }
    @Override
    public int paper() {
        return PANEL;
    }

    @Override
    public List<TextPair> textPairs() {
        List<TextPair> pairs = new ArrayList<>(List.of(new TextPair("page labels", TEXT, PANEL), new TextPair("the chosen page", TEAL, PANEL_LIT), new TextPair("mod names", GOLD, PANEL),
                new TextPair("muted text", MUTED, PANEL),
                // the title bar, the stage and the info pane
                new TextPair("title, teal, on the bar", TEAL, BG), new TextPair("stats, text, on the bar", TEXT, BG), new TextPair("muted on the stage", MUTED, BG),
                new TextPair("gold reward in the pane", GOLD, PANEL), new TextPair("good, green, in the pane", GREEN, PANEL), new TextPair("text on a lit row", TEXT, PANEL_LIT)));
        // the label of every kit button on its face (the pane's buttons)
        for (Part button : KitFaces.BUTTONS) {
            pairs.add(new TextPair("label on " + button.sprite(), KitFaces.label(kit(), button, TEXT), KitFaces.face(kit(), button)));
        }
        return pairs;
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
