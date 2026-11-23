package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;

/**
 * Draws one card of the kit: the frame, the portrait under its plate, the name plate, the health bar with its ticks, the health in numbers and the chips. Nothing is
 * allocated or measured here; {@link BossBarInfo#prepare} has worked it all out. Client only.
 */
final class BossBarView {
    private static final int SEAM_COLOUR = 0xFFE8A0;

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

    static void draw(GuiGraphics g, Font font, HudKit kit, BossBarInfo i, int x, int y, long tick, double now, boolean reduce, float progress) {
        BossBarLayout.Geometry geo = i.geo;
        boolean shifting = i.look.shifting();
        UiKit.draw(g, kit, Part.BOSS_FRAME, x, y, geo.cardW(), BossBarLayout.CARD_H);

        // the portrait, then its plate over it (the plate's window is clear)
        if (i.portrait != null) {
            int ax = x + geo.artX();
            int ay = y + geo.artY();
            if (i.portrait.seam()) {
                BossPortraitTex.drawSeam(g, i.portrait.main(), i.portrait.wolf(), ax, ay, BossPortraits.SEAM_X);
                int flash = BossBarMotion.seamPercent(tick, reduce);
                if (flash > 0) {
                    g.fill(ax + BossPortraits.SEAM_X, ay, ax + BossPortraits.SEAM_X + 1, ay + BossBarLayout.ART, (flash * 255 / 100) << 24 | SEAM_COLOUR);
                }
            } else {
                BossPortraitTex.draw(g, i.portrait.main(), ax, ay);
            }
        }
        UiKit.draw(g, kit, plate(i.look), x + geo.plateX(), y + geo.plateY(), BossBarLayout.PLATE, BossBarLayout.PLATE);

        // the name
        UiKit.draw(g, kit, Part.BOSS_NAME_PLATE, x + geo.nameX(), y + geo.nameY(), geo.nameW(), BossBarLayout.NAME_H);
        g.drawString(font, i.nameShown, x + geo.nameX() + BossBarLayout.NAME_TEXT_X, y + geo.nameY() + BossBarLayout.NAME_TEXT_Y, BossFaces.name(kit), false);

        // the health: the track, the fill (gold while the boss can be hit), the ticks over it
        UiKit.draw(g, kit, Part.BOSS_BAR_TRACK, x + geo.trackX(), y + geo.trackY(), geo.trackW(), BossBarLayout.TRACK_H);
        int fill = BossBarLayout.fillWidth(geo.fillMaxW(), progress);
        if (fill > 0) {
            boolean gold = shifting || i.fallen;
            float alpha = shifting ? BossBarMotion.shiftFillPercent(tick, reduce) / 100f : 1f;
            UiKit.draw(g, kit, gold ? Part.BOSS_BAR_FILL_SHIFTING : Part.BOSS_BAR_FILL, x + geo.fillX(), y + geo.fillY(), fill, BossBarLayout.FILL_H, alpha);
        }
        for (int tx : geo.tickX()) {
            UiKit.draw(g, kit, Part.BOSS_BAR_TICK, x + tx, y + geo.fillY(), 1, BossBarLayout.FILL_H);
        }
        if (!i.hpShown.isEmpty()) {
            g.drawString(font, i.hpShown, x + geo.hpX(), y + geo.hpY(), BossFaces.health(kit), false);
        }

        chips(g, font, kit, i, x, y, now, reduce);
    }

    private static void chips(GuiGraphics g, Font font, HudKit kit, BossBarInfo i, int x, int y, double now, boolean reduce) {
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
            UiKit.draw(g, kit, Part.CHIP, cx, cy, w, BossBarLayout.CHIP_H);
            float alpha = on ? fade : c == i.prevActive ? 1f - fade : 0f;
            if (alpha >= 0.999f) {
                UiKit.draw(g, kit, lit, cx, cy, w, BossBarLayout.CHIP_H);
            } else if (alpha > 0.001f) {
                UiKit.draw(g, kit, lit, cx, cy, w, BossBarLayout.CHIP_H, alpha);
            }
            boolean showsLit = alpha >= 0.5f;
            int text = BossFaces.chip(kit, showsLit ? lit : Part.CHIP);
            g.drawString(font, i.labels[c], cx + (w - i.chipTextW[c]) / 2, cy + BossBarLayout.CHIP_TEXT_Y, text, false);
        }
    }
}
