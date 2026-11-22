package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Counters;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.BarPlan;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Bake;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.BakedTexture;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageBloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageGuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageLoadoutPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.StageQuestsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookSkillsPage;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitFaces;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitWidgets;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubThemeViews;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BloodMoonPathPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.PathPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * Blood Moon: a dark full screen. The moon at the top fills with progress to the next rung of the creature ladder (a magic race's
 * ladder is the ring around it), the rungs are phases around it, pages are a rail on the left and launchers a rail on the right, and the
 * page's content sits in two cards under the moon. Every page but Path uses the shared views; Path has its own
 * ({@link BloodMoonPathPage}) because the moon already shows the ladder. All drawn with fills: no textures but our page icons.
 */
public final class BloodMoonTheme implements HubTheme, HubThemeViews {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hub/grimoire/widgets.png");
    private static final int CARD = 0xFF1a0a10;
    private static final int EDGE = 0xFF4a1c26;
    private static final int RED = 0xFFe2493f;
    private static final int GOLD = 0xFFffd36b;
    private static final int PILL = 0xFF2a0b12;
    private static final int COUNT = 0xFFfff1ea;
    /** The screen's gradient, top to bottom: the moon band's text is on it. */
    private static final int SKY_TOP = 0xFF2a0b14;
    private static final int SKY_BOTTOM = 0xFF07040a;
    private final HubPageView path = new BloodMoonPathPage();
    private final BookSkillsPage skills = new BookSkillsPage(true);
    private final HubPageView pack = new BookPackPage(BloodMoonLayout.RIGHT, BloodMoonLayout.MOON);
    private final HubPageView quests = new StageQuestsPage();
    private final HubPageView bloodline = new StageBloodlinePage(StageBloodlinePage.Look.MOON);
    private final HubPageView guide = new StageGuidePage();
    private final HubPageView loadout = new StageLoadoutPage();

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return BloodMoonLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubLayout layout(int width, int height, java.util.List<HubPage> pages, int seals) {
        return BloodMoonLayout.of(width, height, pages, seals);
    }

