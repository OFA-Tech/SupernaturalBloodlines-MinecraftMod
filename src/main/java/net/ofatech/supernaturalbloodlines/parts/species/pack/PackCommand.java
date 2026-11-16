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
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
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
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /pack invite|accept|leave|heal|info|emissary|anchor|roar} ({@code roar} is the keybind's server method). An invitation works like the sireline call: a clickable
 * [Accept] with a token bound to the invited player, single use, valid for a minute. Each action returns the error to
 * show, or null when it worked (the messages to the people involved are sent here). Registered only with Werewolves.
 */
public final class PackCommand {
    private static final String KEY = "supernaturalbloodlines.pack.";
    private static final CallRequests REQUESTS = new CallRequests();
    private static final CallRequests ALLY_REQUESTS = new CallRequests();
    private static final double HEAL_RANGE = 4;

    private PackCommand() {
    }

    public static CallRequests requests() {
        return REQUESTS;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /pack is also a short word other mods use; /sb pack is the same tree under our own prefix (13d)
        com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> pack = d.register(Commands.literal("pack")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, invite(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("accept")
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> run(c, accept(self(c), StringArgumentType.getString(c, "token"))))))
                .then(Commands.literal("leave").executes(c -> run(c, leave(self(c)))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(c -> {
                                    GameProfile g = GameProfileArgument.getGameProfiles(c, "player").iterator().next();
                                    return run(c, kick(self(c), g.getId(), g.getName()));
                                })))
                .then(Commands.literal("ally")
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> run(c, allyInvite(self(c), EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("token", StringArgumentType.word())
                                        .executes(c -> run(c, allyAccept(self(c), StringArgumentType.getString(c, "token"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(c -> {
                                            GameProfile g = GameProfileArgument.getGameProfiles(c, "player").iterator().next();
                                            return run(c, allyRemove(self(c), g.getId(), g.getName()));
                                        }))))
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
        d.register(Commands.literal("sb").then(Commands.literal("pack").redirect(pack)));
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

    /** A rank outside the werewolf faction is dormant (a cured True Alpha): it cannot lead a pack until they rejoin. */
    private static boolean isActiveAlpha(ServerPlayer p) {
        return PackAccess.isAlpha(PackAccess.wolf(p).rank()) && VampirismSupernatural.isWerewolf(p);
    }

    private static PackContext context(@Nullable UUID alpha, long now) {
        return new PackContext(alpha, 0, now, 0, 30L * 24000, 300, alpha != null);
    }

    @Nullable
    public static Component invite(ServerPlayer alpha, ServerPlayer target) {
        if (!isActiveAlpha(alpha)) {
            return error("not_alpha");
        }
        PackIndex index = PackIndex.get(alpha.serverLevel());
        if (!index.leaderOf(alpha.getUUID()).equals(alpha.getUUID())) {
            return error("not_leader");
        }
        if (target == alpha) {
            return error("self");
        }
        // anyone can join: a werewolf as a Beta, an Alpha as a second Alpha, everyone else as a Packmate; only a player already in someone else's pack cannot
        if (index.alphaOf(target.getUUID()).isPresent() || PackAccess.wolf(target).packAlpha() != null) {
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
        if (alpha == null || !isActiveAlpha(alpha)) {
            return error("alpha_gone");
        }
        WolfState w = PackAccess.wolf(p);
        PackIndex index = PackIndex.get(p.serverLevel());
        UUID leader = index.leaderOf(alpha.getUUID());
        // a Beta keeps their pack until /pack leave: a second invitation or bite never switches it
        if (w.packAlpha() != null || index.alphaOf(p.getUUID()).isPresent()) {
            return error("has_pack");
        }
        if (leader.equals(p.getUUID())) {
            return error("self");
        }
        if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
            if (PackAccess.applyPack(p, PackEvent.JOINED_PACK, context(leader, now)).refused()) {
                return error("refused");
            }
        } else {
            // an Alpha keeps the spark and a non-werewolf is a Packmate: the index alone holds them (a leader brings their members)
            List<UUID> own = index.betasOf(p.getUUID());
            if (!index.join(leader, p.getUUID())) {
                return error("refused");
            }
            own.forEach(id -> SparkHandler.repoint(p.server, id, leader));
        }
        PackLeader.refresh(p);
        PartyLink.sync(p.server, p.getUUID(), true);
        p.sendSystemMessage(Component.translatable(KEY + "joined", name(alpha)));
        alpha.sendSystemMessage(Component.translatable(KEY + "joined_alpha", name(p)));
        return null;
    }

    @Nullable
    public static Component leave(ServerPlayer p) {
        WolfState w = PackAccess.wolf(p);
        PackIndex index = PackIndex.get(p.serverLevel());
        if (index.alphaOf(p.getUUID()).isEmpty() && PackAccess.isAlpha(w.rank())) {
            return error("alpha_cannot_leave");
        }
        if (index.alphaOf(p.getUUID()).isEmpty() && w.packAlpha() == null) {
            return error("no_pack");
        }
        if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
            PackAccess.applyPack(p, PackEvent.LEFT_PACK, context(null, p.serverLevel().getGameTime()));
        }
        // a co-Alpha or a Packmate lives in the index only
        UUID wasIn = index.leaderOf(p.getUUID());
        index.leave(p.getUUID());
        PartyLink.sync(p.server, wasIn, true);
        p.sendSystemMessage(Component.translatable(KEY + "left"));
        return null;
    }

    /** The leader removes a member, online or not. A werewolf Beta becomes an Omega (now, or at their next login). */
    @Nullable
    public static Component kick(ServerPlayer leader, UUID target, String targetName) {
        if (!isActiveAlpha(leader)) {
            return error("not_alpha");
        }
        PackIndex index = PackIndex.get(leader.serverLevel());
        if (!index.leaderOf(leader.getUUID()).equals(leader.getUUID())) {
            return error("not_leader");
        }
        if (target.equals(leader.getUUID())) {
            return error("self");
        }
        if (!index.betasOf(leader.getUUID()).contains(target)) {
            return error("not_member", targetName);
        }
        PackMembers.remove(leader.server, target, Component.translatable(KEY + "kicked", name(leader)));
        leader.sendSystemMessage(Component.translatable(KEY + "kicked_ok", targetName));
        PartyLink.sync(leader.server, leader.getUUID(), true);
        return null;
    }

    /** One leader asks another to ally their packs. */
    @Nullable
    public static Component allyInvite(ServerPlayer leader, ServerPlayer target) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        if (!isActiveAlpha(leader) || !index.leaderOf(leader.getUUID()).equals(leader.getUUID())) {
            return error("not_leader");
        }
        if (target == leader) {
            return error("self");
        }
        if (!index.leaderOf(target.getUUID()).equals(target.getUUID()) || !isActiveAlpha(target)) {
            return error("ally_not_leader", name(target));
        }
        if (index.alliesOf(leader.getUUID()).contains(target.getUUID())) {
            return error("ally_already", name(target));
        }
        String token = ALLY_REQUESTS.open(leader.getUUID(), target.getUUID(), leader.serverLevel().getGameTime());
        Component accept = Component.translatable(KEY + "accept").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pack ally accept " + token)));
        target.sendSystemMessage(Component.translatable(KEY + "ally_invite", name(leader)).append(" ").append(accept));
        leader.sendSystemMessage(Component.translatable(KEY + "ally_invited", name(target)));
        return null;
    }

    @Nullable
    public static Component allyAccept(ServerPlayer p, String token) {
        long now = p.serverLevel().getGameTime();
        CallRequests.Request r = ALLY_REQUESTS.take(token, p.getUUID(), now).orElse(null);
        if (r == null) {
            return error("expired");
        }
        ServerPlayer other = SireCall.player(p.server, r.caller());
        PackIndex index = PackIndex.get(p.serverLevel());
        if (other == null || !isActiveAlpha(other) || !isActiveAlpha(p)) {
            return error("alpha_gone");
        }
        if (!index.ally(index.leaderOf(p.getUUID()), index.leaderOf(other.getUUID()))) {
            return error("refused");
        }
        p.sendSystemMessage(Component.translatable(KEY + "ally_made", name(other)));
        other.sendSystemMessage(Component.translatable(KEY + "ally_made", name(p)));
        return null;
    }

    /** Either leader ends an alliance with the pack {@code target} is in. */
    @Nullable
    public static Component allyRemove(ServerPlayer leader, UUID target, String targetName) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        if (!isActiveAlpha(leader) || !index.leaderOf(leader.getUUID()).equals(leader.getUUID())) {
            return error("not_leader");
        }
        if (!index.unally(leader.getUUID(), index.leaderOf(target))) {
            return error("ally_none", targetName);
        }
        leader.sendSystemMessage(Component.translatable(KEY + "ally_ended", targetName));
        return null;
    }

    /** An Alpha gives up the spark to fully heal a player within 4 blocks and cure their wolfsbane and hybrid venom (R93). With no online heir the pack dissolves and the giver is left an Omega. */
    @Nullable
    public static Component heal(ServerPlayer alpha, ServerPlayer target) {
        if (!isActiveAlpha(alpha)) {
            return error("not_alpha");
        }
        if (PackAccess.wolf(alpha).rank() == WolfState.Rank.TRUE_ALPHA) {
            return error("true_alpha_keeps_spark");
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
        if (!isActiveAlpha(alpha)) {
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
        // one Alpha at a time: a witch who already serves another pack must be dropped there first
        if (index.emissaryAlpha(target.getUUID()).filter(a -> !a.equals(alpha.getUUID())).isPresent()) {
            return error("emissary_taken", name(target));
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
        PackIndex index = PackIndex.get(p.serverLevel());
        if (w.lineage() == Lineage.NONE && !index.inPack(p.getUUID())) {
            source.sendFailure(error("not_werewolf"));
            return 0;
        }
        MutableComponent out = Component.translatable(KEY + "info.header", name(p))
                .append("\n").append(Component.translatable(KEY + "info.rank", w.rank().name()));
        UUID leader = index.leaderOf(p.getUUID());
        if (leader.equals(p.getUUID())) {
            out.append("\n").append(Component.translatable(KEY + "info.betas", names(p, index.betasOf(p.getUUID()))));
            out.append("\n").append(Component.translatable(KEY + "info.emissary", names(p, index.emissaryOf(p.getUUID()).stream().toList())));
        } else {
            out.append("\n").append(Component.translatable(KEY + "info.alpha", names(p, List.of(leader))));
        }
        out.append("\n").append(Component.translatable(KEY + "info.allies", names(p, List.copyOf(index.alliesOf(leader)))));
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
