package net.ofatech.supernaturalbloodlines.shared.hud;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.FocusRing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitColors;

/**
 * The three looks of the HUD as {@link KitColors}: the kit it draws and the colours of the hub theme of the same name (a test holds each equal to the theme's own).
 * The HUD has no theme object, only a {@link HudStyle.Style}.
 */
public enum HudKit implements KitColors {
    GRIMOIRE("grimoire", 0xFFE9DCBC, 0xFF3B2414, 0xFF5E4228, 0xFF7A1218, 0xFF245A3A),
    BLOOD_MOON("bloodmoon", 0xFF1A0A10, 0xFFF0E0E0, 0xFFC9A0A8, 0xFFFFD36B, 0xFF7FE0A0),
    TENSURA("tensura", 0xFF12293F, 0xFFE6F3FB, 0xFF8FB2C4, 0xFF3FD0D6, 0xFF5EE08A);

    private final String kit;
    private final int paper;
    private final int ink;
    private final int muted;
    private final int accent;
    private final int good;

    HudKit(String kit, int paper, int ink, int muted, int accent, int good) {
        this.kit = kit;
        this.paper = paper;
        this.ink = ink;
        this.muted = muted;
        this.accent = accent;
        this.good = good;
    }

    public static HudKit of(HudStyle.Style style) {
        return switch (style) {
            case GRIMOIRE -> GRIMOIRE;
            case BLOOD_MOON -> BLOOD_MOON;
            case TENSURA -> TENSURA;
        };
    }

    @Override
    public String kit() {
        return kit;
    }

    @Override
    public int paper() {
        return paper;
    }

    @Override
    public int ink() {
        return ink;
    }

    @Override
    public int muted() {
        return muted;
    }

    @Override
    public int accent() {
        return accent;
    }

    @Override
    public int good() {
        return good;
    }

    @Override
    public int focusColor() {
        return FocusRing.forPaper(paper);
    }
}