package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.MoonRule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.network.EclipsePayload;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The lunar eclipse (R95 weakness): a full-moon night may be an eclipse, with {@code lunar_eclipse_chance}. Until dawn the
 * werewolves have no form actions ({@link FormBlocks}), no rank perks ({@link RankEffects}) and no pack bonus
 * ({@link PackBonus}). The state is one number in the overworld's {@link Data}: the night (day number) it belongs to. A night
 * lies inside one day number ({@link #DUSK} to {@link #DAWN}), so the eclipse is over when the day number moves on or the
 * time of day reaches {@link #DAWN}; {@link #isActive} reads exactly that, so a stored eclipse that nobody cleared (a
 * crash, a restart after the dawn, a {@code /time set}) is never active. The server tick clears and announces it, and
 * {@link #onServerStarted} clears it quietly. Does not link Werewolves.
 */
public final class LunarEclipse {
    public static final long NONE = -1;
    // Werewolves' own full-moon window (Helper.isFullMoon): the time of day is after 12786 and before 23216 on the phase-0 day
    public static final long DUSK = MoonRule.NIGHT_START;
    public static final long DAWN = MoonRule.NIGHT_END;
    public static final String START_MESSAGE = "supernaturalbloodlines.eclipse.start";
    public static final String END_MESSAGE = "supernaturalbloodlines.eclipse.end";
    private static final String NAME = "supernaturalbloodlines_eclipse";
    private static final String ECLIPSE_NIGHT = "eclipseNight";
    private static final String ROLLED_NIGHT = "rolledNight";
    private static final int CHECK_PERIOD = 20;
    private static final DoubleSupplier RANDOM = () -> ThreadLocalRandom.current().nextDouble();

    private static volatile DoubleSupplier roll = RANDOM;
    private static volatile Function<MinecraftServer, Collection<ServerPlayer>> players = s -> s.getPlayerList().getPlayers();

    private LunarEclipse() {
    }

    /** The eclipse night (a day number, or {@link #NONE}) and the last night that was rolled for it. Saved with the overworld. */
    public static final class Data extends SavedData {
        private long eclipseNight = NONE;
        private long rolledNight = NONE;

        public long eclipseNight() {
            return eclipseNight;
        }

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            d.eclipseNight = tag.contains(ECLIPSE_NIGHT) ? tag.getLong(ECLIPSE_NIGHT) : NONE;
            d.rolledNight = tag.contains(ROLLED_NIGHT) ? tag.getLong(ROLLED_NIGHT) : NONE;
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLong(ECLIPSE_NIGHT, eclipseNight);
            tag.putLong(ROLLED_NIGHT, rolledNight);
            return tag;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), NAME);
    }

    /** Pure: the eclipse stored in {@code d} is running at this overworld time (days are 24000 ticks, the night ends at {@link #DAWN}). */
    public static boolean isActive(Data d, long dayTime) {
        return d.eclipseNight != NONE && dayTime / 24000L == d.eclipseNight && dayTime % 24000L < DAWN;
    }

    public static boolean active(MinecraftServer server) {
        return isActive(data(server), server.overworld().getDayTime());
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % CHECK_PERIOD == 0) {
                tick(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> onServerStarted(e.getServer()));
        // every login hears the state, even "no eclipse": the client keeps the last one it was told across worlds
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                EclipsePayload.send(p, new EclipsePayload(active(p.server)));
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> EclipseCommand.register(e.getDispatcher()));
        RankEffects.addSuppressor(p -> active(p.server));
        PackBonus.addSuppressor(p -> active(p.server));
    }

    /** Once a second: ends an eclipse that is over, and rolls for one at dusk of a full-moon night (once per night, only with someone online). */
    public static void tick(MinecraftServer server) {
        Data d = data(server);
        long time = server.overworld().getDayTime();
        long day = time / 24000L;
        long timeOfDay = time % 24000L;
        if (d.eclipseNight != NONE && !isActive(d, time)) {
            stop(server);
        } else if (d.eclipseNight == NONE && d.rolledNight != day && timeOfDay > DUSK && timeOfDay < DAWN
                && server.overworld().getMoonPhase() == 0 && !players.apply(server).isEmpty()) {
            d.rolledNight = day;
            d.setDirty();
            if (roll.getAsDouble() < SpeciesConfig.lunarEclipseChance()) {
                start(server);
            }
        }
    }

    /** After a start: a stored eclipse that is already over (the server was off through the dawn) is dropped, without a word. */
    public static void onServerStarted(MinecraftServer server) {
        Data d = data(server);
        if (d.eclipseNight != NONE && !isActive(d, server.overworld().getDayTime())) {
            d.eclipseNight = NONE;
            d.setDirty();
        }
    }

    /** Starts the eclipse of tonight (of the next night once the dawn has come). False when one is already running. */
    public static boolean start(MinecraftServer server) {
        Data d = data(server);
        long time = server.overworld().getDayTime();
        if (isActive(d, time)) {
            return false;
        }
        d.eclipseNight = time / 24000L + (time % 24000L >= DAWN ? 1 : 0);
        d.setDirty();
        changed(server, true);
        return true;
    }

    /** Ends it now. False when none is stored. */
    public static boolean stop(MinecraftServer server) {
        Data d = data(server);
        if (d.eclipseNight == NONE) {
            return false;
        }
        d.eclipseNight = NONE;
        d.setDirty();
        changed(server, false);
        return true;
    }

    private static void changed(MinecraftServer server, boolean on) {
        server.getPlayerList().broadcastSystemMessage(Component.translatable(on ? START_MESSAGE : END_MESSAGE), false);
        for (ServerPlayer p : players.apply(server)) {
            EclipsePayload.send(p, new EclipsePayload(on));
            RankEffects.sync(p);
            PackBonus.tick(p);
        }
    }

    /** For tests: who counts as online (mock players are not in the player list); null resets. */
    @VisibleForTesting
    public static void overridePlayers(@Nullable Function<MinecraftServer, Collection<ServerPlayer>> lookup) {
        players = lookup != null ? lookup : s -> s.getPlayerList().getPlayers();
    }

    /** For tests: makes the chance roll deterministic (a value in [0, 1)). */
    @VisibleForTesting
    public static void useRoll(DoubleSupplier forced) {
        roll = forced;
    }

    @VisibleForTesting
    public static void resetRoll() {
        roll = RANDOM;
    }
}
