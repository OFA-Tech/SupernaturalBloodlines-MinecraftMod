package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesTensuraMixinPlugin;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;
import net.ofatech.supernaturalbloodlines.shared.tensura.ApexLines;

/**
 * Called from {@code mixin.speciestensura.RaceInstanceMixin} at the end of {@code ManasRaceInstance.getNextEvolutions}:
 * the evolution screen's list, "set tracked" and the server's {@code contains} check all go through it, so adding the apex
 * race here is enough for the player to pick it (docs/parts/species_tensura.md). Switch: {@code apex_evolutions_enabled}.
 */
public final class ApexOffer {
    private static volatile boolean failureLogged;

    private ApexOffer() {}

    /**
     * The apex races offered from this Tensura race, in the order of the player's races (creature, then magic): the saint tier or higher of a
     * human-like line, the lord tier of the vampire line or the spirit beast tier of Beastfolk, for the races {@link ApexLines} allows on that
     * line, and only once the player has reached the apex rung of that race's ladder ({@code rungReached}; always true without the supernatural part).
     * A Potential is not a hunter of the Five: only the species gets the hunters' apex.
     */
    public static List<String> apexIdsFor(Species species, List<SupernaturalRace> races, String currentRaceId, Predicate<SupernaturalRace> rungReached) {
        List<String> out = new ArrayList<>();
        for (SupernaturalRace race : races) {
            if (race == SupernaturalRace.HUNTER && species != Species.HUNTER_OF_THE_FIVE) {
                continue;
            }
            ApexSpec.forRace(race).filter(spec -> ApexLines.qualifies(race, currentRaceId) && rungReached.test(race))
                    .map(ApexSpec::id).filter(id -> !out.contains(id)).ifPresent(out::add);
        }
        return out;
    }

    /** The mixin's answer: the race's own list plus the apex races (a mutable copy; Tensura's lists are immutable). */
    public static List<ManasRace> withApex(ManasRaceInstance instance, LivingEntity entity, List<ManasRace> original) {
        if (!(entity instanceof Player player) || !SpeciesTensuraConfig.apexEnabled()) {
            return original;
        }
        try {
            List<String> ids = apexIdsFor(BloodlineAccess.get(player).species(), PlayerRaces.of(player), instance.getRaceId().toString(),
                    race -> LadderQuery.apexRungReached(player, race));
            List<ManasRace> out = null;
            for (String id : ids) {
                ManasRace apex = RaceAPI.getRaceRegistry().get(ResourceLocation.fromNamespaceAndPath(ApexSpec.RACE_NAMESPACE, id));
                if (apex != null && !original.contains(apex)) {
                    out = out == null ? new ArrayList<>(original) : out;
                    out.add(apex);
                }
            }
            return out == null ? original : out;
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
        // the class literal loads ManasRaceInstance, and loading it is what applies the mixin: the flag is only set after that
        return ManasRaceInstance.class != null && SpeciesTensuraMixinPlugin.apexHooked();
    }
}
