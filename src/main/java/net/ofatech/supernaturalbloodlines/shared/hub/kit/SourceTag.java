package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.Locale;

/** The short tag of the mod a task, skill or action comes from: SB, VMP, WW, ENC, TEN. Pure. */
public enum SourceTag {
    SB("species"), VMP("vampirism"), WW("werewolves"), ENC("enchanted"), TEN("tensura");

    private final String source;

    SourceTag(String source) {
        this.source = source;
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
