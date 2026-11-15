package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubModel;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.HubHooks;

/**
 * The Tensura expansion: Tensura's own visual language (a grey bevelled frame, a navy list, teal-edged navy panels, octagons, gold for
 * what is current) on a wider window, drawn with fills: our own art in Tensura's style, nothing of Tensura's shipped or drawn. Pages and
 * launchers are two lists on the left; every page uses the shared views, whose two regions are the two panels. Registered by
 * {@code supernatural_tensura} only, so without Tensura {@code hub_theme = TENSURA} shows the Grimoire.
 */
public final class TensuraHubTheme implements HubTheme {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hub/grimoire/widgets.png");
    private static final int FRAME = 0xFF505050;
    private static final int LIGHT = 0xFF9c9c9c;
    private static final int DARK = 0xFF2b2b2b;
    private static final int NAVY = 0xFF192032;
    private static final int PANEL = 0xFF041d2c;
    private static final int TEAL = 0xFF093c4c;
    private static final int BUTTON = 0xFF5a5a5a;
    private static final int GOLD = 0xFFffd36b;
    private static final int CYAN = 0xFF5fd3e6;

    @Override
    public HubLayout layout(int width, int height, int tabs, int seals) {
        return TensuraHubLayout.of(width, height, tabs, seals);
    }

