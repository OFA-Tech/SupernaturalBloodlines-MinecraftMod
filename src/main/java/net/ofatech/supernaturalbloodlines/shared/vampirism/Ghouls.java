package net.ofatech.supernaturalbloodlines.shared.vampirism;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;

/** Whether a player is a Ghoul (plan 11), for parts that must not import each other. Only the species part ever sets the flag. */
public final class Ghouls {
    private Ghouls() {
    }

    /** A vampire who is still a Ghoul: the flag only counts while the player is in the vampire faction. */
    public static boolean isGhoul(Player p) {
        return BloodlineAccess.get(p).ghoul() && inVampireFaction(p);
    }

    public static boolean inVampireFaction(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION;
    }
}
