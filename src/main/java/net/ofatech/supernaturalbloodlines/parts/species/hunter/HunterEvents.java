package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.VisibleForTesting;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/** Keeps the {@link FiveRoster} in step with the species, and frees the slot of a Hunter of the Five who leaves the hunters. */
public final class HunterEvents {
    private static volatile Function<MinecraftServer, List<ServerPlayer>> onlineOverride;

    private HunterEvents() {}

    /** Everyone who counts as online for choosing potentials and lifting curses. */
    public static List<ServerPlayer> online(MinecraftServer server) {
        Function<MinecraftServer, List<ServerPlayer>> pinned = onlineOverride;
        return pinned != null ? pinned.apply(server) : server.getPlayerList().getPlayers();
    }

    /** For tests: mock players are not in the player list; this says who is online. Null clears it. */
    @VisibleForTesting
    public static void overrideOnline(Function<MinecraftServer, List<ServerPlayer>> value) {
        onlineOverride = value;
    }

    public static void register() {
        MarkTracker.register();
        PotentialChooser.register();
        HunterCurse.register();
        HunterInstinct.register();
        // every way the species changes (rite, debug command, rules) goes through BloodlineAccess.set
        BloodlineAccess.addListener((p, change) -> {
            setMember(p, change.after().species());
            startClock(p, change.before().species(), change.after().species());
        });
        NeoForge.EVENT_BUS.addListener(HunterEvents::onFactionLevelChanged);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
            }
        });
    }

    /** Idempotent: heals the roster if it ever missed a change (lost file, state set while the part was off). */
    public static void sync(ServerPlayer p) {
        setMember(p, BloodlineAccess.get(p).species());
    }

    private static void setMember(ServerPlayer p, Species species) {
        FiveRoster roster = FiveRoster.get(p.serverLevel());
        if (species == Species.HUNTER_OF_THE_FIVE) {
            roster.add(p.getUUID());
        } else {
            roster.remove(p.getUUID());
        }
        roster.setPotential(p.getUUID(), species == Species.POTENTIAL);
    }

    /** Whatever way the mark is gained (rite, debug command), the instinct clock (R84) starts then if nothing set it. */
    private static void startClock(ServerPlayer p, Species before, Species after) {
        BloodlineState s = BloodlineAccess.get(p);
        if (after == Species.HUNTER_OF_THE_FIVE && before != after && s.species() == after && s.lastVampireKill() == 0) {
            BloodlineAccess.set(p, s.toBuilder().lastVampireKill(p.level().getGameTime()).build());
        }
    }

    /** Leaving the hunter faction ends the mark: the rules (LEFT_BASE_FACTION) turn HUNTER_OF_THE_FIVE into NONE. */
    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (event.getOldFaction() != VReference.HUNTER_FACTION || event.getCurrentFaction() == VReference.HUNTER_FACTION
                || !(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        boolean wasPotential = BloodlineAccess.get(p).species() == Species.POTENTIAL;
        BloodlineAccess.applyEvent(p, BloodlineEvent.LEFT_BASE_FACTION,
                SpeciesContext.of(p, BaseFaction.HUNTER, p.level().getGameTime(), null));
        if (wasPotential && BloodlineAccess.get(p).species() == Species.NONE) {
            PotentialChooser.owe(p.server, p.getUUID()); // the world still owes the Five a potential
        }
    }
}
