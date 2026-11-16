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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookBloodlinePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookGuidePage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookPackPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookQuestsPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookRitesPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.BookSkillsPage;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
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
    private final HubPageView quests = new BookQuestsPage();
    private final HubPageView rites = new BookRitesPage();
    private final HubPageView bloodline = new BookBloodlinePage();
    private final HubPageView guide = new BookGuidePage();

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return BloodMoonLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubPageView view(HubPage page) {
        return switch (page) {
            case PATH -> path;
            case SKILLS -> skills;
            case PACK -> pack;
            case QUESTS -> quests;
            case RITES -> rites;
            case BLOODLINE -> bloodline;
            case GUIDE -> guide;
            default -> HubThemeViews.super.view(page);
        };
    }

    /** The sky: a dark red wash, and a soft red glow behind the stage where the moon will be. */
    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        Rect s = l.frame();
        g.fillGradient(s.x(), s.y(), s.right(), s.bottom(), SKY_TOP, SKY_BOTTOM);
        Rect stage = l.region(BloodMoonLayout.MOON);
        int cx = stage.x() + stage.w() * 2 / 5;
        int cy = stage.y() + stage.h() / 2;
        int radius = Math.min(stage.w() / 2, stage.h() * 3 / 4);
        for (int i = 16; i >= 1; i--) {
            disc(g, cx, cy, radius * i / 16, 0x07000000 | 0xA0202A);
        }
    }

    /** Before a page: the cards behind the list pages (the moon pages draw their own), and nothing else. */
    @Override
    public void drawOverlay(HubScreen.Frame f, HubPage page) {
        boolean cards = switch (page) {
            case QUESTS, RITES, BLOODLINE, GUIDE, LOADOUT -> true;
            case SKILLS -> skills.legacyActive();
            default -> false;
        };
        if (page.locked(f.view.context())) {
            cards = true;
        }
        if (cards) {
            for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                Rect c = BloodMoonLayout.card(f.region(region));
                f.g.fill(c.x(), c.y(), c.right(), c.bottom(), 0xE61a0a10);
                f.g.renderOutline(c.x(), c.y(), c.w(), c.h(), EDGE);
            }
        }
        if (page == HubPage.PATH || page == HubPage.PACK) {
            Rect c = BloodMoonLayout.card(f.region(BloodMoonLayout.RIGHT));
            f.g.fill(c.x(), c.y(), c.right(), c.bottom(), 0xE61a0a10);
            f.g.renderOutline(c.x(), c.y(), c.w(), c.h(), EDGE);
        }
    }

    /** The moon: a glow, a disc lit from the bottom up to the progress, a few craters, a ring of ticks, and the live model in front. */
    @Override
    public void drawEmblem(UiContext ctx, Rect area, EmblemData data) {
        GuiGraphics g = ctx.g();
        int cx = area.x() + area.w() / 2;
        int cy = area.y() + area.h() / 2;
        int r = Math.max(12, Math.min(area.w(), area.h()) / 2 - 8);
        disc(g, cx, cy, r + 6, 0x28e2493f);
        disc(g, cx, cy, r + 3, 0x30e2493f);
        float fill = Math.max(0.1f, data.progress());
        int line = cy + r - Math.round(2 * r * fill);
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - dy * dy);
            int y = cy + dy;
            float k = (dy + r) / (2f * r);
            int color = y >= line ? mix(0xFFffb3a3, 0xFF9a1820, k) : mix(0xFF4a1520, 0xFF2a0b12, k);
            g.fill(cx - half, y, cx + half, y + 1, color);
        }
        int[][] craters = {{-r * 45 / 100, -r * 40 / 100, r / 6}, {r * 30 / 100, -r * 15 / 100, r / 9}, {-r * 60 / 100, r * 25 / 100, r / 10},
                {r * 20 / 100, r * 50 / 100, r / 7}, {r * 55 / 100, r * 30 / 100, r / 12}};
        for (int[] c : craters) {
            disc(g, cx + c[0], cy + c[1], Math.max(1, c[2]), 0x22000000);
        }
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
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x(), r.y(), r.w(), hot ? RED : EDGE);
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x() + 1, r.y() + 1, r.w() - 2, hot ? 0xFF4a1520 : PILL);
        KitDraw.icon(g, Icons.MOON_WAXING_CRESCENT, r.x() + (r.w() - 12) / 2, r.y() + (r.h() - 12) / 2, 12, RED);
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    @Override
    public void drawClose(GuiGraphics g, Rect r, boolean hot, boolean focused) {
        KitDraw.icon(g, Icons.CLOSE, r.x() + (r.w() - 12) / 2 + 1, r.y() + (r.h() - 12) / 2, 12, hot ? GOLD : 0xFFf0e0e0);
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    /** The floating bar along the bottom: the selection's name and pips, a line about it, and its actions as pills on the right. */
    @Override
    public void drawDetail(UiContext ctx, Rect r, Detail d) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xEE140810);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), EDGE);
        List<Rect> pills = new ArrayList<>();
        int right = r.right() - 6;
        for (int i = d.actions().size() - 1; i >= 0; i--) {
            Detail.Action a = d.actions().get(i);
            int w = Math.min(font.width(a.label()) + 14, Math.max(40, r.w() / 3));
            Rect pill = new Rect(right - w, r.y() + (r.h() - 16) / 2, w, 16);
            right = pill.x() - 4;
            pills.add(0, pill);
            boolean hot = a.enabled() && (ctx.hovered(pill) || ctx.focused("detail:" + a.id()));
            int fill = a.primary() ? (a.enabled() ? (hot ? 0xFFf06a5f : RED) : 0xFF5a3a42) : (hot ? 0xFF3a1822 : PILL);
            g.fill(pill.x(), pill.y(), pill.right(), pill.bottom(), a.primary() ? fill : EDGE);
            g.fill(pill.x() + 1, pill.y() + 1, pill.right() - 1, pill.bottom() - 1, fill);
            String label = font.plainSubstrByWidth(a.label().getString(), pill.w() - 6);
            g.drawString(font, label, pill.x() + (pill.w() - font.width(label)) / 2, pill.y() + 4,
                    a.enabled() ? (a.primary() ? 0xFFfff1ea : ink()) : 0xFF9a7a82, false);
        }
        int textW = Math.max(40, right - r.x() - 14);
        int x = r.x() + 8;
        g.drawString(font, font.plainSubstrByWidth(d.title().getString(), textW - (d.pips() > 0 ? d.pips() * 6 + 6 : 0)), x, r.y() + 6, 0xFFfff1ea, false);
        if (d.pips() > 0) {
            KitDraw.pips(g, x + Math.min(font.width(d.title()), textW - d.pips() * 6 - 6) + 6, r.y() + 8, d.pips(), d.pipsFilled(), RED, 0xFF4a2430);
        }
        int by = r.y() + 17;
        for (var line : font.split(d.body().isEmpty() ? Component.empty() : d.body().get(0), (int) (textW / 0.75f))) {
            if (by + 7 > r.bottom() - 3) {
                break;
            }
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x, by, 0);
            pose.scale(0.75f, 0.75f, 1f);
            g.drawString(font, line, 0, 0, muted(), false);
            pose.popPose();
            by += 7;
        }
        KitDraw.detailActions(ctx, this, pills, d);
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
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x(), r.y(), r.w(), selected ? 0xFFffb3a3 : EDGE);
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x() + 1, r.y() + 1, r.w() - 2, selected ? RED : CARD);
        KitDraw.icon(g, Icons.of(page), r.x() + (r.w() - 12) / 2, r.y() + (r.h() - 12) / 2, 12, selected ? 0xFFfff1ea : 0xFF8A5966);
        if (selected && g.guiWidth() >= BloodMoonLayout.FLAG_MIN_WIDTH) {
            Font font = Minecraft.getInstance().font;
            String label = Component.translatable(page.langKey()).getString();
            int w = (int) Math.ceil(font.width(label) * 0.75f) + 8;
            g.fill(r.right() + 3, r.y() + 4, r.right() + 3 + w, r.y() + r.h() - 4, PILL);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(r.right() + 7, r.y() + 8, 0);
            pose.scale(0.75f, 0.75f, 1f);
            g.drawString(font, label, 0, 0, RED, false);
            pose.popPose();
        }
        if (focused) {
            KitDraw.focusRing(g, r, GOLD);
        }
    }

    /** A round launcher: the other mod's letter. */
    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x(), r.y(), r.w(), lit ? GOLD : EDGE);
        KitDraw.shape(g, Shapes.Shape.ROUND, r.x() + 1, r.y() + 1, r.w() - 2, lit ? 0xFF2a1a1c : CARD);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, GOLD, false);
    }
    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, RungStatus status) {
        // a gem: a diamond of rows
        int fill = switch (status) {
            case REACHED -> RED;
            case CURRENT -> 0xFF1a0a10;
            case LOCKED -> 0xFF1a0a10;
        };
        int edge = switch (status) {
            case REACHED -> 0xFFffb3a3;
            case CURRENT -> GOLD;
            case LOCKED -> 0xFF8a5a64; // 3.3:1 on the cards: a sealed rung must still be seen (WCAG 1.4.11)
        };
        for (int i = 0; i < MARK; i++) {
            int half = 4 - Math.abs(4 - i);
            g.fill(x + 4 - half, y + i, x + 5 + half, y + i + 1, edge);
            if (half > 0) {
                g.fill(x + 5 - half, y + i, x + 4 + half, y + i + 1, fill);
            }
        }
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, TaskState state) {
        g.renderOutline(x, y, BOX, BOX, state == TaskState.CLAIMABLE ? good() : muted());
        if (state == TaskState.CLAIMABLE) {
            g.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, 0xFF3f8f5a);
        } else if (state == TaskState.CLAIMED) {
            g.fill(x + 2, y + 2, x + BOX - 2, y + BOX - 2, muted());
        }
    }

    @Override
    public void drawStamp(GuiGraphics g, int x, int y, boolean lit) {
        disc(g, x + STAMP / 2, y + STAMP / 2, STAMP / 2, lit ? GOLD : 0xFFb07a1a);
        disc(g, x + STAMP / 2, y + STAMP / 2, STAMP / 2 - 2, lit ? 0xFFd23a44 : 0xFF9a1820);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, "\u2713", x + 4, y + 3, 0xFFfff1ea, false);
    }

    @Override
    public void drawRule(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, EDGE);
    }

    @Override
    public void drawBar(GuiGraphics g, Rect r, float fraction) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF3a1a22);
        g.fill(r.x(), r.y(), r.x() + Math.round(r.w() * Math.max(0f, Math.min(1f, fraction))), r.bottom(), RED);
    }

    @Override
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        int x = region.right() + 2;
        g.fill(x, region.y(), x + 2, region.bottom(), 0xFF2a0d15);
        int thumb = Math.max(8, region.h() * region.h() / (region.h() + max));
        int y = region.y() + (region.h() - thumb) * offset / Math.max(1, max);
        g.fill(x, y, x + 2, y + thumb, RED);
    }

    @Override
    public void drawFocus(GuiGraphics g, Rect r) {
        g.renderOutline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, GOLD);
    }

    @Override
    public int paper() {
        return CARD;
    }

    @Override
    public List<TextPair> textPairs() {
        return List.of(new TextPair("moon count", COUNT, PILL),
                new TextPair("race, gold, on the sky top", GOLD, SKY_TOP), new TextPair("race, gold, on the sky bottom", GOLD, SKY_BOTTOM),
                new TextPair("next rung, muted, on the sky top", muted(), SKY_TOP), new TextPair("next rung, muted, on the sky bottom", muted(), SKY_BOTTOM),
                new TextPair("EP, ink, on the sky top", ink(), SKY_TOP), new TextPair("EP, ink, on the sky bottom", ink(), SKY_BOTTOM));
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
