package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.ApexRaceQuery;

/** Reads the Tensura race of a player for the migration: only races of this mod's namespace are apex races of ours. */
public final class ApexRaceReader {
    private ApexRaceReader() {
    }

    public static void register() {
        ApexRaceQuery.set(ApexRaceReader::pathOf);
    }

    static String pathOf(Player p) {
        return RaceAPI.getRaceFrom((LivingEntity) p).getRace()
                .map(r -> r.getRaceId())
                .filter(id -> id.getNamespace().equals(SupernaturalBloodlines.MODID))
                .map(id -> id.getPath())
                .orElse(null);
    }
}