    @Override
    public void drawGroupHeading(GuiGraphics g, Rect r, HubPage.Group group) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), group == HubPage.Group.OPERATOR ? GOLD : EDGE);
    }

    @Override
    public HubPageView view(HubPage page) {
        return switch (page) {
            case PATH -> path;
            case SKILLS -> skills;
            case PACK -> pack;
            case QUESTS -> quests;
            case BLOODLINE -> bloodline;
            case GUIDE -> guide;
            case LOADOUT -> loadout;
            default -> HubThemeViews.super.view(page);
        };
    }

    /** The sky: a dark red wash, and a soft red glow behind the stage where the moon will be. */
    @Override
    public String kit() {
        return "bloodmoon";
    }

    @Override
    public String titleFont() {
        return "title_gothic";
    }

    /** The cover picture is the sky for its first 272 rows and its last band is the void: the rest of a taller screen is that band. */
    private static final int COVER_H = 272;

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        KitDraw.activeTheme(this);
        Rect s = l.frame();
        if (UiKit.has(kit(), Part.COVER)) {
            int h = Math.min(s.h(), COVER_H);
            UiKit.draw(g, this, Part.COVER, s.x(), s.y(), s.w(), h);
            if (s.h() > h) {
                g.fill(s.x(), s.y() + h, s.right(), s.bottom(), SKY_BOTTOM);
            }
        } else {
            g.fillGradient(s.x(), s.y(), s.right(), s.bottom(), SKY_TOP, SKY_BOTTOM);
        }
        Rect stage = l.region(BloodMoonLayout.MOON);
        int cx = stage.x() + stage.w() * 2 / 5;
        int cy = stage.y() + stage.h() / 2;
        int radius = BloodMoonLayout.glowRadius(stage);
        // the glow is baked once at a capped size and stretched: it is a soft blur, so a 128 pixel diameter (radius {@link #GLOW_PX}, 64) is plenty, and the texture does not change with the window
        if (radius > 0) {
            BakedTexture.blitScaled(g, BakedTexture.get(BakedTexture.Key.of("glow", GLOW_PX), 2 * GLOW_PX, 2 * GLOW_PX, Bake.glow(GLOW_PX)), cx - radius, cy - radius,
                    2 * radius, 2 * radius, 2 * GLOW_PX, 2 * GLOW_PX);
        }
    }

    /** Before a page: the cards behind the list pages (the moon pages draw their own), and nothing else. */
    @Override
    public void drawOverlay(HubScreen.Frame f, HubPage page) {
        boolean cards = switch (page) {
            case GUIDE -> true;
            case SKILLS -> skills.legacyActive();
            default -> false;
        };
        if (page.locked(f.view.context())) {
            cards = true;
        }
        if (cards) {
            for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                UiKit.draw(f.g, this, Part.PANEL, BloodMoonLayout.card(f.region(region)));
            }
        }
        if ((page == HubPage.PATH && !f.view.ghoul()) || (page == HubPage.PACK && f.screen().profile() == null)) {
            UiKit.draw(f.g, this, Part.PANEL, BloodMoonLayout.card(f.region(BloodMoonLayout.RIGHT)));
        }
    }

    /** The moon: a glow, a disc lit from the bottom up to the progress, a few craters, a ring of ticks, and the live model in front. */
    @Override
    public void drawEmblem(UiContext ctx, Rect area, EmblemData data) {
        GuiGraphics g = ctx.g();
        int cx = area.x() + area.w() / 2;
        int cy = area.y() + area.h() / 2;
        int r = BloodMoonLayout.moonRadius(area);
        // the halo, the disc lit up to the progress and the craters are one baked picture (a new one only when the radius or the progress step changes)
        int step = Bake.step(data.progress());
        int size = Bake.moonSize(r);
        BakedTexture.blit(g, BakedTexture.get(BakedTexture.Key.of("moon", r, step), size, size, Bake.moon(r, step)), cx - size / 2, cy - size / 2, size, size);
        KitDraw.tickRing(g, cx, cy, r + 10, 48, data.progress(), RED, 0xFF4a2430);
        int m = (int) (r * 0.95);
        KitDraw.model(g, new Rect(cx - m / 2, cy - m / 2 - 2, m, m), data.entity(), data.drag());
    }

    @Override
    public void drawCounters(UiContext ctx, Rect r, Counters c) {
        String ep = c.epText();
        String text = c.sp() + " SP" + (ep == null ? "" : "  \u00b7  " + ep);
        Font font = ctx.font();
        ctx.g().drawString(font, text, r.right() - font.width(text), r.y(), GOLD, true);
    }

    @Override
    public void drawThemeButton(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        round(g, r, hot);
        KitDraw.icon(g, Icons.MOON_WAXING_CRESCENT, r.x(), r.y(), Math.min(r.w(), r.h()), 0xFFFFFFFF);
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        round(g, r, hot);
        KitDraw.icon(g, Icons.CLOSE, r.x(), r.y(), Math.min(r.w(), r.h()), 0xFFFFFFFF);
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    private static final int SEAL_PX = 20;

    /** The round ring of a rail button, launcher or switch: the seal picture (20 px) centred on the box, brighter when lit. */
    private void round(GuiGraphics g, Rect r, boolean lit) {
        UiKit.draw(g, this, lit ? Part.SEAL_HOVER : Part.SEAL, r.x() + (r.w() - SEAL_PX) / 2, r.y() + (r.h() - SEAL_PX) / 2, SEAL_PX, SEAL_PX);
    }

    /** The floating bar along the bottom: the selection's name and pips, a line about it, and its actions as pills on the right; what does not fit moves behind "...". */
    @Override
    public void drawDetail(UiContext ctx, Rect r, Detail d) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        UiKit.draw(g, this, Part.PANEL, r);
        List<Integer> widths = new ArrayList<>();
        int primary = -1;
        for (int i = 0; i < d.actions().size(); i++) {
            widths.add(font.width(d.actions().get(i).label()));
            if (d.actions().get(i).primary()) {
                primary = i;
            }
        }
        // a line about the selection needs room to be read: the text keeps at least 45% of the bar and the buttons that do not fit go behind "..."
        int want = Math.max(font.width(d.title()) + (d.pips() > 0 ? d.pips() * 6 + 6 : 0), d.body().isEmpty() ? 0 : r.w() * 45 / 100);
        BarPlan plan = BarPlan.of(r, widths, primary, want);
        List<Detail.Action> shown = new ArrayList<>();
        List<Rect> rects = new ArrayList<>();
        for (int i = 0; i < d.actions().size(); i++) {
            if (plan.pills().get(i) != null) {
                shown.add(d.actions().get(i));
                rects.add(plan.pills().get(i));
                pill(ctx, plan.pills().get(i), d.actions().get(i));
            }
        }
        if (plan.overflow() != null) {
            Rect more = plan.overflow();
            boolean hot = ctx.hovered(more) || ctx.focused("detail:more");
            Part morePart = KitFaces.button(false, true, hot || overflowOpen, false);
            UiKit.draw(g, this, morePart, more);
            String moreLabel = LabelFit.cut(Component.translatable("supernaturalbloodlines.hub.more").getString(), more.w() - 6, font::width);
            g.drawString(font, moreLabel, more.x() + (more.w() - font.width(moreLabel)) / 2, more.y() + (more.h() - 8) / 2, KitFaces.label(kit(), morePart, ink()), false);
            ctx.target("detail:more", more, List.of(Component.translatable("supernaturalbloodlines.hub.more_actions")), () -> overflowOpen = !overflowOpen);
            if (overflowOpen) {
                // the actions that did not fit, stacked above the bar
                int y = r.y() - 2;
                for (int i = plan.hidden().size() - 1; i >= 0; i--) {
                    Detail.Action a = d.actions().get(plan.hidden().get(i));
                    int w = Math.max(90, font.width(a.label()) + 14);
                    Rect item = new Rect(more.right() - w, y - PILL_ROW, w, PILL_ROW);
                    y -= PILL_ROW + 2;
                    pill(ctx, item, a);
                    shown.add(a);
                    rects.add(item);
                }
            }
        } else {
            overflowOpen = false;
        }
        int textW = Math.max(40, plan.title());
        int x = r.x() + BarPlan.PAD + 2;
        String title = LabelFit.cut(d.title().getString(), textW - (d.pips() > 0 ? d.pips() * 6 + 6 : 0), font::width);
        g.drawString(font, title, x, r.y() + 6, 0xFFfff1ea, false);
        if (d.pips() > 0) {
            KitDraw.pips(g, x + font.width(title) + 6, r.y() + 8, d.pips(), d.pipsFilled(), RED, 0xFF4a2430);
        }
        int by = r.y() + 17;
        String bodyText = d.body().stream().map(Component::getString).collect(java.util.stream.Collectors.joining(" \u00b7 "));
        if (d.body().size() > 1) {
            // a bar of several lines of text shows them on one line, cut to fit: the whole of them are in the tooltip, one to a line
            ctx.target("detail:body", new Rect(x, r.y() + 16, textW, Math.max(10, r.h() - 18)), d.body(), null);
        }
        for (String line : LabelFit.wrap(bodyText, textW, 2, font::width)) { // two lines, the whole of them: the bar is wide enough for the text
            if (by + 8 > r.bottom() - 3) {
                break;
            }
            g.drawString(font, line, x, by, muted(), false);
            by += 9;
        }
        KitDraw.detailActions(ctx, this, rects, d.withActions(shown));
    }

    private static final int PILL_ROW = 16;
    /** The glow behind the stage is baked at this radius in pixels (a 128 pixel diameter) and stretched to the real one. */
    private static final int GLOW_PX = 64;
    private boolean overflowOpen;

    private void pill(UiContext ctx, Rect pill, Detail.Action a) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        boolean hot = a.enabled() && (ctx.hovered(pill) || ctx.focused("detail:" + a.id()));
        Part part = KitFaces.button(a.primary(), a.enabled(), hot, false);
        UiKit.draw(g, this, part, pill);
        String label = LabelFit.cut(a.label().getString(), pill.w() - 6, font::width);
        g.drawString(font, label, pill.x() + (pill.w() - font.width(label)) / 2, pill.y() + (pill.h() - 8) / 2, KitFaces.label(kit(), part, ink()), false);
    }

    /** Claimed tasks of the next rung out of its tasks; a full moon at the top of the ladder. */
    public static float progress(HubModel.LadderView l) {
        return l.active().map(a -> a.tasks() == 0 ? 0f : a.claimed() / (float) a.tasks()).orElse(1f);
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - dy * dy);
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gr = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }

    /** A round rail button; the chosen page is filled red with its name in a flag beside it when the screen is wide enough. */
    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        round(g, r, selected);
        KitDraw.icon(g, Icons.of(page), r.x(), r.y(), Math.min(r.w(), r.h()), selected ? 0xFFFFFFFF : 0xB0FFFFFF);
        if (page.group() == HubPage.Group.OPERATOR) {
            // a gold gem on the ring: the operator's page is apart from the rest
            UiKit.draw(g, this, Part.GEM_GOLD, r.right() - 8, r.y() - 2, 11, 11);
        }
        if (selected && g.guiWidth() >= BloodMoonLayout.FLAG_MIN_WIDTH) {
            Font font = Minecraft.getInstance().font;
            String label = Component.translatable(page.langKey()).getString();
            int w = font.width(label) + 12;
            UiKit.draw(g, this, Part.PLATE, r.right() + 3, r.y() + 3, w, r.h() - 6);
            g.drawString(font, label, r.right() + 9, r.y() + (r.h() - 8) / 2, ink(), false);
        }
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    /** A round launcher: the other mod's letter. */
    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        round(g, r, lit);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, GOLD, false);
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
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        KitWidgets.scrollbar(g, this, region, offset, max);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        UiKit.focus(g, this, r);
    }
    @Override
    public int paper() {
        return CARD;
    }

    @Override
    public List<TextPair> textPairs() {
        List<TextPair> pairs = new ArrayList<>(List.of(new TextPair("moon count", COUNT, PILL),
                new TextPair("race, gold, on the sky top", GOLD, SKY_TOP), new TextPair("race, gold, on the sky bottom", GOLD, SKY_BOTTOM),
                new TextPair("next rung, muted, on the sky top", muted(), SKY_TOP), new TextPair("next rung, muted, on the sky bottom", muted(), SKY_BOTTOM),
                new TextPair("EP, ink, on the sky top", ink(), SKY_TOP), new TextPair("EP, ink, on the sky bottom", ink(), SKY_BOTTOM),
                // the floating bar
                new TextPair("bar title", 0xFFfff1ea, 0xFF140810), new TextPair("bar body, muted", muted(), 0xFF140810),
                new TextPair("gold on a card", GOLD, CARD), new TextPair("ink on a card", ink(), CARD)));
        // the label of every kit button on its face (the pills of the floating bar)
        for (Part button : KitFaces.BUTTONS) {
            pairs.add(new TextPair("label on " + button.sprite(), KitFaces.label(kit(), button, ink()), KitFaces.face(kit(), button)));
        }
        return pairs;
    }
    @Override
    public int ink() {
        return 0xFFf0e0e0;
    }

    @Override
    public int muted() {
        return 0xFFc9a0a8;
    }

    @Override
    public int accent() {
        return GOLD;
    }

    @Override
    public int good() {
        return 0xFF7fe0a0;
    }

    @Override
    public int sourceColor(String source) {
        return switch (source) {
            case "vampirism" -> 0xFFff8a8a;
            case "werewolves" -> 0xFFf0a43a;
            case "enchanted" -> 0xFF6fe08a;
            case "tensura" -> 0xFF6fd6ea;
            default -> 0xFFc7a6ff;
        };
    }
}
