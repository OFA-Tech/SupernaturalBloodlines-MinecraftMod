package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /bloodline tree [player]}, {@code /bloodline call}, {@code /bloodline accept <token>} (what the [Accept] click runs) and
 * {@code /bloodline compel <token> discount|follow} (what the compulsion's buttons run).
 */
public final class BloodlineCommand {
    private static final String KEY = "supernaturalbloodlines.bloodline.";

    private BloodlineCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bloodline")
                .then(Commands.literal("tree")
                        .executes(c -> tree(c, c.getSource().getPlayerOrException().getGameProfile()))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(c -> tree(c, one(c)))))
                .then(Commands.literal("call")
                        .executes(c -> call(c.getSource(), c.getSource().getPlayerOrException())))
                .then(Commands.literal("accept")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> accept(c.getSource(), c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "token")))))
                .then(Commands.literal("compel")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .then(Commands.literal("discount").executes(c -> compel(c, "discount")))
                                .then(Commands.literal("follow").executes(c -> compel(c, "follow"))))));
    }

    private static int compel(CommandContext<CommandSourceStack> c, String choice) throws CommandSyntaxException {
        Component error = Compulsion.choose(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "token"), choice);
        if (error != null) {
            c.getSource().sendFailure(error);
            return 0;
        }
        return 1;
    }

    /** The one profile the argument names; a selector matching several players is refused. */
    private static GameProfile one(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<GameProfile> found = GameProfileArgument.getGameProfiles(c, "player");
        if (found.size() != 1) {
            c.getSource().sendFailure(Component.translatable(KEY + "tree.one_player"));
            return null;
        }
        return found.iterator().next();
    }

    private static int tree(CommandContext<CommandSourceStack> c, GameProfile target) {
        return target == null ? 0 : tree(c.getSource(), target);
    }

    /** Everyone may read their own tree; someone else's needs permission level 2. */
    public static int tree(CommandSourceStack source, GameProfile target) {
        ServerPlayer self = source.getPlayer();
        if ((self == null || !self.getUUID().equals(target.getId())) && !source.hasPermission(2)) {
            source.sendFailure(Component.translatable(KEY + "tree.denied"));
            return 0;
        }
        SirelineIndex index = SirelineIndex.get(source.getLevel());
        UUID root = target.getId();
        MutableComponent out = Component.translatable(KEY + "tree.header", index.nameOf(root).orElse(target.getName()));
        index.sireOf(root).ifPresent(sire -> out.append("\n").append(Component.translatable(KEY + "tree.sired_by", index.nameOf(sire).orElse(sire.toString()))));
        var lines = SirelineTree.below(index, root);
        if (lines.isEmpty()) {
            out.append("\n").append(Component.translatable(KEY + "tree.none"));
        }
        for (SirelineTree.Line l : lines) {
            String type = "minecraft:player".equals(l.entityType()) ? "" : " (" + l.entityType() + ")";
            out.append("\n").append(Component.literal("  ".repeat(l.depth()) + "- " + l.name() + type));
        }
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static int call(CommandSourceStack source, ServerPlayer caller) {
        SireCall.Result r = SireCall.call(caller);
        if (r.error() != null) {
            source.sendFailure(r.error());
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(KEY + "call.sent", r.players(), r.mobs()), false);
        return 1;
    }

    private static int accept(CommandSourceStack source, ServerPlayer sired, String token) {
        SireCall.Result r = SireCall.accept(sired, token);
        if (r.error() != null) {
            source.sendFailure(r.error());
            return 0;
        }
        return 1;
    }
}
