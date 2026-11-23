package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import org.jetbrains.annotations.Nullable;

/** {@code /ward invite|uninvite|drop|info} (R309): the typed form of every hub button of the ward card; each ends in {@link WardService}. */
public final class WardCommand {
    private WardCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ward")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, WardService.invite(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("uninvite")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(c -> run(c, WardService.uninvite(self(c), StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("drop")
                        .then(Commands.argument("ward", StringArgumentType.word())
                                .executes(c -> run(c, WardService.drop(self(c), StringArgumentType.getString(c, "ward"))))))
                .then(Commands.literal("info").executes(c -> info(c))));
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

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = self(c);
        var wards = WardIndex.get(player.serverLevel()).table().ownedBy(player.getUUID());
        if (wards.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.ward.info_none"), false);
            return 1;
        }
        for (WardTable.Ward ward : wards) {
            c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.ward.info_line", ward.id(), ward.kind().name().toLowerCase(java.util.Locale.ROOT),
                    ward.cx(), ward.cy(), ward.cz(), ward.invited().size() - 1, ward.fallen() ? Component.translatable("supernaturalbloodlines.ward.fallen") : ""), false);
        }
        return wards.size();
    }
}
