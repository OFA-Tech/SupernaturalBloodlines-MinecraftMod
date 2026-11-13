package net.ofatech.supernaturalbloodlines.shared.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.Config;

/**
 * R96 moon control: whether a werewolf shifts at will and is spared the forced full-moon shift. Who has it is decided
 * by sources that parts register (a bound Moon Ring, hybrid types; Alphas and anchors later). Links no Werewolves
 * class (the faction is matched by id), but it only makes sense when Werewolves is loaded: register sources behind
 * {@code isLoaded("werewolves")}. Nothing here blocks anything; whoever enforces the lack of control reads {@link #of}.
 */
public final class MoonControl {
    /** NONE = only the forced shift; HALF = half forms at will; FULL = full forms too (Werewolves level from config). */
    public enum Control { NONE, HALF, FULL }

    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");
    private static final List<Predicate<ServerPlayer>> SOURCES = new CopyOnWriteArrayList<>();

    private MoonControl() {
    }

    public static void addSource(Predicate<ServerPlayer> source) {
        SOURCES.add(source);
    }

    public static Control of(ServerPlayer p) {
        if (SOURCES.stream().noneMatch(s -> s.test(p))) {
            return Control.NONE;
        }
        return werewolfLevel(p) >= fullFormLevel() ? Control.FULL : Control.HALF;
    }

    public static int fullFormLevel() {
        try {
            return Config.FULL_FORM_LEVEL.get();
        } catch (IllegalStateException notLoaded) {
            return Config.DEFAULT_FULL_FORM_LEVEL;
        }
    }

    private static int werewolfLevel(ServerPlayer p) {
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        IPlayableFaction<?> f = h.getCurrentFaction();
        return f != null && WEREWOLF_ID.equals(f.getID()) ? h.getCurrentLevel() : 0;
    }
}
