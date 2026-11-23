package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look;

/**
 * The colours the wheel fills its rings and writes its text with, read off the approved mockups (the bands, the lit wedge, the panel and its inks). Sprites carry the cells,
 * chips, panel and glow; these are only what a sprite cannot be: a ring band of any size and its lit wedge. ARGB. Pure.
 *
 * @param panel the opaque colour the side panel and chips are written on
 * @param onPill the colour of the text on the ON pill and the selected chip, and {@code pill} what it is written on
 */
public record WheelPalette(int outerBand, int outerLit, int innerBand, int innerLit, int middleTrack, int middleLit, int hub, int seal, int line, int rim, int ink, int muted,
                           int panel, int warn, int pill, int onPill, int hintInk, int hintOn) {
    /** A text and what it is written on, for the contrast test. */
    public record TextPair(String what, int fg, int bg) {
    }

    public static final WheelPalette GRIMOIRE = new WheelPalette(0xE6CFBF8F, 0xF2F1E8CC, 0xF04A1919, 0xF0E9DDB8, 0x80B7A572, 0xF0F1E8CC, 0xFF260A0A, 0xFF9A1C22, 0xFFA07532, 0xFF7A5A32,
            0xFF2A1A10, 0xFF6A5236, 0xFFE9DDB8, 0xFF7A1218, 0xFF9A1C22, 0xFFF5ECCF, 0xFFF5ECCF, 0xFF4A1717);
    public static final WheelPalette BLOOD_MOON = new WheelPalette(0xCC10060A, 0xE62E0F18, 0xCC1D0B13, 0xE62E141C, 0x66443640, 0xCC53333B, 0xFF211315, 0xFFBE4137, 0xFF5E2A38, 0xFFBE4137,
            0xFFFFF1EA, 0xFFC9A7A5, 0xFF1D0B13, 0xFFFF7A6B, 0xFFBE4137, 0xFFFFF1EA, 0xFFFFF1EA, 0xFF1D0B13);
    public static final WheelPalette TENSURA = new WheelPalette(0xCC112636, 0xE61B3C56, 0xCC12263B, 0xE61B3C56, 0x66304761, 0xCC354B62, 0xFF0A1827, 0xFF12293F, 0xFF2A5370, 0xFF3FD0D6,
            0xFFE6F3FB, 0xFF97A7B5, 0xFF12293F, 0xFFF0646A, 0xFF3FD0D6, 0xFF0A1827, 0xFFE6F3FB, 0xFF12293F);

    public static WheelPalette of(Look look) {
        return switch (look) {
            case GRIMOIRE -> GRIMOIRE;
            case BLOOD_MOON -> BLOOD_MOON;
            case TENSURA -> TENSURA;
        };
    }

    /** Every text the wheel writes and what it is on: each must read at 4.5:1. */
    public List<TextPair> textPairs() {
        return List.of(new TextPair("entry name and panel text", ink, panel), new TextPair("mode caption and slot rows", muted, panel), new TextPair("locked reason", warn, panel),
                new TextPair("ON pill and selected group label", onPill, pill), new TextPair("G hint", hintInk, hintOn));
    }
}
