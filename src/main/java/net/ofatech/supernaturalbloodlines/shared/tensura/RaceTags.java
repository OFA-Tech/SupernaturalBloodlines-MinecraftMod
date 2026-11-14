package net.ofatech.supernaturalbloodlines.shared.tensura;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/** Tensura's race tags (ManasCore race tags, not entity-type tags) that more than one part asks about. */
public final class RaceTags {
    public static final TagKey<ManasRace> UNDEAD = of("undead");
    public static final TagKey<ManasRace> DAEMON = of("daemon");

    /** Ours: races whose players can be bitten and fed on even when Tensura's tags call them bloodless (the apex races). */
    public static final TagKey<ManasRace> BITABLE = TagKey.create(RaceAPI.getRaceRegistryKey(),
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bitable"));

    private RaceTags() {}

    private static TagKey<ManasRace> of(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    public static boolean isBitable(@Nullable ManasRaceInstance race) {
        return race != null && race.is(BITABLE);
    }

    public static boolean isUndeadOrDaemon(@Nullable ManasRaceInstance race) {
        return race != null && (race.is(UNDEAD) || race.is(DAEMON));
    }
}
