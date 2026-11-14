package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.shared.vampirism.Ghouls;

/**
 * The Tensura side of the Ghoul (plan 11, R20e). A player whose race is exactly {@code tensura:human} shows {@code tensura:ghoul}
 * while they are a Ghoul and {@code tensura:vampire} once the turning is done; every other race is kept. While a Ghoul, no Tensura
 * evolution goes through.
 *
 * Evolutions: {@code RaceHelper.evolveRace} ends in {@code Races.evolveRace}, which calls {@code RaceStorage.setRace(race, true, false, msg)}
 * and that posts ManasCore's {@code SET_RACE} with that first boolean as the evolution flag; an {@code EventResult.isFalse()} makes
 * {@code setRace} return before anything changes. So this needs no mixin. Only evolutions are refused: our own race changes, a cure
 * (Tensura race back to human) and an admin's race command are not evolutions.
 */
final class GhoulRace {
    static final String HUMAN = "tensura:human";
    static final String GHOUL = "tensura:ghoul";
    static final String VAMPIRE = "tensura:vampire";

    private GhoulRace() {
    }

    static void register() {
        RaceEvents.SET_RACE.register(GhoulRace::onSetRace);
        BloodlineAccess.addListener(GhoulRace::onBloodlineChange);
    }

    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace, boolean evolution,
                                         Changeable<Boolean> teleport, Changeable<MutableComponent> message) {
        if (evolution && newRace != null && entity instanceof ServerPlayer p && !p.level().isClientSide && Ghouls.isGhoul(p)) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.ghoul_no_evolve"));
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    private static void onBloodlineChange(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        boolean was = change.before().ghoul();
        boolean is = change.after().ghoul();
        if (was == is) {
            return;
        }
        String race = RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
        if (is && HUMAN.equals(race)) {
            setRace(p, GHOUL);
        } else if (!is && GHOUL.equals(race) && Ghouls.inVampireFaction(p)) {
            // the turning is done (a Ghoul who left the vampires is not here: the cure sets the race back to human)
            setRace(p, VAMPIRE);
        }
    }

    private static void setRace(ServerPlayer p, String race) {
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(race), false);
    }
}
