package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /pack invite|accept|leave|heal|info|emissary|anchor|roar} ({@code roar} is the keybind's server method). An invitation works like the sireline call: a clickable
 * [Accept] with a token bound to the invited player, single use, valid for a minute. Each action returns the error to
 * show, or null when it worked (the messages to the people involved are sent here). Registered only with Werewolves.
 */
public final class PackCommand {
    private static final String KEY = "supernaturalbloodlines.pack.";
    private static final CallRequests REQUESTS = new CallRequests();
    private static final double HEAL_RANGE = 4;

    private PackCommand() {
    }

    public static CallRequests requests() {
        return REQUESTS;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("pack")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, invite(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("accept")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> run(c, accept(self(c), StringArgumentType.getString(c, "token"))))))
                .then(Commands.literal("leave").executes(c -> run(c, leave(self(c)))))
                .then(Commands.literal("heal")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, heal(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("info").executes(c -> info(c.getSource(), self(c))))
                .then(Commands.literal("emissary")
                        .executes(c -> run(c, emissary(self(c), null)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, emissary(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("roar").executes(c -> run(c, AlphaRoar.roar(self(c)))))
                .then(Commands.literal("anchor")
                        .executes(c -> run(c, anchor(self(c), null)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, anchor(self(c), EntityArgument.getPlayer(c, "player")))))));
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

    private static Component error(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    private static String name(ServerPlayer p) {
        return p.getGameProfile().getName();
    }

    private static PackContext context(@Nullable UUID alpha, long now) {
        return new PackContext(alpha, 0, now, 0, 30L * 24000, 300, alpha != null);
    }

    @Nullable
    public static Component invite(ServerPlayer alpha, ServerPlayer target) {
        if (!PackAccess.isAlpha(PackAccess.wolf(alpha).rank())) {
            return error("not_alpha");
        }
        if (target == alpha) {
            return error("self");
        }
        WolfState t = PackAccess.wolf(target);
        if (t.lineage() == Lineage.NONE) {
            return error("target_not_werewolf", name(target));
        }
        if (PackAccess.isAlpha(t.rank())) {
            return error("target_alpha", name(target));
        }
        if (t.packAlpha() != null) {
            return error("target_has_pack", name(target));
        }
        long now = alpha.serverLevel().getGameTime();
        target.sendSystemMessage(requestMessage(name(alpha), REQUESTS.open(alpha.getUUID(), target.getUUID(), now)));
        alpha.sendSystemMessage(Component.translatable(KEY + "invited", name(target)));
        return null;
    }

    private static Component requestMessage(String alphaName, String token) {
        Component accept = Component.translatable(KEY + "accept").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pack accept " + token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(KEY + "accept_hover", alphaName))));
        return Component.translatable(KEY + "invite", alphaName).append(" ").append(accept);
    }

    @Nullable
    public static Component accept(ServerPlayer p, String token) {
        long now = p.serverLevel().getGameTime();
        CallRequests.Request r = REQUESTS.take(token, p.getUUID(), now).orElse(null);
        if (r == null) {
            return error("expired");
        }
        ServerPlayer alpha = SireCall.player(p.server, r.caller());
        if (alpha == null || !PackAccess.isAlpha(PackAccess.wolf(alpha).rank())) {
            return error("alpha_gone");
        }
        WolfState w = PackAccess.wolf(p);
        if (w.lineage() == Lineage.NONE) {
            return error("not_werewolf");
        }
        // a Beta keeps their pack until /pack leave: a second invitation or bite never switches it
        if (w.packAlpha() != null) {
            return error("has_pack");
        }
        if (PackAccess.applyPack(p, PackEvent.JOINED_PACK, context(alpha.getUUID(), now)).refused()) {
            return error("refused");
        }
        p.sendSystemMessage(Component.translatable(KEY + "joined", name(alpha)));
        alpha.sendSystemMessage(Component.translatable(KEY + "joined_alpha", name(p)));
        return null;
    }

    @Nullable
    public static Component leave(ServerPlayer p) {
        WolfState w = PackAccess.wolf(p);
        if (PackAccess.isAlpha(w.rank())) {
            return error("alpha_cannot_leave");
        }
        if (w.packAlpha() == null) {
            return error("no_pack");
        }
        PackAccess.applyPack(p, PackEvent.LEFT_PACK, context(null, p.serverLevel().getGameTime()));
        p.sendSystemMessage(Component.translatable(KEY + "left"));
        return null;
    }

    /** An Alpha gives up the spark to fully heal a player within 4 blocks and cure their wolfsbane and hybrid venom (R93). */
    @Nullable
    public static Component heal(ServerPlayer alpha, ServerPlayer target) {
        if (!PackAccess.isAlpha(PackAccess.wolf(alpha).rank())) {
            return error("not_alpha");
        }
        if (target == alpha) {
            return error("self");
        }
        if (target.level() != alpha.level() || alpha.distanceToSqr(target) > HEAL_RANGE * HEAL_RANGE) {
            return error("heal_too_far", name(target));
        }
        SparkHandler.cure(target);
        SparkHandler.giveUp(alpha);
        target.sendSystemMessage(Component.translatable(KEY + "healed", name(alpha)));
        alpha.sendSystemMessage(Component.translatable(KEY + "gave_up", name(target)));
        return null;
    }

    /** Who may be an emissary: the witch line, siphoners included (the altar bonus alone leaves them out). */
    private static boolean isMagicUser(Species s) {
        return s == Species.WITCH || s == Species.SIPHONER || s == Species.HERETIC || s == Species.TRIBRID;
    }

    /** The Alpha names the witch whose rites cost less near the pack; a null target clears it. */
    @Nullable
    public static Component emissary(ServerPlayer alpha, @Nullable ServerPlayer target) {
        if (!PackAccess.isAlpha(PackAccess.wolf(alpha).rank())) {
            return error("not_alpha");
        }
        PackIndex index = PackIndex.get(alpha.serverLevel());
        if (target == null) {
            index.setEmissary(alpha.getUUID(), null);
            alpha.sendSystemMessage(Component.translatable(KEY + "emissary_cleared"));
            return null;
        }
        if (target == alpha) {
            return error("self");
        }
        if (!isMagicUser(BloodlineAccess.get(target).species())) {
            return error("emissary_witch");
        }
        index.setEmissary(alpha.getUUID(), target.getUUID());
        alpha.sendSystemMessage(Component.translatable(KEY + "emissary_set", name(target)));
        return null;
    }

    /** A werewolf names the player whose presence halves their forced full-moon shift; a null target clears it. */
    @Nullable
    public static Component anchor(ServerPlayer p, @Nullable ServerPlayer target) {
        if (PackAccess.wolf(p).lineage() == Lineage.NONE) {
            return error("not_werewolf");
        }
        PackIndex index = PackIndex.get(p.serverLevel());
        if (target == null) {
            index.setAnchor(p.getUUID(), null);
            p.sendSystemMessage(Component.translatable(KEY + "anchor_cleared"));
            return null;
        }
        if (target == p) {
            return error("self");
        }
        index.setAnchor(p.getUUID(), target.getUUID());
        p.sendSystemMessage(Component.translatable(KEY + "anchor_set", name(target)));
        return null;
    }

    private static int info(CommandSourceStack source, ServerPlayer p) {
        WolfState w = PackAccess.wolf(p);
        if (w.lineage() == Lineage.NONE) {
            source.sendFailure(error("not_werewolf"));
            return 0;
        }
        PackIndex index = PackIndex.get(p.serverLevel());
        MutableComponent out = Component.translatable(KEY + "info.header", name(p))
                .append("\n").append(Component.translatable(KEY + "info.rank", w.rank().name()));
        if (PackAccess.isAlpha(w.rank())) {
            out.append("\n").append(Component.translatable(KEY + "info.betas", names(p, index.betasOf(p.getUUID()))));
            out.append("\n").append(Component.translatable(KEY + "info.emissary", names(p, index.emissaryOf(p.getUUID()).stream().toList())));
        } else {
            out.append("\n").append(Component.translatable(KEY + "info.alpha", names(p, index.alphaOf(p.getUUID()).stream().toList())));
        }
        out.append("\n").append(Component.translatable(KEY + "info.anchor", names(p, index.anchorOf(p.getUUID()).stream().toList())));
        source.sendSuccess(() -> out, false);
        return 1;
    }

    private static Component names(ServerPlayer viewer, List<UUID> ids) {
        if (ids.isEmpty()) {
            return Component.translatable(KEY + "info.none");
        }
        return Component.literal(String.join(", ", ids.stream().map(id -> {
            String n = PackAccess.nameOf(viewer.server, id);
            return n != null ? n : id.toString().substring(0, 8);
        }).toList()));
    }
}
