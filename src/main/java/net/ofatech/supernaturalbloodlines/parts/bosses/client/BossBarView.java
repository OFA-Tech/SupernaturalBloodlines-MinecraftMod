package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;

/**
 * Draws the pieces of the boss row from the kit: the focus boss's card (the frame, the portrait under its plate, the name plate, the health bar with its ticks, the
 * health in numbers and the chips), a token (the portrait plate, a health ring and a form mark) and the "+N" token. Nothing is measured here and no list or string is
 * built; {@link BossBarInfo#prepare} has worked out the text, the cut name and the geometry. ({@code UiKit.draw} builds the small plan of a nine-slice per draw: the only
 * allocation left in a frame.) Client only.
 */
final class BossBarView {
    private static final int SEAM_COLOUR = 0xFFE8A0;
    /** The portrait sits this far inside a plate of the kit. */
    private static final int ART_IN_PLATE = 5;
    private static final int RING = 2;
    private static final int MARK = 10;

    private BossBarView() {
    }

    /** The chip picture of chip {@code i} when lit: the gold one while a shifter shifts, the lit one otherwise. */
    static Part lit(BossBarForms.Look look, int i) {
        return look.set() == BossBarForms.Set.SHIFTER && i == 1 ? Part.CHIP_SHIFTING : Part.CHIP_ON;
    }

    /** The picture of the portrait plate: gold while the boss shifts. */
    static Part plate(BossBarForms.Look look) {
        return look.shifting() ? Part.BOSS_PORTRAIT_PLATE_SHIFTING : Part.BOSS_PORTRAIT_PLATE;
    }

