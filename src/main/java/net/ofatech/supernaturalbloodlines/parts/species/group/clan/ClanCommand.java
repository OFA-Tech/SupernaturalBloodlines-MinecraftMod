package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** The clan's literals of the shared {@code /group} tree. A literal that is the clan's only answers {@code wrong_type} anywhere else. */
public final class ClanCommand {
    private ClanCommand() {
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> member(BiFunction<ServerPlayer, String, Component> action) {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((c, b) -> {
                    if (c.getSource().getEntity() instanceof ServerPlayer me) {
                        ClanDiscipline.namesFor(me).forEach(b::suggest);
                    }
                    return b.buildFuture();
                })
                .executes(c -> run(c, action.apply(self(c), StringArgumentType.getString(c, "player"))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> promote() {
        return Commands.literal("promote").then(member(ClanDiscipline::promote));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> demote() {
        return Commands.literal("demote").then(member(ClanDiscipline::demote));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> exile() {
        return Commands.literal("exile").then(member(ClanDiscipline::exile));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> charter() {
        return Commands.literal("charter")
                .then(Commands.literal("forge").executes(c -> run(c, Charters.forge(self(c)))))
                .then(Commands.literal("reforge").executes(c -> run(c, Charters.reforge(self(c)))));
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static int run(CommandContext<CommandSourceStack> c, @Nullable Component error) {
        if (error == null) {
            return 1;
        }
        c.getSource().sendFailure(error);
        return 0;
    }
}
