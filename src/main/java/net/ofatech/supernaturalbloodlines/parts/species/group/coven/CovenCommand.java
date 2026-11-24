package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/** The coven's literals of the shared {@code /group} tree. A literal that is the coven's only answers {@code wrong_type} (or {@code no_group}) anywhere else. */
public final class CovenCommand {
    private CovenCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> link() {
        return Commands.literal("link")
                .then(Commands.literal("open").executes(c -> run(c, CircleLink.open(self(c)))))
                .then(Commands.literal("close").executes(c -> run(c, CircleLink.close(self(c)))))
                .then(Commands.literal("join")
                        .executes(c -> run(c, CircleLink.join(self(c), null)))
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> run(c, CircleLink.join(self(c), StringArgumentType.getString(c, "token"))))))
                .then(Commands.literal("leave").executes(c -> run(c, CircleLink.leave(self(c)))))
                .then(Commands.literal("consent")
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.Arrays.stream(Consent.values()).map(Consent::id), b))
                                .executes(c -> run(c, consent(self(c), StringArgumentType.getString(c, "level"))))));
    }

    /** Sets the level the player's rites may ask of them; stored per player. */
    @Nullable
    public static Component consent(ServerPlayer p, String word) {
        if (CircleLink.covenOf(p) == null) {
            return CircleLink.notACoven(p);
        }
        Consent level = Consent.parse(word);
        if (!level.id().equals(word)) {
            return CovenText.error("bad_level");
        }
        GroupIndex.get(p.serverLevel()).setConsent(p.getUUID(), level);
        p.sendSystemMessage(CovenText.of("consent_set", CovenText.of("consent." + level.id())));
        HubRequests.changed(p);
        return null;
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