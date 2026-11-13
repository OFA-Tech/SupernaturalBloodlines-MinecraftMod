package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** What the mark means for other systems. */
public final class HunterRules {
    private HunterRules() {}

    /** Potentials and Hunters of the Five cannot be compelled. Compulsion checks this together with {@code VervainRules.protects}. */
    public static boolean immuneToCompulsion(Player p) {
        Species s = BloodlineAccess.get(p).species();
        return s == Species.POTENTIAL || s == Species.HUNTER_OF_THE_FIVE;
    }
}
