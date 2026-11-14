package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;

/** Tensura's undead and daemon races are supernatural; they are not vampiric and not prey. */
final class TensuraSupernatural {
    private TensuraSupernatural() {}

    static void register() {
        Supernatural.register(TensuraSupernatural::isUndeadOrDaemonRace, e -> false, e -> false);
    }

    private static boolean isUndeadOrDaemonRace(Entity e) {
        return e instanceof LivingEntity living && RaceTags.isUndeadOrDaemon(RaceAPI.getRaceFrom(living).getRace().orElse(null));
    }
}
