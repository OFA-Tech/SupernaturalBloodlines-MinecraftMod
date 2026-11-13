package net.ofatech.supernaturalbloodlines.shared.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.Config;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * R96 moon control: whether a werewolf shifts at will and is spared the forced full-moon shift. Who has it is decided
 * by sources that parts register; each returns the most control it grants (a Moon Ring, hybrid types and Alphas FULL, a
 * born wolf with an anchor HALF). The result is the best source, capped by the Werewolves level: FULL needs
 * {@code full_form_level}, below it even a FULL source only gives HALF. Links no Werewolves class (the faction is matched
 * by id), but it only makes sense when Werewolves is loaded: register sources behind {@code isLoaded("werewolves")}.
 * Nothing here blocks anything; {@link ForcedShift} enforces it and reads {@link #of}.
 */
public final class MoonControl {
    /** NONE = only the forced shift; HALF = half forms at will; FULL = full forms too (Werewolves level from config). */
    public enum Control { NONE, HALF, FULL }

    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");
    private static final List<Function<ServerPlayer, Control>> SOURCES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<ServerPlayer>> HALVERS = new CopyOnWriteArrayList<>();
    private static volatile Boolean moonBoundOverride;

    private MoonControl() {
    }

    /** A source that gives FULL control (subject to the level cap) when the predicate holds. */
    public static void addSource(Predicate<ServerPlayer> source) {
        addCap(p -> source.test(p) ? Control.FULL : Control.NONE);
    }

    /** A source that gives up to the returned control (NONE for nothing). The level cap still applies. */
    public static void addCap(Function<ServerPlayer, Control> source) {
        SOURCES.add(source);
    }

    /**
     * A reason to halve the forced shift without giving control (an anchor near a bitten wolf). Born wolves are always
     * halved. Reasons do not stack: halved is halved, one coin per wolf and night ({@link #bornSparedOn}).
     */
    public static void addForcedShiftHalver(Predicate<ServerPlayer> halves) {
        HALVERS.add(halves);
    }

    public static boolean forcedShiftHalved(ServerPlayer p) {
        return HALVERS.stream().anyMatch(h -> h.test(p));
    }

    public static Control of(ServerPlayer p) {
        Control best = Control.NONE;
        for (Function<ServerPlayer, Control> s : SOURCES) {
            Control c = s.apply(p);
            if (c.ordinal() > best.ordinal()) {
                best = c;
            }
        }
        if (best == Control.NONE) {
            return Control.NONE;
        }
        Control cap = werewolfLevel(p) >= fullFormLevel() ? Control.FULL : Control.HALF;
        return best.ordinal() <= cap.ordinal() ? best : cap;
    }

    /**
     * R90: a born wolf is spared half of the forced full-moon shifts. Werewolves retries the forced shift every second all
     * night, so a per-attempt coin would only delay it: the coin is thrown once per wolf and night ({@code day} = world
     * time / 24000), so the same wolves are spared all night.
     */
    public static boolean bornSparedOn(UUID id, long day) {
        long h = id.getMostSignificantBits() * 31 + id.getLeastSignificantBits() + day * 0x9E3779B97F4A7C15L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h & 1) == 0;
    }

    public static int fullFormLevel() {
        try {
            return Config.FULL_FORM_LEVEL.get();
        } catch (IllegalStateException notLoaded) {
            return Config.DEFAULT_FULL_FORM_LEVEL;
        }
    }

    /** The {@code moon_bound_werewolves} config: false means nobody needs control to shift. */
    public static boolean moonBound() {
        Boolean pinned = moonBoundOverride;
        if (pinned != null) {
            return pinned;
        }
        try {
            return Config.MOON_BOUND.get();
        } catch (IllegalStateException notLoaded) {
            return Config.DEFAULT_MOON_BOUND;
        }
    }

    /** For tests: pins {@link #moonBound()}; null clears it. */
    @VisibleForTesting
    public static void overrideMoonBound(Boolean value) {
        moonBoundOverride = value;
    }

    private static int werewolfLevel(ServerPlayer p) {
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        IPlayableFaction<?> f = h.getCurrentFaction();
        return f != null && WEREWOLF_ID.equals(f.getID()) ? h.getCurrentLevel() : 0;
    }
}
