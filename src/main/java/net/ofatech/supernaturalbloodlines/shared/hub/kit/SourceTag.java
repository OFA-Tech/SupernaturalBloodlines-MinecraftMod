package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.Locale;

/** The short tag of the mod a task, skill or action comes from: SB, VMP, WW, ENC, TEN. Pure. */
public enum SourceTag {
    SB("species"), VMP("vampirism"), WW("werewolves"), ENC("enchanted"), TEN("tensura");

    private final String source;

    SourceTag(String source) {
        this.source = source;
    }

    /** The chip is {@code HEIGHT} tall; its picture has a 4 px edge, so the mod's colour is a 2 px stripe just inside the left edge and the label starts after it. */
    public static final int HEIGHT = 10;
    public static final int STRIPE_X = 4;
    public static final int STRIPE_Y = 3;
    public static final int STRIPE_W = 2;
    public static final int STRIPE_H = HEIGHT - 2 * STRIPE_Y;
    public static final int TEXT_X = 8;

    /** The width of the chip for a label {@code labelWidth} wide: the label after the stripe, and the picture's right edge. */
    public static int width(int labelWidth) {
        return TEXT_X + labelWidth + 4;
    }

    /** The source id {@code HubTheme.sourceColor} knows. */
    public String source() {
        return source;
    }

    public String label() {
        return name();
    }

    /** The tag of a mod id or a source name; ours (SB) for our own parts and anything unknown. */
    public static SourceTag of(String modOrSource) {
        if (modOrSource == null) {
            return SB;
        }
        return switch (modOrSource.toLowerCase(Locale.ROOT)) {
            case "vampirism" -> VMP;
            case "werewolves" -> WW;
            case "enchanted" -> ENC;
            case "tensura" -> TEN;
            default -> SB;
        };
    }
}