    @Override
    public void drawFrame(GuiGraphics g, HubLayout l) {
        Rect f = l.frame();
        g.fill(f.x() - 3, f.y() - 3, f.right() + 3, f.bottom() + 3, 0xFF000000);
        bevel(g, f, FRAME, LIGHT, DARK);
        Rect nav = TensuraHubLayout.nav(f);
        box(g, nav, NAVY, 0xFF324064);
        for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
            box(g, TensuraHubLayout.panel(l.region(region)), PANEL, TEAL);
        }
        if (!l.seals().isEmpty()) {
            // the line between the page list and the mod list
            int y = l.seals().get(0).y() - 4;
            g.fill(nav.x() + 4, y, nav.right() - 4, y + 1, 0xFF324064);
        }
    }

    /** Tensura's bevel: a light top-left edge and a dark bottom-right one, 2 px. */
    private static void bevel(GuiGraphics g, Rect r, int face, int light, int dark) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), face);
        g.fill(r.x(), r.y(), r.right(), r.y() + 2, light);
        g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), light);
        g.fill(r.x(), r.bottom() - 2, r.right(), r.bottom(), dark);
        g.fill(r.right() - 2, r.y(), r.right(), r.bottom(), dark);
    }

    /** A black-edged box with an inner coloured line, as Tensura's panels are. */
    private static void box(GuiGraphics g, Rect r, int face, int inner) {
        g.fill(r.x() - 2, r.y() - 2, r.right() + 2, r.bottom() + 2, 0xFF000000);
        g.fill(r.x(), r.y(), r.right(), r.bottom(), face);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), inner);
    }

    private static void row(GuiGraphics g, Rect r, boolean selected, boolean lit, String label, int labelColor, Runnable icon) {
        Font font = Minecraft.getInstance().font;
        g.fill(r.x() - 1, r.y() - 1, r.right() + 1, r.bottom() + 1, 0xFF000000);
        bevel(g, r, selected ? 0xFF24324f : lit ? 0xFF6a6a6a : BUTTON, selected ? 0xFF4a6090 : 0xFF8a8a8a, selected ? 0xFF101828 : 0xFF3a3a3a);
        icon.run();
        String text = font.plainSubstrByWidth(label, r.w() - 18);
        g.drawString(font, text, r.x() + 15, r.y() + (r.h() - 8) / 2, labelColor, true);
    }

    @Override
    public void drawTab(GuiGraphics g, Rect r, HubPage page, boolean selected, boolean focused) {
        row(g, r, selected, false, Component.translatable(page.langKey()).getString(), selected ? 0xFFffe680 : 0xFFeef5ff, () -> {
            g.setColor(selected ? 1f : 0.93f, selected ? 0.9f : 0.96f, selected ? 0.5f : 1f, 1f);
            g.blit(ICONS, r.x() + 4, r.y() + (r.h() - 8) / 2, page.ordinal() * 9, 24, 8, 8, 64, 32);
            g.setColor(1f, 1f, 1f, 1f);
        });
        if (focused) {
            drawFocus(g, r);
        }
    }

    @Override
    public void drawSeal(GuiGraphics g, Rect r, String letter, int color, boolean lit) {
        row(g, r, false, lit, letter, 0xFFeef5ff, () -> { });
    }

    @Override
    public void drawLauncher(GuiGraphics g, Rect r, HubHooks.Launcher launcher, boolean lit) {
        Font font = Minecraft.getInstance().font;
        // the short name (the mod's), as the full one ("Tensura: the status menu") does not fit the list; the tooltip has it
        Component label = Component.translatableWithFallback("supernaturalbloodlines.hub.launcher.short." + launcher.id(), launcher.id());
        row(g, r, false, lit, label.getString(), 0xFFeef5ff,
                () -> g.drawString(font, launcher.letter(), r.x() + 5, r.y() + (r.h() - 8) / 2, launcher.color(), true));
    }

    @Override
    public void drawRungMark(GuiGraphics g, int x, int y, HubModel.RungStatus status) {
        int edge = switch (status) {
            case REACHED -> CYAN;
            case CURRENT -> GOLD;
            case LOCKED -> TEAL;
        };
        // an octagon: the square with its corners cut
        g.fill(x + 2, y, x + MARK - 2, y + MARK, edge);
        g.fill(x, y + 2, x + MARK, y + MARK - 2, edge);
        g.fill(x + 1, y + 1, x + MARK - 1, y + MARK - 1, edge);
        g.fill(x + 3, y + 2, x + MARK - 3, y + MARK - 2, 0xFF02141c);
        g.fill(x + 2, y + 3, x + MARK - 2, y + MARK - 3, 0xFF02141c);
    }

    @Override
    public void drawTaskBox(GuiGraphics g, int x, int y, HubModel.TaskState state) {
        g.renderOutline(x, y, BOX, BOX, state == HubModel.TaskState.CLAIMABLE ? good() : muted());
        if (state == HubModel.TaskState.CLAIMABLE) {
            g.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, 0xFF2f8f5a);
        } else if (state == HubModel.TaskState.CLAIMED) {
            g.fill(x + 2, y + 2, x + BOX - 2, y + BOX - 2, muted());
        }
    }

    @Override
    public void drawStamp(GuiGraphics g, int x, int y, boolean lit) {
        Rect r = new Rect(x, y, STAMP, STAMP);
        g.fill(r.x() - 1, r.y() - 1, r.right() + 1, r.bottom() + 1, 0xFF000000);
        bevel(g, r, lit ? 0xFFffb12e : 0xFFe08a00, 0xFFffe680, 0xFF8a4a00);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, "\u2713", x + 4, y + 3, 0xFF1a0a00, false);
    }

    @Override
    public void drawRule(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 2, 0xFF02141c);
    }

    @Override
    public void drawBar(GuiGraphics g, Rect r, float fraction) {
        g.fill(r.x() - 1, r.y() - 1, r.right() + 1, r.bottom() + 1, 0xFF000000);
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF8d8d8d);
        g.fillGradient(r.x(), r.y(), r.x() + Math.round(r.w() * Math.max(0f, Math.min(1f, fraction))), r.bottom(), 0xFFffb12e, 0xFFff6a00);
    }

    @Override
    public void drawScrollbar(GuiGraphics g, Rect region, int offset, int max) {
        int x = region.right() + 2;
        g.fill(x, region.y(), x + 3, region.bottom(), 0xFF02141c);
        int thumb = Math.max(8, region.h() * region.h() / (region.h() + max));
        int y = region.y() + (region.h() - thumb) * offset / Math.max(1, max);
        g.fill(x, y, x + 3, y + thumb, LIGHT);
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
    public int ink() {
        return 0xFFeef5ff;
    }

    @Override
    public int muted() {
        return 0xFF9fc0cc;
    }

    @Override
    public int accent() {
        return GOLD;
    }

    @Override
    public int good() {
        return 0xFF7ee08f;
    }

    @Override
    public int sourceColor(String source) {
        return switch (source) {
            case "vampirism" -> 0xFFff8a8a;
            case "werewolves" -> 0xFFffbe5c;
            case "enchanted" -> 0xFF7ee08f;
            case "tensura" -> CYAN;
            default -> 0xFFc9a8ff;
        };
    }
}
