package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/** Tensura's undead and daemon races are supernatural; they are not vampiric and not prey. */
final class TensuraSupernatural {
    private static final TagKey<ManasRace> UNDEAD = raceTag("undead");
    private static final TagKey<ManasRace> DAEMON = raceTag("daemon");

    private TensuraSupernatural() {}

    static void register() {
        Supernatural.register(TensuraSupernatural::isUndeadOrDaemonRace, e -> false, e -> false);
    }

    private static TagKey<ManasRace> raceTag(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    private static boolean isUndeadOrDaemonRace(Entity e) {
        return e instanceof LivingEntity living
                && RaceAPI.getRaceFrom(living).getRace().map(r -> r.is(UNDEAD) || r.is(DAEMON)).orElse(false);
    }
}
