package net.ofatech.supernaturalbloodlines.parts.species.unlock;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;

/**
 * {@code /bloodline rites list|learn|forget <player> [<rite>]} (op 2): what a player knows of the rites. {@code <rite>} is a gated rite, a lore id
 * ({@code esther}, {@code finn}, ...) or {@code all}. Forgetting a rite of the player's kind takes effect until the next check (every five seconds).
 */
public final class RiteUnlockCommand {
    private static final String KEY = "supernaturalbloodlines.rites.command.";

    private RiteUnlockCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("rites").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> list(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("learn").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rite", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(RiteUnlockTable.choices(), b))
                                .executes(c -> change(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "rite"), true)))))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rite", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(RiteUnlockTable.choices(), b))
                                .executes(c -> change(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "rite"), false)))));
    }

    /** The gated rites {@code what} names; empty when it names nothing. */
    public static List<String> resolve(String what) {
        if (what.equals("all")) {
            return List.copyOf(RiteUnlockTable.GATED.keySet());
        }
        if (RiteUnlockTable.isGated(what)) {
            return List.of(what);
        }
        return RiteUnlockTable.taughtBy(what);
    }

    public static int change(CommandSourceStack source, ServerPlayer target, String what, boolean learn) {
        List<String> rites = resolve(what);
        if (rites.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "unknown", what));
            return 0;
        }
        int changed = 0;
        for (String rite : rites) {
            if (learn ? RiteUnlocks.learn(target, rite, false) : RiteUnlocks.forget(target, rite)) {
                changed++;
            }
        }
        int n = changed;
        source.sendSuccess(() -> Component.translatable(KEY + (learn ? "learned" : "forgot"), target.getDisplayName(), n), true);
        return changed;
    }

    public static int list(CommandSourceStack source, ServerPlayer target) {
        List<String> known = RiteUnlocks.known(target);
        source.sendSuccess(() -> Component.translatable(KEY + "list", target.getDisplayName(), known.size(), RiteUnlockTable.GATED.size(),
                known.isEmpty() ? Component.literal("-") : Component.literal(String.join(", ", known))), false);
        return known.size();
    }
}
