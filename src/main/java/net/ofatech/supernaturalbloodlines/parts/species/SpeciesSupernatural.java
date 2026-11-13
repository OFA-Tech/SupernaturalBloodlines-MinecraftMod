package net.ofatech.supernaturalbloodlines.parts.species;

import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** What species tells {@link Supernatural}: every species except the human-side ones is supernatural; witches and siphoners are never prey (R81). */
public final class SpeciesSupernatural {
    private SpeciesSupernatural() {
    }

    public static void register() {
        Supernatural.register(e -> e instanceof Player p && isSupernatural(BloodlineAccess.get(p).species()),
                e -> e instanceof Player p && isVampiric(BloodlineAccess.get(p).species()),
                e -> e instanceof Player p && isVampiric(BloodlineAccess.get(p).species()));
    }

    static boolean isSupernatural(Species s) {
        return s != Species.NONE && s != Species.POTENTIAL && s != Species.HUNTER_OF_THE_FIVE;
    }

    /** Vampire traits: also the species that are prey (vampire and werewolf types). */
    static boolean isVampiric(Species s) {
        return switch (s) {
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID, HERETIC -> true;
            default -> false;
        };
    }
}
