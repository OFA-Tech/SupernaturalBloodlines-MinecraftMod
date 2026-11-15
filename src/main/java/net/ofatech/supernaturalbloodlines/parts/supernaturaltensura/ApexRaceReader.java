package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.shared.supernatural.ApexRaceQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;
import net.ofatech.supernaturalbloodlines.shared.tensura.ApexLines;

/** Reads the Tensura race of a player for the migration and the {@code tensura_tier} detector: only races of this mod's namespace are apex races of ours. */
public final class ApexRaceReader {
    private ApexRaceReader() {
    }

    public static void register() {
        ApexRaceQuery.set(ApexRaceReader::pathOf);
        ApexRaceQuery.setTier(ApexRaceReader::atApexTier);
    }

    static String pathOf(Player p) {
        return RaceAPI.getRaceFrom((LivingEntity) p).getRace()
                .map(r -> r.getRaceId())
                .filter(id -> id.getNamespace().equals(SupernaturalBloodlines.MODID))
                .map(id -> id.getPath())
                .orElse(null);
    }

    static boolean atApexTier(Player p, SupernaturalRace race) {
        return RaceAPI.getRaceFrom((LivingEntity) p).getRace()
                .map(r -> ApexLines.atApexTier(race, r.getRaceId().toString(), PlayerRaces.of(p)))
                .orElse(false);
    }
}
