package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;

/** The nine once-per-world bosses (plan 17). {@code landed} is whether the build can spawn it yet; the others only have their switch. */
public enum BossId {
    MIKAEL(true), KLAUS(true), ELIJAH(true), KOL(true), REBEKAH(true), FINN(true), DEUCALION(false), PETER_HALE(false), BEAST_OF_GEVAUDAN(false);

    private final boolean landed;

    BossId(boolean landed) {
        this.landed = landed;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean landed() {
        return landed;
    }

    /** The six vampire Originals are the NPC roots of their lines; the other three are not. */
    public Optional<NpcOriginal> npcOriginal() {
        return NpcOriginal.byId(id());
    }

    public static Optional<BossId> byId(String id) {
        return Arrays.stream(values()).filter(b -> b.id().equals(id)).findFirst();
    }
}