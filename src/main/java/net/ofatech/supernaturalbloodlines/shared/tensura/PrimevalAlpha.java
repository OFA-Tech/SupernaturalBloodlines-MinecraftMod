package net.ofatech.supernaturalbloodlines.shared.tensura;

import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;

/**
 * L-WOL-6, a True Alpha on {@code alpha_primeval}: the one case where the apex race gives more (stat bonuses x1.5, and the granted skill
 * S-WOL-8 Primeval Roar). Shared by species_tensura (the stats) and supernatural_tensura (the skill), which must not import each other.
 * Dormant, and so false, while the player is not a werewolf now (R91: a cured True Alpha keeps the rank, not the perks).
 */
public final class PrimevalAlpha {
    public static final String RACE_ID = "supernaturalbloodlines:alpha_primeval";

    private PrimevalAlpha() {
    }

    public static boolean active(Player p) {
        return BloodlineAccess.get(p).wolf().rank() == WolfState.Rank.TRUE_ALPHA
                && RaceAPI.getRaceFrom((LivingEntity) p).getRace().map(r -> RACE_ID.equals(r.getRaceId().toString())).orElse(false)
                && PlayerRaces.of(p).contains(SupernaturalRace.WEREWOLF);
    }
}
