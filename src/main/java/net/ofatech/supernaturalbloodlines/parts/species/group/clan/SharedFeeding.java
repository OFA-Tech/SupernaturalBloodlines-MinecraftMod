package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IBloodStats;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.clan.FeedLedger;
import net.ofatech.supernaturalbloodlines.core.group.clan.FeedShare;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.supernatural.SharedFeed;

/**
 * Shared feeding (R274): a feed on a living creature gives each clanmate nearby a little blood. The listener runs last, so it reads what the feeder really got;
 * the mate drinks with an empty source inside {@link SharedFeed}, so the event that fires is ignored here and by every rule that must not act on a share.
 */
public final class SharedFeeding {
    private static final FeedLedger LEDGER = new FeedLedger();
    private static final Map<UUID, Long> TOLD = new HashMap<>();
    private static final int SPACING_TICKS = 100;
    private static LongSupplier ticks;
    private static boolean warned;

    private SharedFeeding() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BloodDrinkEvent.PlayerDrinkBloodEvent.class, SharedFeeding::onDrink);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    /** For tests: pins the game time the same-feeder spacing reads; null restores the level's. */
    public static void overrideTicks(LongSupplier supplier) {
        ticks = supplier;
    }

    public static void reset() {
        LEDGER.clear();
        TOLD.clear();
        ticks = null;
    }

    private static void onDrink(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        if (SharedFeed.active() || e.getAmount() <= 0 || e.getBloodSource().getEntity().isEmpty()
                || !GroupsConfig.feedShareActive()
                || !(e.getVampire().getRepresentingEntity() instanceof ServerPlayer feeder)) {
            return;
        }
        ServerLevel level = feeder.serverLevel();
        ClanScope.Clan clan = ClanScope.of(level, feeder.getUUID());
        if (clan == null || !ClanScope.appliesTo(level, clan.leader(), feeder)) {
            return;
        }
        MinecraftServer server = feeder.server;
        if (e.getBloodSource().getEntity().get() instanceof ServerPlayer victim && (victim == feeder || Charters.isMember(server, victim.getUUID(), clan.group()))) {
            return;
        }
        int share = FeedShare.of(e.getAmount(), GroupsConfig.feedSharePercent(), GroupsConfig.feedShareMax());
        long tick = ticks != null ? ticks.getAsLong() : level.getGameTime();
        long now = PackIndex.now();
        double radius = GroupsConfig.feedShareRadius();
        for (UUID id : PackIndex.get(level).roster(clan.leader())) {
            ServerPlayer mate = SireCall.player(server, id);
            if (mate == null || mate == feeder || !mate.isAlive() || mate.level() != feeder.level() || mate.distanceTo(feeder) > radius
                    || !ClanScope.appliesTo(level, clan.leader(), mate)) {
                continue;
            }
            try {
                IBloodStats stats = VampirismAPI.vampirePlayer(mate).getBloodStats();
                if (stats.getBloodLevel() >= stats.getMaxBlood()) {
                    continue;
                }
                int granted = LEDGER.grant(feeder.getUUID(), mate.getUUID(), share, now, tick, GroupsConfig.feedShareCapPerMinute(), SPACING_TICKS);
                if (granted > 0) {
                    SharedFeed.run(() -> VampirismAPI.vampirePlayer(mate).drinkBlood(granted, 0f, IDrinkBloodContext.none()));
                    Long told = TOLD.get(mate.getUUID());
                    if (told == null || now - told >= 60_000L || now < told) {
                        TOLD.put(mate.getUUID(), now);
                        mate.displayClientMessage(ClanText.of("shared_feed"), true);
                    }
                }
            } catch (RuntimeException failure) {
                if (!warned) {
                    warned = true;
                    SupernaturalBloodlines.LOGGER.warn("a shared feed failed for one mate (logged once)", failure);
                }
            }
        }
    }
}
