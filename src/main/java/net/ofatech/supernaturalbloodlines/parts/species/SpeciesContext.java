package net.ofatech.supernaturalbloodlines.parts.species;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import org.jetbrains.annotations.Nullable;

/** Builds the {@link BloodlineContext} the rules run with: the real faction, eligibility, Originals count and config. */
public final class SpeciesContext {
    private SpeciesContext() {
    }

    public static BloodlineContext of(ServerPlayer p, @Nullable UUID bloodSource) {
        BaseFaction faction = FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(p));
        return of(p, faction, p.level().getGameTime(), bloodSource);
    }

    public static BloodlineContext of(ServerPlayer p, BaseFaction faction, long gameTime, @Nullable UUID bloodSource) {
        FiveRoster roster = FiveRoster.get(p.serverLevel());
        // a potential already holds a slot, and the rite would only turn it into the mark
        int taken = roster.fiveCount() - (roster.isPotential(p.getUUID()) ? 1 : 0);
        return BloodlineContext.defaults()
                .withFaction(faction)
                .withHumanLike(SpeciesEligibility.isHumanLike(p))
                .withOriginalsInWorld(SpeciesData.get(p.server).count())
                .withMaxOriginals(SpeciesConfig.maxOriginals())
                .withFiveCount(taken)
                .withMaxFive(SpeciesConfig.maxFive())
                .withTransitionTicks(SpeciesConfig.transitionTicks())
                .withBloodSource(bloodSource)
                .withGameTime(gameTime)
                .withWerewolvesLoaded(ModList.get().isLoaded("werewolves"));
    }
}
