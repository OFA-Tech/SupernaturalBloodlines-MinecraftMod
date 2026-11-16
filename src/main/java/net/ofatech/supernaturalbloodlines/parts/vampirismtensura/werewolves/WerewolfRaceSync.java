package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.ReentrancyGuard;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.attachment.RaceMemory;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSyncConfig;
import org.jetbrains.annotations.Nullable;

/**
 * R20b: werewolves follow Tensura's Beastfolk line (beastfolk, beast_lord, spirit_beast; divine_beast stays a plain Tensura
 * evolution). An exact {@code tensura:human} who becomes a werewolf is made a beastfolk; an Alpha already on the line is
 * a beast_lord, a True Alpha a spirit_beast. The race is never lowered and any other race is never touched. Registered
 * by the part only when Werewolves is loaded; the faction is matched by id, so this class links no Werewolves class.
 */
public final class WerewolfRaceSync {
    private static final String CHANNEL = "werewolf_race_sync";
    private static final String HUMAN = "tensura:human";
    private static final String BEASTFOLK = "tensura:beastfolk";
    private static final String BEAST_LORD = "tensura:beast_lord";
    private static final String SPIRIT_BEAST = "tensura:spirit_beast";

    private WerewolfRaceSync() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WerewolfRaceSync::onFactionLevelChanged);
        BloodlineAccess.addListener((p, change) -> {
            if (change.before().wolf().rank() != change.after().wolf().rank()) {
                sync(p, false);
            }
        });
    }

    /** The race to set, or empty to keep the current one. Pure. */
    public static Optional<String> targetRace(@Nullable String current, Rank rank, boolean joining) {
        String race = current;
        if (joining && HUMAN.equals(race)) {
            race = BEASTFOLK;
        }
        if (BEASTFOLK.equals(race) && rank == Rank.ALPHA) {
            race = BEAST_LORD;
        }
        if ((BEASTFOLK.equals(race) || BEAST_LORD.equals(race)) && rank == Rank.TRUE_ALPHA) {
            race = SPIRIT_BEAST;
        }
        return race == null || race.equals(current) ? Optional.empty() : Optional.of(race);
    }

    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        if (!(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide) {
            return;
        }
        if (isWerewolf(event.getCurrentFaction()) && !isWerewolf(event.getOldFaction())) {
            sync(p, true);
        } else if (isWerewolf(event.getOldFaction()) && !isWerewolf(event.getCurrentFaction())) {
            restore(p);
        }
    }

    private static boolean isWerewolf(@Nullable IPlayableFaction<?> faction) {
        return faction != null && FactionRaceRules.WEREWOLF_FACTION_ID.equals(faction.getID());
    }

    private static void sync(ServerPlayer p, boolean joining) {
        // a species player's race belongs to the species layer; a player outside the werewolves keeps theirs
        if (p.level().isClientSide || !VampireSyncConfig.werewolfBeastfolkEnabled() || BloodlineAccess.get(p).species() != Species.NONE
                || !isWerewolf(VampirismAPI.factionPlayerHandler(p).getCurrentFaction())) {
            return;
        }
        ReentrancyGuard.run(p.getUUID(), CHANNEL, () -> {
            String current = RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
            targetRace(current, BloodlineAccess.get(p).wolf().rank(), joining).ifPresent(race -> {
                p.setData(ModAttachments.RACE_MEMORY, p.getData(ModAttachments.RACE_MEMORY).afterChange(current, race));
                RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(race), false);
            });
        });
    }

    /**
     * A cure or any other way out of the werewolves: the race this mod set goes back to what it replaced, the way {@code VampireSync} sends a cured
     * vampire back to human. A race the player evolved to themselves stays. The memory is spent either way.
     */
    private static void restore(ServerPlayer p) {
        if (BloodlineAccess.get(p).species() != Species.NONE) {
            return;
        }
        ReentrancyGuard.run(p.getUUID(), CHANNEL, () -> {
            // spent inside the guard: a skipped (re-entered) restore must keep the memory for the one that runs
            RaceMemory memory = p.getData(ModAttachments.RACE_MEMORY);
            p.setData(ModAttachments.RACE_MEMORY, RaceMemory.NONE);
            String current = RaceAPI.getRaceFrom(p).getRace().map(r -> r.getRaceId().toString()).orElse(null);
            memory.restore(current).ifPresent(race -> RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(race), false));
        });
    }
}
