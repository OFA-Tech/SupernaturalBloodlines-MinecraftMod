package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesTensuraMixinPlugin;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.HumanLikeLine;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;

/**
 * Called from {@code mixin.speciestensura.RaceInstanceMixin} at the end of {@code ManasRaceInstance.getNextEvolutions}:
 * the evolution screen's list, "set tracked" and the server's {@code contains} check all go through it, so adding the apex
 * race here is enough for the player to pick it (docs/parts/species_tensura.md). Switch: {@code apex_evolutions_enabled}.
 */
public final class ApexOffer {
    private static volatile boolean failureLogged;

    private ApexOffer() {}

    /** The apex race a player with this species is offered from this race, if any (saint tier or higher of a human-like line). */
    public static Optional<String> apexIdFor(Species species, String currentRaceId) {
        if (!HumanLikeLine.isSaintOrHigher(currentRaceId)) {
            return Optional.empty();
        }
        return ApexSpec.forSpecies(species).map(ApexSpec::id);
    }

    /** The mixin's answer: the race's own list plus the apex race (a mutable copy; Tensura's lists are immutable). */
    public static List<ManasRace> withApex(ManasRaceInstance instance, LivingEntity entity, List<ManasRace> original) {
        if (!(entity instanceof Player player) || !SpeciesTensuraConfig.apexEnabled()) {
            return original;
        }
        try {
            Optional<String> id = apexIdFor(BloodlineAccess.get(player).species(), instance.getRaceId().toString());
            ManasRace apex = id.map(i -> RaceAPI.getRaceRegistry().get(ResourceLocation.fromNamespaceAndPath(ApexSpec.RACE_NAMESPACE, i))).orElse(null);
            if (apex == null || original.contains(apex)) {
                return original;
            }
            List<ManasRace> out = new ArrayList<>(original);
            out.add(apex);
            return out;
        } catch (RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                SupernaturalBloodlines.LOGGER.warn("species_tensura: the apex evolution offer failed, no apex option is added", e);
            }
            return original;
        }
    }

    /** The mixin is on {@code ManasRaceInstance}: false when it was not applied (a ManasCore update, or the part is off). */
    public static boolean hookApplied() {
        return ManasRaceInstance.class != null && SpeciesTensuraMixinPlugin.apexHooked();
    }
}
