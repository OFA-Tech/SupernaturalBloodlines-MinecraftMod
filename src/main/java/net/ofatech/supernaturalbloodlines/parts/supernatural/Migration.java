package net.ofatech.supernaturalbloodlines.parts.supernatural;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.ReentrancyGuard;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.shared.supernatural.ApexRaceQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.SireCount;
import net.ofatech.supernaturalbloodlines.shared.werewolves.EmissaryLines;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;

/**
 * Runs once per player, on login, when the schema is 0: gives a player who had a race before the ladders what that race proves (see
 * {@link MigrationRules}). It runs before the login re-read of the detectors ({@code Detectors}), so the race is stamped from
 * {@code turnedAt}, not from "now".
 */
public final class Migration {
    private static final String CHANNEL = "supernatural-migration";

    private Migration() {
    }

    /** True when the player was migrated by this call. */
    public static boolean runIfNeeded(ServerPlayer p) {
        if (SupernaturalAccess.get(p).schema() >= MigrationRules.SCHEMA) {
            return false;
        }
        return ReentrancyGuard.run(p.getUUID(), CHANNEL, () -> migrate(p));
    }

    private static void migrate(ServerPlayer p) {
        List<SupernaturalRace> races = TaskTracker.races(p);
        IPlayableFaction<?> faction = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        boolean moon = ModList.get().isLoaded("werewolves") && MoonControl.of(p) != MoonControl.Control.NONE;
        MigrationRules.Facts facts = new MigrationRules.Facts(races.get(0), races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty(),
                BloodlineAccess.get(p), p.level().getGameTime(), faction == null ? null : faction.getID().toString(),
                VampirismAPI.factionPlayerHandler(p).getCurrentLevel(), EmissaryLines.isEmissary(p), moon, SireCount.of(p),
                ApexRaceQuery.pathOf(p));
        MigrationRules.Result result = MigrationRules.migrate(LadderTable.current(), SupernaturalAccess.get(p), facts);
        SupernaturalAccess.set(p, result.state());
        if (!result.rungsGiven().isEmpty()) {
            SupernaturalBloodlines.LOGGER.info("Supernatural migration of {}: {} rungs given ({} SP, no EP): {}", p.getGameProfile().getName(),
                    result.rungsGiven().size(), result.spGiven(), result.rungsGiven());
        }
    }
}
