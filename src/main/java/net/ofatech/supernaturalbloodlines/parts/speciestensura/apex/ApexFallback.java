package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
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
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.jetbrains.annotations.Nullable;

/**
 * The game side of {@link ApexFallbackRules}: remembers the race a werewolf left to become {@code alpha_primeval}, sends them back to it when they stop holding
 * a spark (a rank change, or a True Alpha leaving the werewolves) and back to the apex when they hold one again. Only the player's own evolutions are
 * recorded; our own moves are not (a flag, not a guard on the player, because Tensura fires the race event inside the call).
 */
public final class ApexFallback {
    private static final String WEREWOLF_FACTION_ID = "werewolves:werewolf";
    private static boolean busy;

    private ApexFallback() {}

    public static void register() {
        RaceEvents.SET_RACE.register(ApexFallback::onSetRace);
        BloodlineAccess.addListener(ApexFallback::onBloodline);
        NeoForge.EVENT_BUS.addListener(ApexFallback::onFaction);
    }

    private static boolean isWerewolf(@Nullable IPlayableFaction<?> faction) {
        return faction != null && WEREWOLF_FACTION_ID.equals(faction.getID().toString());
    }

    @Nullable
    private static String raceOf(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    // fires before the change: the new race is the argument, the old one is still the player's
    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace,
                                         boolean flag, Changeable<Boolean> changeable, Changeable<MutableComponent> message) {
        if (!busy && newRace != null && entity instanceof ServerPlayer p && !p.level().isClientSide) {
            WolfState w = BloodlineAccess.get(p).wolf();
            String next = ApexFallbackRules.recorded(oldRace == null ? null : oldRace.getRaceId().toString(), newRace.getRaceId().toString(), w.preApex());
            if (!next.equals(w.preApex())) {
                BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().preApex(next).build()).build());
            }
        }
        return EventResult.pass();
    }

    private static void onBloodline(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        if (p.level().isClientSide || change.before().wolf().rank() == change.after().wolf().rank()) {
            return;
        }
        boolean werewolf = isWerewolf(VampirismAPI.factionPlayerHandler(p).getCurrentFaction());
        // a cure clears the wolf layer (rank included) while the player is still in the faction, so the rank alone says the spark is gone
        react(p, ApexFallbackRules.holds(change.before().wolf().rank(), werewolf), ApexFallbackRules.holds(change.after().wolf().rank(), werewolf));
    }

    private static void onFaction(PlayerFactionEvent.FactionLevelChanged e) {
        if (!(e.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide
                || isWerewolf(e.getOldFaction()) == isWerewolf(e.getCurrentFaction())) {
            return;
        }
        // a dormant True Alpha holds nothing until they rejoin; an Alpha who leaves is handled by the rank change this triggers
        var rank = BloodlineAccess.get(p).wolf().rank();
        boolean in = isWerewolf(e.getCurrentFaction());
        react(p, ApexFallbackRules.holds(rank, !in), ApexFallbackRules.holds(rank, in));
    }

    private static void react(ServerPlayer p, boolean before, boolean after) {
        WolfState w = BloodlineAccess.get(p).wolf();
        String current = raceOf(p);
        switch (ApexFallbackRules.decide(before, after, current, w.preApex())) {
            case REVERT -> {
                String to = ApexFallbackRules.target(w.preApex());
                BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().preApex(to).build()).build());
                setRace(p, to);
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.apex.fallback"));
            }
            case RESTORE -> {
                setRace(p, ApexFallbackRules.APEX);
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.apex.restored"));
            }
            case NONE -> { }
        }
    }

    private static void setRace(ServerPlayer p, String race) {
        busy = true;
        try {
            RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(race), false);
        } finally {
            busy = false;
        }
    }
}
