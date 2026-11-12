package net.ofatech.supernaturalbloodlines.core.command;

import com.mojang.brigadier.CommandDispatcher;
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
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

import java.util.Arrays;
import java.util.Locale;

/** {@code /supernaturalbloodlines debug get|set|event}: quick state control for in-game checks (permission 2). */
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
                                                        StringArgumentType.getString(c, "event"))))))));
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
        // Core cannot see factions: the context has faction NONE and humanLike true (BloodlineContext.defaults()).
        Outcome out = BloodlineAccess.applyEvent(p, event, BloodlineContext.defaults().withGameTime(p.level().getGameTime()));
        String note = " (debug context: faction NONE, human-like)";
        if (out.refused()) {
            c.getSource().sendSuccess(() -> Component.literal(event + " refused: " + out.refusalKey() + note), false);
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal(event + " applied: " + out.state() + ", actions "
                + out.actions() + " (not performed)" + note), true);
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
