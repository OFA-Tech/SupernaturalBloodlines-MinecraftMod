package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.SightRules;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterInstinct;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Shared sight (R293): what a brother with the prey sense senses, the brothers who lack it see too: the same ids, at the sensor's own pace, range and filters
 * (cloaked prey, witches and siphoners are never in the list). Nothing else is sent: no name, no distance, no count. A client ignores an id for an entity it
 * has not loaded, so a brother far from the prey loses nothing.
 */
public final class BrotherhoodSight {
    private BrotherhoodSight() {
    }

    /** Called with the list the hunter was just sent; never recomputes it. */
    public static void share(ServerPlayer sensor, List<Integer> ids) {
        if (ids.isEmpty() || !GroupsConfig.sightShareEnabled()) {
            return;
        }
        ServerLevel level = sensor.serverLevel();
        BrotherhoodScope.Brotherhood b = BrotherhoodScope.of(level, sensor.getUUID());
        if (b == null || !SightRules.gives(BrotherhoodScope.appliesTo(level, b.leader(), sensor), Codes.isBreaker(sensor.server, sensor.getUUID()), HunterInstinct.hasSense(sensor))) {
            return;
        }
        int radius = GroupsConfig.sightRadius();
        for (UUID id : PackIndex.get(level).roster(b.leader())) {
            ServerPlayer mate = id.equals(sensor.getUUID()) ? null : SireCall.player(sensor.server, id);
            if (mate != null && SightRules.receives(BrotherhoodScope.appliesTo(level, b.leader(), mate), Codes.isBreaker(sensor.server, id), HunterInstinct.hasSense(mate), true,
                    mate.level() == sensor.level(), mate.distanceToSqr(sensor), radius)) {
                List<Integer> mine = SightRules.idsFor(ids, mate.getId());
                if (!mine.isEmpty()) {
                    GlowPayload.send(mate, mine);
                }
            }
        }
    }
}