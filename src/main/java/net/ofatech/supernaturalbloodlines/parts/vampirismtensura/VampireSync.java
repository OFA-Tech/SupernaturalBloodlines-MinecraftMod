package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import java.util.OptionalInt;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.ReentrancyGuard;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * Tensura vampire line -> Vampirism level floor, and a cure -> Tensura race reset.
 * Joining Vampirism never changes the Tensura race (R20).
 */
final class VampireSync {
    private static final String CHANNEL = "vampire_sync";

    private VampireSync() {}

    static void register() {
        RaceEvents.SET_RACE.register(VampireSync::onSetRace);
        NeoForge.EVENT_BUS.addListener(VampireSync::onFactionLevelChanged);
    }

    // SET_RACE fires before the change (old, entity, new); the new race id is what counts here.
    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace,
                                         boolean flag, Changeable<Boolean> changeable, Changeable<MutableComponent> message) {
        if (newRace != null && entity instanceof ServerPlayer p && !p.level().isClientSide && isSyncable(p)) {
            ReentrancyGuard.run(p.getUUID(), CHANNEL, () -> raiseToRaceFloor(p, newRace.getRaceId().toString()));
        }
        return EventResult.pass();
    }

    private static void raiseToRaceFloor(ServerPlayer p, String raceId) {
        IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(p);
        IPlayableFaction<?> vampire = VReference.VAMPIRE_FACTION;
        boolean isVampire = handler.getCurrentFaction() == vampire;
        int current = isVampire ? handler.getCurrentLevel() : 0;
        OptionalInt level = VampireSyncConfig.mapping(vampire.getHighestReachableLevel()).levelForRace(raceId, current);
        if (level.isEmpty() || (!isVampire && !handler.canJoin(vampire))) {
            return;
        }
        if (!isVampire || level.getAsInt() != current) {
            handler.setFactionAndLevel(vampire, level.getAsInt());
        }
    }

    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (!(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide || !isSyncable(p)
                || event.getOldFaction() != VReference.VAMPIRE_FACTION || event.getCurrentFaction() == VReference.VAMPIRE_FACTION) {
            return;
        }
        ReentrancyGuard.run(p.getUUID(), CHANNEL, () -> {
            String current = RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
            VampireSyncConfig.mapping(VReference.VAMPIRE_FACTION.getHighestReachableLevel()).raceOnCure(current)
                    .ifPresent(cure -> RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(cure), false));
        });
    }

    private static boolean isSyncable(ServerPlayer p) {
        return BloodlineAccess.get(p).species() == Species.NONE;
    }
}
