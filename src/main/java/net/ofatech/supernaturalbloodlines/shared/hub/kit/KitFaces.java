package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;

/**
 * The colour of the middle of each kit button, so the label drawn on it can be checked (4.5:1) without reading a texture at run time. {@code KitFacesTest}
 * compares every entry with the PNG, so a redrawn button that changes the colour fails the build. Pure.
 */
public final class KitFaces {
    // normal, hover, pressed, disabled; then the same four of the primary button
    private static final int[][] FACES = {
            {0xFFD8C896, 0xFFEADFAE, 0xFFC6B27C, 0xFFD5CBB0, 0xFF8E1C22, 0xFFAE2A30, 0xFF741519, 0xFFCFC4A6},
            {0xFF22111A, 0xFF2E1620, 0xFF10060A, 0xFF170A10, 0xFFBF332B, 0xFFD9453B, 0xFF8E2420, 0xFF22121A},
            {0xFF12293F, 0xFF17374E, 0xFF0F2338, 0xFF10243A, 0xFF3FD0D6, 0xFF68E4EA, 0xFF1F6A74, 0xFF172C3F}};

    private KitFaces() {
    }

    /** The button parts a face is known for: normal, hover, pressed and disabled of the plain and the primary button. */
    public static final Part[] BUTTONS = {Part.BUTTON_NORMAL, Part.BUTTON_HOVER, Part.BUTTON_PRESSED, Part.BUTTON_DISABLED,
            Part.BUTTON_PRIMARY_NORMAL, Part.BUTTON_PRIMARY_HOVER, Part.BUTTON_PRIMARY_PRESSED, Part.BUTTON_PRIMARY_DISABLED};

    /** The other surfaces text stands on: a chip, a lit chip, a plate and an inset (a text box). */
    public static final Part[] SURFACES = {Part.CHIP, Part.CHIP_ON, Part.PLATE, Part.INSET, Part.CHIP_SHIFTING, Part.BOSS_FRAME, Part.BOSS_NAME_PLATE};
    private static final int[][] SURFACE_FACES = {
            {0xFFE2D6B0, 0xFF9A1C22, 0xFFE9DDB8, 0xFFDCCD9E, 0xFFB8903A, 0xFFE9DDB8, 0xFF4A1717},
            {0xFF1D0B13, 0xFFBF332B, 0xFF1D0B13, 0xFF10060A, 0xFFFFD36B, 0xFF1D0B13, 0xFF1D0B13},
            {0xFF17374E, 0xFF3FD0D6, 0xFF12293F, 0xFF0A1827, 0xFFE8B33A, 0xFF12293F, 0xFF12293F}};

    public static int face(String kit, Part button) {
        int k = Math.max(0, KitSprites.KITS.indexOf(kit));
        for (int i = 0; i < BUTTONS.length; i++) {
            if (BUTTONS[i] == button) {
                return FACES[k][i];
            }
        }
        for (int i = 0; i < SURFACES.length; i++) {
            if (SURFACES[i] == button) {
                return SURFACE_FACES[k][i];
            }
        }
        throw new IllegalArgumentException(button + " is not a button or a surface");
    }

    /** Whether the table above has a face for {@code part} (a button or a surface); a label on any other part is not checked against a face. */
    public static boolean knows(Part part) {
        return java.util.Arrays.asList(BUTTONS).contains(part) || java.util.Arrays.asList(SURFACES).contains(part);
    }

    /**
     * The face to read a label on. The table holds the middle of the shipped art; a resource pack that repaints a sprite changes the middle, and the label must
     * read on what is really drawn: the picture's own solid middle (read from the pack's PNG) when it has one and is opaque, else the table.
     */
    public static int resolveFace(int tableFace, @org.jetbrains.annotations.Nullable Integer pictureMiddle) {
        return pictureMiddle != null && (pictureMiddle >>> 24) >= 0xC0 ? pictureMiddle | 0xFF000000 : tableFace;
    }

    /** The label on a button: the theme's ink when it reads on the face (4.5:1), else white or black, whichever does. */
    public static int label(String kit, Part button, int ink) {
        return labelOn(face(kit, button), ink);
    }

    /** The label on any face: {@code ink} when it reads (4.5:1), else white or black. */
    public static int labelOn(int face, int ink) {
        return Contrast.ratio(ink, face) >= 4.5 ? ink : Contrast.bestText(face);
    }

    /**
     * The colour a part is when its sprite is missing and the theme's flat colours are drawn instead ({@code UiKit#flat}): a surface is the paper, an accent part the
     * accent, a track the muted colour at half alpha over the paper.
     */
    public static int flatFace(KitSprites.Flat flat, int paper, int accent, int muted) {
        return switch (flat) {
            case ACCENT -> 0xFF000000 | accent;
            case TRACK -> blend(muted, paper, 0x80);
            default -> 0xFF000000 | paper;
        };
    }

    private static int blend(int over, int under, int alpha) {
        int out = 0xFF000000;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int o = (over >> shift) & 0xFF;
            int u = (under >> shift) & 0xFF;
            out |= ((o * alpha + u * (255 - alpha)) / 255) << shift;
        }
        return out;
    }

    /** The button part of a state: primary or plain, enabled, hot (hovered or focused) and pressed. */
    public static Part button(boolean primary, boolean enabled, boolean hot, boolean pressed) {
        int base = primary ? 4 : 0;
        int state = !enabled ? 3 : pressed ? 2 : hot ? 1 : 0;
        return BUTTONS[base + state];
    }
}
