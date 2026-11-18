package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminAccess;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/**
 * Runs a {@link HubCommandPayload}: the row must be in {@code AdminCommands}, the arguments must fit it, and the typed command runs as the
 * sender through Brigadier, which applies its own permission check and every rule of the command. What the command says goes to chat as
 * usual and, with whether it worked, back to the sender as a {@link HubCommandReplyPayload} for the page to show.
 */
public final class HubCommands {
    private static final String KEY = "supernaturalbloodlines.hub.command.";

    private HubCommands() {}

    /** Collects what a command sends to its source and also passes it on to the player, so chat shows it too. */
    private static final class Capture implements CommandSource {
        private final ServerPlayer player;
        final List<Component> lines = new ArrayList<>();

        Capture(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public void sendSystemMessage(Component message) {
            lines.add(message);
            player.sendSystemMessage(message);
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false; // the actor already sees the reply; other ops are told by the command's own log line
        }
    }

    /**
     * @param level   the sender's permission level as the server's op list says it
     * @param stack   the command source of the sender (a test gives one with its own permission)
     * @return whether the command was run (false: refused before running)
     */
    public static boolean run(ServerPlayer p, int level, HubCommandPayload payload, Function<ServerPlayer, CommandSourceStack> stack, Consumer<HubCommandReplyPayload> reply) {
        Optional<AdminCommands.Row> row = AdminCommands.byId(payload.rowId());
        if (row.isEmpty()) {
            reply.accept(failure(payload.rowId(), "unknown"));
            return false;
        }
        if (row.get().level() >= AdminAccess.OP_LEVEL && !AdminAccess.allowed(level)) {
            // the same answer a typed command gets: nothing about the row, and no reply at all
            SupernaturalBloodlines.LOGGER.warn("{} (permission {}) tried the OP command row {} from the hub and was refused", p.getGameProfile().getName(), level, row.get().id());
            return false;
        }
        if (p.server.getCommands().getDispatcher().getRoot().getChild(row.get().root()) == null) {
            reply.accept(failure(row.get().id(), "unavailable")); // the part that owns this command is off on this server
            return false;
        }
        if (isClosedToken(p, row.get(), payload.args())) {
            reply.accept(failure(row.get().id(), "answered"));
            return false;
        }
        Optional<String> command = AdminCommands.command(row.get(), payload.args(), uuid -> nameOrUuid(p, uuid));
        if (command.isEmpty()) {
            reply.accept(failure(row.get().id(), "bad_arguments"));
            return false;
        }
        Capture capture = new Capture(p);
        boolean[] worked = {false};
        CommandSourceStack source = stack.apply(p).withSource(capture).withCallback((success, result) -> worked[0] = success && result != 0);
        p.server.getCommands().performPrefixedCommand(source, command.get());
        reply.accept(new HubCommandReplyPayload(row.get().id(), worked[0], capture.lines));
        return true;
    }

    /** A token the player already answered, or that ran out: nothing is open under it any more. Rows without a token, and a call that leaves an optional one out, are never closed. */
    private static boolean isClosedToken(ServerPlayer p, AdminCommands.Row row, List<String> args) {
        List<AdminCommands.Arg> form = row.args();
        for (int i = 0; i < form.size() && i < args.size(); i++) {
            if (form.get(i).name().equals("token") && !args.get(i).isEmpty()) {
                String token = args.get(i);
                return HubRequests.of(p).stream().noneMatch(r -> token.equals(r.token()));
            }
        }
        return false;
    }

    /** The name of the player with this UUID if they are online (the commands that take a player argument want a name), else the UUID itself (the ones that take a member by string know it). */
    static String nameOrUuid(ServerPlayer asker, String uuid) {
        ServerPlayer online = asker.server.getPlayerList().getPlayer(UUID.fromString(uuid));
        return online == null ? uuid : online.getGameProfile().getName();
    }

    private static HubCommandReplyPayload failure(String rowId, String key) {
        return new HubCommandReplyPayload(rowId.length() > 8 ? "" : rowId, false, List.of(Component.translatable(KEY + key)));
    }

    /** One payload from a player: over their rate it is answered "too quick" instead of dropped, so a double click is not silence. */
    public static boolean dispatch(ServerPlayer p, boolean withinRate, int level, HubCommandPayload payload, Function<ServerPlayer, CommandSourceStack> stack, Consumer<HubCommandReplyPayload> reply) {
        if (!withinRate) {
            reply.accept(failure(payload.rowId(), "too_quick"));
            return false;
        }
        return run(p, level, payload, stack, reply);
    }

    static void onCommand(HubCommandPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                dispatch(p, AdminNet.allowedRate(p), AdminNet.levelOf(p), payload, ServerPlayer::createCommandSourceStack, r -> PacketDistributor.sendToPlayer(p, r));
            }
        });
    }
}
