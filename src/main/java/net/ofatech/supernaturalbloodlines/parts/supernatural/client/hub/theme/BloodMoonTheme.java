package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final HubPageView PATH = new BloodMoonPathPage();

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return BloodMoonLayout.of(width, height, tabs, seals);
    }

    @Override
    public HubPageView view(HubPage page) {
        return page == HubPage.PATH ? PATH : HubThemeViews.super.view(page);
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        Rect s = l.frame();
        g.fillGradient(s.x(), s.y(), s.right(), s.bottom(), SKY_TOP, SKY_BOTTOM);
        for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
            Rect c = BloodMoonLayout.card(l.region(region));
            g.fill(c.x(), c.y(), c.right(), c.bottom(), 0xE61a0a10);
            g.renderOutline(c.x(), c.y(), c.w(), c.h(), EDGE);
        }
    }

    @Override
    public void drawOverlay(HubScreen.Frame f, HubPage page) {
        GuiGraphics g = f.g;
        Font font = f.font();
        Rect band = f.region(BloodMoonLayout.MOON);
        int r = Math.max(14, Math.min(band.h() / 2 - 10, 44));
        int cx = band.x() + band.w() / 2;
        int cy = band.y() + band.h() / 2;
        List<HubModel.LadderView> ladders = f.view.ladders();
        // glow, then the moon: lit from the bottom up to the progress of the creature ladder, the rest in shadow
        disc(g, cx, cy, r + 4, 0x30e2493f);
        float fill = ladders.isEmpty() ? 0f : progress(ladders.get(0));
        int line = cy + r - Math.round(2 * r * fill);
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - dy * dy);
            int y = cy + dy;
            int color = y >= line ? mix(0xFFffb3a3, 0xFF9a1820, (dy + r) / (2f * r)) : mix(0xFF4a1520, 0xFF2a0b12, (dy + r) / (2f * r));
            g.fill(cx - half, y, cx + half, y + 1, color);
            g.fill(cx - half - 1, y, cx - half, y + 1, 0xFF9a2a33); // the rim
            g.fill(cx + half, y, cx + half + 1, y + 1, 0xFF9a2a33);
        }
        if (!ladders.isEmpty()) {
            // the count in the moon, so the fill reads without the tooltip
            Component count = ladders.get(0).active().map(a -> (Component) Component.literal(a.claimed() + " / " + a.tasks()))
                    .orElse(Component.translatable("supernaturalbloodlines.hub.moon.full"));
            // on a dark pill: the lit moon runs from pale pink to deep red, and no one text colour reads on all of it
            int cw = font.width(count);
            g.fill(cx - cw / 2 - 3, cy - 6, cx + cw / 2 + 3, cy + 6, PILL);
            g.drawString(font, count, cx - cw / 2, cy - 4, COUNT, false);
        }
        if (ladders.size() > 1) {
            ring(g, cx, cy, r + 2, progress(ladders.get(1)));
        }
        for (int i = 0; i < ladders.size() && i < 2; i++) {
            phases(f, ladders.get(i), cx, cy, r + 9, i == 0);
        }
        // the race on the left of the band, the points on the right
        int x = band.x() + 2;
        int y = band.y() + 4;
        for (HubModel.LadderView l : ladders) {
            Component race = Component.translatable("supernaturalbloodlines.supernatural.race." + l.race().id());
            g.drawString(font, font.plainSubstrByWidth(race.getString(), cx - r - 14 - x), x, y, GOLD, true);
            y += 10;
            Component next = l.active().map(a -> (Component) Component.translatable("supernaturalbloodlines.hub.moon.next",
                    Component.translatableWithFallback("supernaturalbloodlines.hub.rung." + a.id(), a.name())))
                    .orElse(Component.translatable("supernaturalbloodlines.hub.moon.top"));
            for (var seq : font.split(next, cx - r - 14 - x)) {
                g.drawString(font, seq, x, y, muted(), false);
                y += 9;
            }
            y += 3;
        }
        // the counters sit at the bottom right of the band: the top right is where vanilla's toasts are drawn
        Rect counters = BloodMoonLayout.counters(band);
        int rx = counters.right();
        Component sp = Component.translatable("supernaturalbloodlines.hub.moon.sp", f.view.state().sp());
        g.drawString(font, sp, rx - font.width(sp), counters.y(), GOLD, true);
        HubHooks.tensura().ifPresent(info -> {
            Component ep = Component.translatable("supernaturalbloodlines.hub.moon.ep", String.format("%,d", (long) info.ep()));
            g.drawString(font, ep, rx - font.width(ep), counters.y() + 10, ink(), false);
        });
    }

    /** Claimed tasks of the next rung out of its tasks; a full moon at the top of the ladder. */
    static float progress(HubModel.LadderView l) {
        return l.active().map(a -> a.tasks() == 0 ? 0f : a.claimed() / (float) a.tasks()).orElse(1f);
    }

    /** The rungs as gems on an arc: the creature ladder below the moon, left to right; a magic ladder above it. */
    private void phases(HubScreen.Frame f, HubModel.LadderView ladder, int cx, int cy, int radius, boolean below) {
        List<HubModel.RungRow> rungs = ladder.rungs();
        int n = rungs.size();
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.5 : i / (double) (n - 1);
            double angle = Math.toRadians(below ? 160 - 140 * t : 200 + 140 * t);
            int gx = cx + (int) Math.round(radius * Math.cos(angle)) - 4;
            int gy = cy + (int) Math.round(radius * Math.sin(angle)) - 4;
            HubModel.RungRow row = rungs.get(i);
            drawRungMark(f.g, gx, gy, row.status());
            String id = "phase:" + ladder.race().id() + ":" + row.id();
            Rect hit = new Rect(gx - 1, gy - 1, MARK + 2, MARK + 2);
            f.target(id, hit, PathPage.rungTooltip(ladder, row), null);
            if (f.focused(id)) {
                drawFocus(f.g, hit);
            }
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, float fraction) {
        int steps = 72;
        for (int i = 0; i < steps; i++) {
            double a = Math.toRadians(-90 + 360.0 * i / steps);
            int x = cx + (int) Math.round(r * Math.cos(a));
            int y = cy + (int) Math.round(r * Math.sin(a));
            g.fill(x, y, x + 1, y + 1, i < steps * fraction ? GOLD : 0xFF3a1a22);
        }
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

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), selected ? 0xFF2a0d15 : CARD);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), selected ? RED : EDGE);
        g.setColor(selected ? 1f : 0.54f, selected ? 0.7f : 0.35f, selected ? 0.64f : 0.4f, 1f);
        g.blit(ICONS, r.x() + (r.w() - 8) / 2, r.y() + (r.h() - 8) / 2, page.ordinal() * 9, 24, 8, 8, 64, 32);
        g.setColor(1f, 1f, 1f, 1f);
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), lit ? 0xFF1c1c28 : 0xFF101018);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), lit ? 0xFF6a6a88 : 0xFF2c2c38);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2, color, false);
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
