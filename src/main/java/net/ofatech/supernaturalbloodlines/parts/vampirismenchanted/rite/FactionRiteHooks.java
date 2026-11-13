package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;

/**
 * Where the species part plugs into {@link FactionRiteFactory} without this part knowing it. The defaults are what the
 * rites did on their own; the species part installs its own from its init.
 */
public final class FactionRiteHooks {
    /** The context the rules (preview and real call) are run with. */
    @FunctionalInterface
    public interface ContextProvider {
        BloodlineContext of(ServerPlayer target, BaseFaction faction, long gameTime);
    }

    /** Performs the faction side of an Outcome. */
    @FunctionalInterface
    public interface OutcomeRunner {
        void run(ServerPlayer target, Outcome outcome);
    }

    private static volatile ContextProvider context = (p, faction, time) ->
            BloodlineContext.defaults().withFaction(faction).withGameTime(time);
    private static volatile OutcomeRunner runner = FactionRiteFactory::runFactionActions;

    private FactionRiteHooks() {
    }

    public static void install(ContextProvider contextProvider, OutcomeRunner outcomeRunner) {
        context = contextProvider;
        runner = outcomeRunner;
    }

    public static BloodlineContext context(ServerPlayer target, BaseFaction faction, long gameTime) {
        return context.of(target, faction, gameTime);
    }

    public static void run(ServerPlayer target, Outcome outcome) {
        runner.run(target, outcome);
    }
}
