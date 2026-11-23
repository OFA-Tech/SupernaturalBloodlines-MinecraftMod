package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import org.jetbrains.annotations.Nullable;

/**
 * Which of the 15 portraits ({@code textures/gui/boss/<name>.png}, 24 x 24) a bar shows. Klaus has one for each of his three forms; Deucalion and Peter have a human
 * and a wolf one and no portrait of the shift, so while they shift the card shows the human one on the left of a seam and the wolf one on its right; the Beast has
 * one for each stage; the other five bosses have one. Pure.
 */
public final class BossPortraits {
    /** {@code wolf} is the second picture of a seam, null for a plain portrait. */
    public record Portrait(String main, @Nullable String wolf) {
        public boolean seam() {
            return wolf != null;
        }
    }

    private BossPortraits() {
    }

    public static Portrait of(BossId boss, BossBarForms.Look look) {
        int form = look.active();
        return switch (boss) {
            case KLAUS -> new Portrait(form == 0 ? "klaus_human" : form == 1 ? "klaus_shifting" : "klaus_wolf", null);
            case DEUCALION -> shifter("deucalion_human", "deucalion_demon_wolf", form);
            case PETER_HALE -> shifter("peter_human", "peter_wolf", form);
            case BEAST_OF_GEVAUDAN -> new Portrait(form == 0 ? "beast_host" : form == 1 ? "beast_beast" : "beast_spirit", null);
            default -> new Portrait(boss.id(), null);
        };
    }

    private static Portrait shifter(String human, String wolf, int form) {
        return form == 0 ? new Portrait(human, null) : form == 1 ? new Portrait(human, wolf) : new Portrait(wolf, null);
    }

    /** The x (of 24) where the seam of a shift runs, and its width in pixels. */
    public static final int SEAM_X = 12;
}