    /** {@code color} with its alpha scaled by {@code a} (0 to 1): the text of a card that is fading. */
    static int faded(int color, float a) {
        int alpha = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, a)));
        return Math.max(alpha, 5) << 24 | color & 0xFFFFFF; // an alpha under 4 would be drawn opaque by the font
    }

    /** @param a how opaque the whole card is: 1 normally, less while two cards cross-fade */
    static void draw(GuiGraphics g, Font font, HudKit kit, BossBarInfo i, int x, int y, long tick, double now, boolean reduce, float progress, float a) {
        BossBarLayout.Geometry geo = i.geo;
        boolean shifting = i.look.shifting();
        UiKit.draw(g, kit, Part.BOSS_FRAME, x, y, geo.cardW(), BossBarLayout.CARD_H, a);

        // the portrait, then its plate over it (the plate's window is clear)
        if (i.portrait != null) {
            portrait(g, i, x + geo.artX(), y + geo.artY(), tick, reduce, a);
        }
        UiKit.draw(g, kit, plate(i.look), x + geo.plateX(), y + geo.plateY(), BossBarLayout.PLATE, BossBarLayout.PLATE, a);

        // the name
        UiKit.draw(g, kit, Part.BOSS_NAME_PLATE, x + geo.nameX(), y + geo.nameY(), geo.nameW(), BossBarLayout.NAME_H, a);
        g.drawString(font, i.nameShown, x + geo.nameX() + BossBarLayout.NAME_TEXT_X, y + geo.nameY() + BossBarLayout.NAME_TEXT_Y,
                faded(UiKit.labelOver(kit, Part.BOSS_NAME_PLATE, BossFaces.name(kit)), a), false);

        // the health: the track, the fill (gold while the boss can be hit), the ticks over it
        UiKit.draw(g, kit, Part.BOSS_BAR_TRACK, x + geo.trackX(), y + geo.trackY(), geo.trackW(), BossBarLayout.TRACK_H, a);
        int fill = BossBarLayout.fillWidth(geo.fillMaxW(), progress);
        if (fill > 0) {
            boolean gold = shifting || i.fallen;
            float alpha = (shifting ? BossBarMotion.shiftFillPercent(tick, reduce) / 100f : 1f) * a;
            UiKit.draw(g, kit, gold ? Part.BOSS_BAR_FILL_SHIFTING : Part.BOSS_BAR_FILL, x + geo.fillX(), y + geo.fillY(), fill, BossBarLayout.FILL_H, alpha);
        }
        for (int tx : geo.tickX()) {
            UiKit.draw(g, kit, Part.BOSS_BAR_TICK, x + tx, y + geo.fillY(), 1, BossBarLayout.FILL_H, a);
        }
        if (!i.hpShown.isEmpty()) {
            g.drawString(font, i.hpShown, x + geo.hpX(), y + geo.hpY(), faded(UiKit.labelOver(kit, Part.BOSS_FRAME, BossFaces.health(kit)), a), false);
        }

        chips(g, font, kit, i, x, y, now, reduce, a);
    }

    private static void portrait(GuiGraphics g, BossBarInfo i, int ax, int ay, long tick, boolean reduce, float a) {
        if (a < 1f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, a);
        }
        if (i.portrait.seam()) {
            BossPortraitTex.drawSeam(g, i.portrait.main(), i.portrait.wolf(), ax, ay, BossPortraits.SEAM_X);
            int flash = BossBarMotion.seamPercent(tick, reduce);
            if (flash > 0) {
                g.fill(ax + BossPortraits.SEAM_X, ay, ax + BossPortraits.SEAM_X + 1, ay + BossBarLayout.ART, (Math.round(flash * 2.55f * a)) << 24 | SEAM_COLOUR);
            }
        } else {
            BossPortraitTex.draw(g, i.portrait.main(), ax, ay);
        }
        if (a < 1f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private static void chips(GuiGraphics g, Font font, HudKit kit, BossBarInfo i, int x, int y, double now, boolean reduce, float a) {
        BossBarLayout.Geometry geo = i.geo;
        float fade = BossBarMotion.chipFade(i.activeSince, now, reduce);
        for (int c = 0; c < geo.chipW().length; c++) {
            int w = geo.chipW()[c];
            if (w == 0) {
                continue;
            }
            int cx = x + geo.chipX()[c];
            int cy = y + geo.chipY();
            boolean on = c == i.look.active();
            Part lit = lit(i.look, c);
            // the lit chip comes in over the plain one, and the one that was lit goes out: a fade of four ticks, none with reduce motion
            UiKit.draw(g, kit, Part.CHIP, cx, cy, w, BossBarLayout.CHIP_H, a);
            float alpha = on ? fade : c == i.prevActive ? 1f - fade : 0f;
            if (alpha > 0.001f) {
                UiKit.draw(g, kit, lit, cx, cy, w, BossBarLayout.CHIP_H, alpha * a);
            }
            boolean showsLit = alpha >= 0.5f;
            int text = UiKit.labelOver(kit, showsLit ? lit : Part.CHIP, kit.ink());
            g.drawString(font, i.labels[c], cx + (w - i.chipTextW[c]) / 2, cy + BossBarLayout.CHIP_TEXT_Y, faded(text, a), false);
        }
    }

    /** A token at ({@code x}, {@code rowY} + the token's offset): the portrait plate, the health ring round it and, for a boss with forms, a mark of its form. */
    static void token(GuiGraphics g, Font font, HudKit kit, BossBarInfo i, int x, int rowY, long tick, boolean reduce, float progress) {
        int ty = rowY + BossRow.TOKEN_DY;
        int px = x + RING;
        int py = ty + RING;
        // the plate is round and the ring square: an opaque backing in the track's colour keeps the sky out of the corners, and gives the ring its 3:1 against what it stands on
        g.fill(x, ty, x + BossRow.TOKEN, ty + BossRow.TOKEN, BossFaces.ringTrack(kit));
        UiKit.draw(g, kit, plate(i.look), px, py, BossBarLayout.PLATE, BossBarLayout.PLATE);
        if (i.portrait != null) {
            portrait(g, i, px + ART_IN_PLATE, py + ART_IN_PLATE, tick, reduce, 1f);
        }
        ring(g, kit, x, ty, progress, i.look.shifting() || i.fallen);
        if (!i.mark.isEmpty()) {
            Part lit = lit(i.look, i.look.active());
            int mx = x + BossRow.TOKEN - MARK;
            int my = ty + BossRow.TOKEN - MARK;
            UiKit.draw(g, kit, lit, mx, my, MARK, MARK);
            g.drawString(font, i.mark, mx + (MARK - i.markW) / 2, my + BossBarLayout.CHIP_TEXT_Y, UiKit.labelOver(kit, lit, kit.ink()), false);
        }
    }

    private static final java.util.List<HealthRing.Rect> TRACK = HealthRing.track(BossRow.TOKEN, RING);
    /** The filled part for each length of the band, worked out once on first use: a frame only looks one up. */
    private static final java.util.List<?>[] FILLED = new java.util.List<?>[HealthRing.length(BossRow.TOKEN, RING) + 1];

    @SuppressWarnings("unchecked")
    private static java.util.List<HealthRing.Rect> filled(float progress) {
        int px = HealthRing.lengthFor(BossRow.TOKEN, RING, progress);
        if (FILLED[px] == null) {
            FILLED[px] = HealthRing.filledPx(BossRow.TOKEN, RING, px);
        }
        return (java.util.List<HealthRing.Rect>) FILLED[px];
    }

    /** The health ring: the track all round, the filled part clockwise from the top centre over it. */
    private static void ring(GuiGraphics g, HudKit kit, int x, int y, float progress, boolean gold) {
        for (HealthRing.Rect r : TRACK) {
            g.fill(x + r.x(), y + r.y(), x + r.x() + r.w(), y + r.y() + r.h(), BossFaces.ringTrack(kit));
        }
        int colour = gold ? BossFaces.ringShifting(kit) : BossFaces.ringFill(kit);
        for (HealthRing.Rect r : filled(progress)) {
            g.fill(x + r.x(), y + r.y(), x + r.x() + r.w(), y + r.y() + r.h(), colour);
        }
    }

    /** The "+N" token: a plain frame with the number of bosses it holds. */
    static void plus(GuiGraphics g, Font font, HudKit kit, String text, int textW, int x, int rowY) {
        int ty = rowY + BossRow.TOKEN_DY;
        UiKit.draw(g, kit, Part.BOSS_FRAME, x, ty, BossRow.TOKEN, BossRow.TOKEN);
        g.drawString(font, text, x + (BossRow.TOKEN - textW) / 2, ty + (BossRow.TOKEN - 8) / 2, UiKit.labelOver(kit, Part.BOSS_FRAME, BossFaces.health(kit)), false);
    }
}
