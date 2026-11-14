package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesEligibility;

/** Human-like for the species part: a race tagged {@code tensura:human_like} or one that evolved inside such a line. */
public final class TensuraHumanLikeCheck implements SpeciesEligibility.HumanLikeCheck {
    public static final TensuraHumanLikeCheck INSTANCE = new TensuraHumanLikeCheck();

    private static final TagKey<ManasRace> HUMAN_LIKE =
            TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", "human_like"));

    private TensuraHumanLikeCheck() {}

    /** A player with no race is not told apart from one on a server without Tensura: eligible. */
    @Override
    public boolean isHumanLike(ServerPlayer p) {
        ManasRaceInstance race = RaceAPI.getRaceFrom(p).getRace().orElse(null);
        return race == null || race.is(HUMAN_LIKE) || HumanLikeLine.isEvolvedMember(race.getRaceId().toString());
    }
}
