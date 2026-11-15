package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;

/** The default theme: an open grimoire, dark ink on parchment, ribbon bookmarks for pages and wax for what can be pressed. */
public final class GrimoireTheme implements HubTheme {
    private static final ResourceLocation PARCHMENT = tex("parchment");
    private static final ResourceLocation WIDGETS = tex("widgets");
    private static final int WIDGETS_W = 64;
    private static final int WIDGETS_H = 32;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hub/grimoire/" + name + ".png");
    }

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return GrimoireLayout.of(width, height, tabs, seals);
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        Rect b = l.frame();
        g.fill(b.x() - 2, b.y() - 2, b.right() + 2, b.bottom() + 2, 0xFF120506);
        g.fill(b.x(), b.y(), b.right(), b.bottom(), 0xFF3a1418);
        outline(g, b.inset(2), 0xFF5e1f25);
        Rect lp = GrimoireLayout.page(l.region(HubLayout.MAIN));
        Rect rp = GrimoireLayout.page(l.region(HubLayout.SIDE));
        paper(g, lp);
        paper(g, rp);
        // the gutter: the paper darkens towards the spine
        for (int i = 0; i < 8; i++) {
            int a = (0x50 - i * 10) << 24;
            g.fill(lp.right() - 1 - i, lp.y(), lp.right() - i, lp.bottom(), a | 0x46280a);
            g.fill(rp.x() + i, rp.y(), rp.x() + i + 1, rp.bottom(), a | 0x46280a);
        }
        g.fill(lp.right(), b.y() + 2, rp.x(), b.bottom() - 2, 0xFF2a1708);
        g.fill(lp.right() + 1, b.y() + 2, lp.right() + 2, b.bottom() - 2, 0xFF5a3a1c);
        g.fill(rp.x() - 2, b.y() + 2, rp.x() - 1, b.bottom() - 2, 0xFF5a3a1c);
        outline(g, lp, 0xFF7a5a32);
        outline(g, rp, 0xFF7a5a32);
    }

    private static void paper(GuiGraphics g, Rect r) {
        for (int y = r.y(); y < r.bottom(); y += 64) {
            for (int x = r.x(); x < r.right(); x += 64) {
                g.blit(PARCHMENT, x, y, 0, 0, Math.min(64, r.right() - x), Math.min(64, r.bottom() - y), 64, 64);
            }
        }
    }

    private static void outline(GuiGraphics g, Rect r, int color) {
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), color);
    }

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        int w = selected ? r.w() + 4 : r.w();
        g.fill(r.x(), r.y() - 1, r.x() + w + 1, r.bottom() + 1, 0xFF2a0a0d);
        g.fill(r.x(), r.y(), r.x() + w, r.bottom(), selected ? 0xFF9a1820 : 0xFF6a1a20);
        if (selected) {
            g.renderOutline(r.x() + 1, r.y() + 1, w - 2, r.h() - 2, 0xFFe8b84a);
        }
        int icon = page.ordinal();
        g.setColor(selected ? 1f : 0.91f, selected ? 0.886f : 0.69f, selected ? 0.627f : 0.69f, 1f);
        g.blit(WIDGETS, r.x() + 4 + (w - 4 - 8) / 2, r.y() + (r.h() - 8) / 2, icon * 9, 24, 8, 8, WIDGETS_W, WIDGETS_H);
        g.setColor(1f, 1f, 1f, 1f);
        if (focused) {
            drawFocus(g, new Rect(r.x(), r.y(), w, r.h()));
        }
    }

    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        g.blit(WIDGETS, r.x(), r.y(), r.w(), r.h(), lit ? 45 : 30, 10, STAMP, STAMP, WIDGETS_W, WIDGETS_H);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        g.drawString(font, letter, r.x() + (r.w() - font.width(letter)) / 2 + 1, r.y() + (r.h() - 8) / 2 + 1, color, false);
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
            case OPEN, FROZEN -> 30;
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
        g.fill(x + fade, y, x + w - fade, y + 1, 0xFF8a6a3a);
        for (int i = 0; i < fade; i++) {
            int a = (0xFF * i / fade) << 24;
            g.fill(x + i, y, x + i + 1, y + 1, a | 0x8a6a3a);
            g.fill(x + w - 1 - i, y, x + w - i, y + 1, a | 0x8a6a3a);
        }
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
        g.renderOutline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, 0xFFe8b84a);
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
