package net.ofatech.supernaturalbloodlines.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;

import java.util.Arrays;
import java.util.Locale;

/** {@code /supernaturalbloodlines debug get|set|event|supernatural}: quick state control for in-game checks (permission 2). */
@EventBusSubscriber(modid = SupernaturalBloodlines.MODID)
public final class DebugCommand {
    private static final DynamicCommandExceptionType UNKNOWN_SPECIES = new DynamicCommandExceptionType(
            n -> Component.literal("Unknown species '" + n + "'. Known: " + names(Species.values())));
    private static final DynamicCommandExceptionType UNKNOWN_EVENT = new DynamicCommandExceptionType(
            n -> Component.literal("Unknown event '" + n + "'. Known: " + names(BloodlineEvent.values())));

    private DebugCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SupernaturalBloodlines.MODID)
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> get(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("species", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Species.values()).map(Enum::name), b))
                                                .executes(c -> set(c, EntityArgument.getPlayer(c, "player"),
                                                        StringArgumentType.getString(c, "species"))))))
                        .then(Commands.literal("event")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("event", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(BloodlineEvent.values()).map(Enum::name), b))
                                                .executes(c -> event(c, EntityArgument.getPlayer(c, "player"),
                                                        StringArgumentType.getString(c, "event"))))))
                        .then(Commands.literal("supernatural")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("show").executes(c -> showSupernatural(c, EntityArgument.getPlayer(c, "player"))))
                                        .then(Commands.literal("backfill").executes(c -> showBackfill(c, EntityArgument.getPlayer(c, "player"))))
                                        .then(Commands.literal("reset").executes(c -> resetSupernatural(c, EntityArgument.getPlayer(c, "player"))))
                                        .then(Commands.literal("sp")
                                                .then(Commands.argument("n", IntegerArgumentType.integer(-1_000_000, 1_000_000))
                                                        .executes(c -> giveSp(c, EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "n")))))
                                        .then(Commands.literal("rung")
                                                .then(Commands.argument("race", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                                Arrays.stream(SupernaturalRace.values()).map(SupernaturalRace::id), b))
                                                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 20))
                                                                .executes(c -> setRung(c, EntityArgument.getPlayer(c, "player"),
                                                                        StringArgumentType.getString(c, "race"), IntegerArgumentType.getInteger(c, "n"))))))))));
    }

    private static int get(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": "
                + BloodlineAccess.get(p)), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, ServerPlayer p, String name)
            throws CommandSyntaxException {
        Species species = parse(Species.class, name, UNKNOWN_SPECIES);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(species).build());
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " is now " + species), true);
        return 1;
    }

    private static int event(CommandContext<CommandSourceStack> c, ServerPlayer p, String name)
            throws CommandSyntaxException {
        BloodlineEvent event = parse(BloodlineEvent.class, name, UNKNOWN_EVENT);
        // Core cannot see factions: the context has faction NONE and humanLike true (BloodlineContext.defaults()),
        // but it does know whether Werewolves is installed, so hybrid types are refused without it.
        Outcome out = BloodlineAccess.applyEvent(p, event, BloodlineContext.defaults().withGameTime(p.level().getGameTime())
                .withWerewolvesLoaded(ModList.get().isLoaded("werewolves")));
        String note = " (debug context: faction NONE, human-like)";
        if (out.refused()) {
            c.getSource().sendFailure(Component.translatable(out.refusalKey()).append(note));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal(event + " applied: " + out.state() + ", actions "
                + out.actions() + " (not performed)" + note), true);
        return 1;
    }

    // ---- debug supernatural <player> show | rung <race> <n> | sp <n> | reset | backfill ----

    private static int showSupernatural(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + SupernaturalAccess.get(p)), false);
        return 1;
    }

    private static int showBackfill(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SupernaturalState s = SupernaturalAccess.get(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": schema " + s.schema() + ", back-filled rungs "
                + (s.backfill().isEmpty() ? "none" : String.join(", ", s.backfill()))), false);
        return 1;
    }

    /** Back to "never migrated": the next login runs the migration again. */
    private static int resetSupernatural(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": supernatural state reset (migrates again on the next login)"), true);
        return 1;
    }

    private static int giveSp(CommandContext<CommandSourceStack> c, ServerPlayer p, int n) {
        SupernaturalState s = SupernaturalAccess.get(p);
        int sp = Math.max(0, s.sp() + n);
        SupernaturalAccess.set(p, s.toBuilder().sp(sp).spEarned(n > 0 ? s.spEarned() + n : s.spEarned()).build());
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " has " + sp + " skill points"), true);
        return 1;
    }

    private static int setRung(CommandContext<CommandSourceStack> c, ServerPlayer p, String race, int n) {
        java.util.Optional<SupernaturalRace> found = SupernaturalRace.byId(race.toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Unknown race '" + race + "'. Known: " + String.join(", ", Arrays.stream(SupernaturalRace.values()).map(SupernaturalRace::id).toList())));
            return 0;
        }
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().rung(found.get(), n).build());
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + found.get().id() + " rung is now " + n), true);
        return 1;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, DynamicCommandExceptionType error)
            throws CommandSyntaxException {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw error.create(name);
        }
    }

    private static String names(Enum<?>[] values) {
        return String.join(", ", Arrays.stream(values).map(Enum::name).toList());
    }
}
