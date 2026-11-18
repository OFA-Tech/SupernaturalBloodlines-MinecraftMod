package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.ApexFallbackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import org.jetbrains.annotations.Nullable;

/**
 * The game side of {@link ApexFallbackRules}: remembers the race a werewolf left to become {@code alpha_primeval}, sends them back to it when they stop holding
 * a spark (a rank change, or a True Alpha leaving the werewolves) and back to the apex when they hold one again. Only the player's own evolutions are
 * recorded, and only once the change really happened; our own moves are not recorded at all (a flag, not a guard on the player, because Tensura fires the race event inside the call).
 */
public final class ApexFallback {
    private static boolean busy;
    /** A race change we saw come in but cannot trust yet: another SET_RACE listener may still refuse it. Server thread only. */
    private record Pending(@Nullable String old, String now) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ApexFallback() {}

    public static void register() {
        RaceEvents.SET_RACE.register(ApexFallback::onSetRace);
        BloodlineAccess.addListener(ApexFallback::onBloodline);
        NeoForge.EVENT_BUS.addListener(ApexFallback::onFaction);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                settle(p); // a change seen this tick is recorded before they go, not dropped with the map entry
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (!PENDING.isEmpty()) {
                new ArrayList<>(PENDING.keySet()).forEach(id -> {
                    ServerPlayer p = e.getServer().getPlayerList().getPlayer(id);
                    if (p == null) {
                        PENDING.remove(id);
                    } else {
                        settle(p);
                    }
                });
            }
        });
    }

    private static boolean isWerewolf(@Nullable IPlayableFaction<?> faction) {
        return faction != null && SupernaturalRace.WEREWOLF_FACTION.equals(faction.getID().toString());
    }

    @Nullable
    private static String raceOf(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    // fires before the change: the new race is the argument, the old one is still the player's
    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace,
                                         boolean flag, Changeable<Boolean> changeable, Changeable<MutableComponent> message) {
        // SET_RACE fires before the change and a later listener can still refuse it (ManasCore's setRace returns false on a refusal), so nothing is
        // recorded here: settle() records it once the race is really the new one (the next tick, or before the next thing that reads it)
        if (!busy && newRace != null && entity instanceof ServerPlayer p && !p.level().isClientSide) {
            settle(p); // an earlier change of the same tick, before this one can replace it (a refused one would drop it)
            PENDING.put(p.getUUID(), new Pending(oldRace == null ? null : oldRace.getRaceId().toString(), newRace.getRaceId().toString()));
        }
        return EventResult.pass();
    }

    /** Records the player's own change of race, if it really happened. Public for the tests. */
    public static void settle(ServerPlayer p) {
        Pending q = PENDING.remove(p.getUUID());
        if (q == null || !q.now().equals(raceOf(p))) {
            return;
        }
        WolfState w = BloodlineAccess.get(p).wolf();
        String next = ApexFallbackRules.recorded(q.old(), q.now(), w.preApex());
        if (!next.equals(w.preApex())) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().preApex(next).build()).build());
        }
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
        settle(p);
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
