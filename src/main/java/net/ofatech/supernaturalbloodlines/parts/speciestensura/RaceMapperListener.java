package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Plan 23 Task 10: listens on SET_RACE and delegates to {@link RaceMapper} only when the reincarnation
 * menu is open (a player's own pick). Admin commands, evolutions and our own writes have the menu closed
 * and are ignored.
 */
final class RaceMapperListener {
    private RaceMapperListener() {}

    static void register() {
        RaceEvents.SET_RACE.register(RaceMapperListener::onSetRace);
    }

    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace,
                                         boolean evolution, Changeable<Boolean> changeable, Changeable<MutableComponent> message) {
        if (evolution || newRace == null || !(entity instanceof ServerPlayer p) || p.level().isClientSide) {
            return EventResult.pass();
        }
        if (!(p.containerMenu instanceof ReincarnationMenu)) {
            return EventResult.pass();
        }
        String raceId = newRace.getRaceId().toString();
        SupernaturalBloodlines.LOGGER.debug("race_mapper: pick {} by {}", raceId, p.getName().getString());
        RaceMapper.applyPick(p, raceId);
        return EventResult.pass();
    }
}