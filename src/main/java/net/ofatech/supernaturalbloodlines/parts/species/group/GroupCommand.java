package net.ofatech.supernaturalbloodlines.parts.species.group;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /group}, with {@code /clan}, {@code /coven} and {@code /brotherhood} as the same tree and {@code /sb group} under our
 * own prefix. The shared actions are {@link PackCommand}'s, behind {@link GroupGate}; a literal that is one type's only answers
 * {@code wrong_type} on another. Registered whether or not Werewolves is installed.
 */
public final class GroupCommand {
    private GroupCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralCommandNode<CommandSourceStack> group = d.register(tree("group"));
        d.register(Commands.literal("clan").redirect(group));
        d.register(Commands.literal("coven").redirect(group));
        d.register(Commands.literal("brotherhood").redirect(group));
        d.register(Commands.literal("sb").then(Commands.literal("group").redirect(group)));
    }

    /** The tree as a plain builder, so a test can build it in a dispatcher of its own. */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> tree(String root) {
        return Commands.literal(root)
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, PackCommand.invite(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("accept")
                        .executes(c -> run(c, PackCommand.acceptLatest(self(c))))
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> run(c, PackCommand.accept(self(c), StringArgumentType.getString(c, "token"))))))
                .then(Commands.literal("leave").executes(c -> run(c, PackCommand.leave(self(c)))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((c, b) -> PackCommand.suggest(c, b, false))
                                .executes(c -> run(c, PackCommand.kickNamed(self(c), StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("ally")
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> run(c, PackCommand.allyInvite(self(c), EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("token", StringArgumentType.word())
                                        .executes(c -> run(c, PackCommand.allyAccept(self(c), StringArgumentType.getString(c, "token"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((c, b) -> PackCommand.suggest(c, b, true))
                                        .executes(c -> run(c, PackCommand.allyRemoveNamed(self(c), StringArgumentType.getString(c, "player")))))))
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenCommand.link())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenCommand.elder())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenCommand.shun())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenCommand.vote())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanCommand.promote())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanCommand.demote())
                .then(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanCommand.exile())
                .then(Commands.literal("info").executes(c -> info(c.getSource(), self(c))))
                .then(Commands.literal("challenge")
                        .then(Commands.literal("accept").executes(c -> run(c, challenge(self(c), null, p -> PackDuel.accept(p)))))
                        .then(Commands.literal("decline").executes(c -> run(c, challenge(self(c), null, p -> PackDuel.decline(p)))))
                        .then(Commands.argument("leader", StringArgumentType.word())
                                .executes(c -> run(c, challenge(self(c), StringArgumentType.getString(c, "leader"), p -> PackDuel.challenge(p, StringArgumentType.getString(c, "leader")))))));
    }

    /** The one challenge command routes by type: a pack's is the duel, a coven's is the call for a new Regent; the others have theirs in later plans. */
    @Nullable
    private static Component challenge(ServerPlayer p, @Nullable String word, java.util.function.Function<ServerPlayer, Component> duel) {
        GroupType type = Groups.typeOf(p);
        if (type == GroupType.COVEN && word != null) {
            return net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance.callElection(p, word);
        }
        if (type != GroupType.PACK || !ModList.get().isLoaded("werewolves")) {
            return GroupText.error(type, "wrong_type");
        }
        return duel.apply(p);
    }

    private static int info(CommandSourceStack source, ServerPlayer p) {
        PackIndex index = PackIndex.get(p.serverLevel());
        if (!index.inPack(p.getUUID())) {
            source.sendFailure(Component.translatable("supernaturalbloodlines.group.info.none"));
            return 0;
        }
        GroupType type = Groups.typeOf(p);
        if (type == GroupType.PACK && ModList.get().isLoaded("werewolves")) {
            return PackCommand.info(source, p);
        }
        UUID leader = index.leaderOf(p.getUUID());
        Component out = Component.translatable("supernaturalbloodlines.group.info.line", GroupText.noun(type), name(p, leader), members(p, index.betasOf(leader)));
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static String name(ServerPlayer viewer, UUID id) {
        String n = PackAccess.nameOf(viewer.server, id);
        return n != null ? n : id.toString().substring(0, 8);
    }

    private static Component members(ServerPlayer viewer, List<UUID> ids) {
        return ids.isEmpty() ? Component.translatable("supernaturalbloodlines.group.info.nobody")
                : Component.literal(String.join(", ", ids.stream().map(id -> name(viewer, id)).toList()));
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
