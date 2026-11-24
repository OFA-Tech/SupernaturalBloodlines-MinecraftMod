package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupGate;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupText;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
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

    public static CallRequests allyRequests() {
        return ALLY_REQUESTS;
    }

    /** A player logged out: the invitations and alliance requests SENT TO them are void. Those they sent stay open until they run out, so an inviter who steps away is answered "offline" and the invitation survives (see {@link #accept}). */
    public static void forget(UUID player) {
        REQUESTS.forgetFor(player);
        ALLY_REQUESTS.forgetFor(player);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /pack is also a short word other mods use; /sb pack is the same tree under our own prefix (13d)
        com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> pack = d.register(Commands.literal("pack")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, invite(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("accept")
                        // no token: the hub's button answers the newest invitation made to the player
                        .executes(c -> run(c, acceptLatest(self(c))))
                        .then(Commands.argument("token", StringArgumentType.word())
                                .executes(c -> run(c, accept(self(c), StringArgumentType.getString(c, "token"))))))
                .then(Commands.literal("leave").executes(c -> run(c, leave(self(c)))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((c, b) -> suggest(c, b, false))
                                .executes(c -> run(c, kickNamed(self(c), StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("ally")
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> run(c, allyInvite(self(c), EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("token", StringArgumentType.word())
                                        .executes(c -> run(c, allyAccept(self(c), StringArgumentType.getString(c, "token"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests((c, b) -> suggest(c, b, true))
                                        .executes(c -> run(c, allyRemoveNamed(self(c), StringArgumentType.getString(c, "player")))))))
                .then(Commands.literal("challenge")
                        .then(Commands.literal("accept").executes(c -> run(c, PackDuel.accept(self(c)))))
                        .then(Commands.literal("decline").executes(c -> run(c, PackDuel.decline(self(c)))))
                        .then(Commands.argument("alpha", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    if (c.getSource().getEntity() instanceof ServerPlayer me) {
                                        PackIndex.get(me.serverLevel()).alphaOf(me.getUUID()).map(id -> PackAccess.nameOf(me.server, id)).ifPresent(b::suggest);
                                    }
                                    return b.buildFuture();
                                })
                                .executes(c -> run(c, PackDuel.challenge(self(c), StringArgumentType.getString(c, "alpha"))))))
                .then(Commands.literal("heal")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, heal(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("protect")
                        .then(Commands.literal("on").executes(c -> run(c, protect(self(c), true))))
                        .then(Commands.literal("off")
                                // turning it off can end the path, so the typed command asks to be told twice, as the hub asks first
                                .executes(c -> run(c, error("protect_confirm")))
                                .then(Commands.literal("confirm").executes(c -> run(c, protect(self(c), false))))))
                .then(Commands.literal("info").executes(c -> info(c.getSource(), self(c))))
                .then(Commands.literal("emissary")
                        .executes(c -> run(c, emissary(self(c), null)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, emissary(self(c), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("roar").executes(c -> run(c, AlphaRoar.roar(self(c)))))
                .then(Commands.literal("surge").executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.PackSkills.surge(self(c)))))
                .then(Commands.literal("spirits").executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.PackSkills.spirits(self(c)))))
                .then(Commands.literal("npc")
                        .then(Commands.literal("order")
                                .then(Commands.argument("token", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .then(Commands.argument("order", StringArgumentType.word())
                                                .suggests((c, b) -> {
                                                    for (net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order o : net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order.values()) {
                                                        b.suggest(o.name().toLowerCase(java.util.Locale.ROOT));
                                                    }
                                                    return b.buildFuture();
                                                })
                                                .executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands.order(self(c),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "token"), StringArgumentType.getString(c, "order")))))))
                        .then(Commands.literal("hunt")
                                .then(Commands.argument("token", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands.toggleHunt(self(c),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "token"))))))
                        .then(Commands.literal("sort")
                                .then(Commands.argument("token", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands.toggleSort(self(c),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "token"))))))
                        .then(Commands.literal("release")
                                .then(Commands.argument("token", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(c -> run(c, net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands.release(self(c),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "token"))))))
                        .then(Commands.literal("list").executes(c -> {
                            var rows = net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands.list(self(c));
                            if (rows.isEmpty()) {
                                c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.npc.list_none"), false);
                            }
                            rows.forEach(r -> c.getSource().sendSuccess(() -> r, false));
                            return rows.size();
                        })))
                .then(Commands.literal("anchor")
                        .executes(c -> run(c, anchor(self(c), null)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> run(c, anchor(self(c), EntityArgument.getPlayer(c, "player")))))));
        d.register(Commands.literal("sb").then(Commands.literal("pack").redirect(pack)));
    }

    /** Who /pack kick may name (the members of the pack) or /pack ally remove (the leaders of the allied packs): ids and names the pack already has, so no profile lookup. */
    private static Map<UUID, String> named(ServerPlayer leader, boolean allies) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        Map<UUID, String> out = new LinkedHashMap<>();
        for (UUID id : allies ? List.copyOf(index.alliesOf(leader.getUUID())) : index.betasOf(leader.getUUID())) {
            String n = PackAccess.nameOf(leader.server, id);
            out.put(id, n != null ? n : id.toString());
        }
        return out;
    }

    public static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> c, SuggestionsBuilder b, boolean allies) {
        if (c.getSource().getEntity() instanceof ServerPlayer me) {
            named(me, allies).values().forEach(b::suggest);
        }
        return b.buildFuture();
    }

    @Nullable
    private static UUID match(Map<UUID, String> candidates, String word) {
        return candidates.entrySet().stream().filter(e -> e.getKey().toString().equalsIgnoreCase(word) || e.getValue().equalsIgnoreCase(word))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    @Nullable
    public static Component kickNamed(ServerPlayer leader, String word) {
        Map<UUID, String> members = named(leader, false);
        UUID id = match(members, word);
        return id == null ? error("not_member", word) : kick(leader, id, members.get(id));
    }

    @Nullable
    public static Component allyRemoveNamed(ServerPlayer leader, String word) {
        Map<UUID, String> allies = named(leader, true);
        UUID id = match(allies, word);
        return id == null ? error("ally_none", word) : allyRemove(leader, id, allies.get(id));
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

    static Component error(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    static String name(ServerPlayer p) {
        return p.getGameProfile().getName();
    }

    /** A rank outside the werewolf faction is dormant (a cured True Alpha): it cannot lead a pack until they rejoin. */
    public static boolean isActiveAlpha(ServerPlayer p) {
        return PackAccess.isAlpha(PackAccess.wolf(p).rank()) && VampirismSupernatural.isWerewolf(p);
    }

    private static PackContext context(@Nullable UUID alpha, long now) {
        return new PackContext(alpha, 0, now, 0, 30L * 24000, 300, alpha != null);
    }

    @Nullable
    public static Component invite(ServerPlayer alpha, ServerPlayer target) {
        GroupType type = Groups.typeOf(alpha);
        Optional<String> gate = GroupGate.canInvite(alpha);
        if (gate.isPresent()) {
            return GroupText.error(type, gate.get());
        }
        PackIndex index = PackIndex.get(alpha.serverLevel());
        if (!index.isLeader(alpha.getUUID())) {
            return GroupText.error(type, "not_leader");
        }
        if (target == alpha) {
            return GroupText.error(type, "self");
        }
        // anyone can join: a werewolf as a Beta, an Alpha as a second Alpha, everyone else as a Packmate; only a player already in someone else's pack cannot
        if (index.alphaOf(target.getUUID()).isPresent() || PackAccess.wolf(target).packAlpha() != null) {
            return GroupText.error(type, "target_in_group", name(target));
        }
        long now = alpha.serverLevel().getGameTime();
        target.sendSystemMessage(inviteMessage(net.ofatech.supernaturalbloodlines.shared.supernatural.ProfileLink.name(name(alpha), alpha.getUUID()), REQUESTS.open(alpha.getUUID(), target.getUUID(), now), index.betasOf(target.getUUID()).size(), type));
        alpha.sendSystemMessage(GroupText.of(type, "invited", name(target)));
        HubRequests.changed(target);
        return null;
    }

    /** The invitation text: who asks, and (when the invited one leads a pack of their own) that it comes along. */
    public static Component inviteMessage(String alphaName, String token, int bringing) {
        return inviteMessage(Component.literal(alphaName), token, bringing);
    }

    public static Component inviteMessage(Component alphaName, String token, int bringing) {
        return inviteMessage(alphaName, token, bringing, GroupType.PACK);
    }

    public static Component inviteMessage(Component alphaName, String token, int bringing, GroupType type) {
        String command = "/group accept "; // /pack is not there in a world without Werewolves
        Component accept = GroupText.of(type, "accept").copy().withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GroupText.of(type, "accept_hover", alphaName))));
        MutableComponent text = GroupText.of(type, "invite", alphaName).copy();
        // a leader who joins brings their own pack along: they are told before they click
        if (bringing > 0) {
            text.append(" ").append(GroupText.of(type, "invite_merge", bringing));
        }
        return text.append(" ").append(accept);
    }

    /** {@code /pack accept} with no token: the newest open invitation to {@code p}. */
    @Nullable
    public static Component acceptLatest(ServerPlayer p) {
        String token = REQUESTS.latestFor(p.getUUID(), p.serverLevel().getGameTime()).orElse(null);
        return token == null ? error("expired") : accept(p, token);
    }

    @Nullable
    public static Component accept(ServerPlayer p, String token) {
        long now = p.serverLevel().getGameTime();
        CallRequests.Request r = REQUESTS.peek(token, p.getUUID(), now).orElse(null);
        if (r == null) {
            HubRequests.changed(p);
            return error("expired");
        }
        ServerPlayer alpha = SireCall.player(p.server, r.caller());
        if (alpha == null) {
            return GroupText.error(Groups.typeOfGroup(p.serverLevel(), r.caller()), "leader_offline"); // only offline: the invitation stays open for the rest of its minute
        }
        GroupType type = Groups.typeOf(alpha);
        if (REQUESTS.take(token, p.getUUID(), now).isEmpty()) {
            HubRequests.changed(p);
            return error("expired"); // answered, or run out, between the peek and the take
        }
        HubRequests.changed(p);
        Optional<String> gate = GroupGate.canLead(alpha);
        if (gate.isPresent()) {
            return GroupText.error(type, "leader_gone");
        }
        WolfState w = PackAccess.wolf(p);
        PackIndex index = PackIndex.get(p.serverLevel());
        // the inviter must still lead: a group that changed hands since the invitation is no longer the one they were asked to join
        if (!index.isLeader(alpha.getUUID())) {
            return GroupText.error(type, "stale_invite");
        }
        UUID leader = alpha.getUUID();
        // a Beta keeps their pack until /pack leave: a second invitation or bite never switches it
        if (w.packAlpha() != null || index.alphaOf(p.getUUID()).isPresent()) {
            return GroupText.error(type, "in_group");
        }
        if (leader.equals(p.getUUID())) {
            return GroupText.error(type, "self");
        }
        // a shunned witch, or a leader who would bring one, is refused with the one refusal everything else has: nothing says who is shunned
        if (type == GroupType.COVEN && (GroupGate.shunned(p.server, p.getUUID()) || index.betasOf(p.getUUID()).stream().anyMatch(id -> GroupGate.shunned(p.server, id)))) {
            return GroupText.error(type, "refused");
        }
        // the cap counts everybody who would be in the group afterwards: the joiner, and the members a joining leader brings
        List<UUID> own = index.betasOf(p.getUUID());
        int max = GroupsConfig.maxMembers();
        if (max > 0 && index.betasOf(leader).size() + 1 + own.size() > max) {
            return GroupText.error(type, "full");
        }
        GroupIndex groups = GroupIndex.get(p.serverLevel());
        if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
            if (PackAccess.applyPack(p, PackEvent.JOINED_PACK, context(leader, now)).refused()) {
                return GroupText.error(type, "refused");
            }
        } else {
            // an Alpha keeps the spark and a non-werewolf is a Packmate: the index alone holds them (a leader brings their members)
            if (!index.join(leader, p.getUUID())) {
                return GroupText.error(type, "refused");
            }
            own.forEach(id -> SparkHandler.repoint(p.server, id, leader));
            PackMembers.moved(p.server, own, leader);
            // the members who came along belong to another group's ranks no more
            own.forEach(groups::clearRanks);
        }
        groups.ensure(leader, type, PackIndex.now());
        PackLeader.refresh(p);
        PartyLink.sync(p.server, p.getUUID(), true);
        p.sendSystemMessage(GroupText.of(type, "joined", name(alpha)));
        alpha.sendSystemMessage(GroupText.of(type, "joined_leader", name(p)));
        return null;
    }

    @Nullable
    public static Component leave(ServerPlayer p) {
        WolfState w = PackAccess.wolf(p);
        PackIndex index = PackIndex.get(p.serverLevel());
        GroupType type = Groups.typeOf(p);
        boolean leads = PackAccess.isAlpha(w.rank()) || (type != GroupType.PACK && !index.betasOf(p.getUUID()).isEmpty());
        if (index.alphaOf(p.getUUID()).isEmpty() && leads) {
            return GroupText.error(type, "leader_cannot_leave");
        }
        if (index.alphaOf(p.getUUID()).isEmpty() && w.packAlpha() == null) {
            return GroupText.error(type, "no_group");
        }
        net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.dropMember(p.server, p.getUUID(), true);
        // a werewolf Beta's state change takes them out of the index by itself: the pack they leave, and the grace for it, are
        // noted first, or there would be neither
        UUID wasIn = index.leaderOf(p.getUUID());
        index.rememberLeft(p.getUUID());
        if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
            PackAccess.applyPack(p, PackEvent.LEFT_PACK, context(null, p.serverLevel().getGameTime()));
        }
        // a co-Alpha or a Packmate lives in the index only
        index.leaveVoluntarily(p.getUUID());
        PartyLink.sync(p.server, wasIn, true);
        p.sendSystemMessage(GroupText.of(type, "left"));
        GroupIndex.get(p.serverLevel()).clearRanks(p.getUUID());
        return null;
    }

    /** The leader removes a member, online or not. A werewolf Beta becomes an Omega (now, or at their next login). */
    @Nullable
    public static Component kick(ServerPlayer leader, UUID target, String targetName) {
        GroupType type = Groups.typeOf(leader);
        Optional<String> gate = GroupGate.canLead(leader);
        if (gate.isPresent()) {
            return GroupText.error(type, gate.get());
        }
        // somebody the leader is fighting for the lead cannot be sent away in the middle of it
        if (PackDuel.board().on(leader.getUUID()).map(d -> d.other(leader.getUUID()).equals(target)).orElse(false)) {
            return error("duel_no_kick", targetName);
        }
        PackIndex index = PackIndex.get(leader.serverLevel());
        if (!index.isLeader(leader.getUUID())) {
            return GroupText.error(type, "not_leader");
        }
        if (target.equals(leader.getUUID())) {
            return GroupText.error(type, "self");
        }
        if (!index.betasOf(leader.getUUID()).contains(target)) {
            return GroupText.error(type, "not_member", targetName);
        }
        PackMembers.remove(leader.server, target, GroupText.key(type, "kicked"), name(leader));
        net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.dropMember(leader.server, target, false);
        GroupIndex.get(leader.serverLevel()).clearRanks(target);
        leader.sendSystemMessage(GroupText.of(type, "kicked_ok", targetName));
        PartyLink.sync(leader.server, leader.getUUID(), true);
        return null;
    }

    /** One leader asks another to ally their packs. */
    @Nullable
    public static Component allyInvite(ServerPlayer leader, ServerPlayer target) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        GroupType type = Groups.typeOf(leader);
        if (GroupGate.canLead(leader).isPresent() || !index.isLeader(leader.getUUID())) {
            return GroupText.error(type, "not_leader");
        }
        if (target == leader) {
            return GroupText.error(type, "self");
        }
        if (!index.isLeader(target.getUUID()) || GroupGate.canLead(target).isPresent()) {
            return GroupText.error(type, "ally_not_leader", name(target));
        }
        if (index.alliesOf(leader.getUUID()).contains(target.getUUID())) {
            return GroupText.error(type, "ally_already", name(target));
        }
        String token = ALLY_REQUESTS.open(leader.getUUID(), target.getUUID(), leader.serverLevel().getGameTime());
        String command = "/group ally accept ";
        Component accept = GroupText.of(type, "accept").copy().withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + token)));
        target.sendSystemMessage(GroupText.of(type, "ally_invite", net.ofatech.supernaturalbloodlines.shared.supernatural.ProfileLink.name(name(leader), leader.getUUID())).copy().append(" ").append(accept));
        leader.sendSystemMessage(GroupText.of(type, "ally_invited", name(target)));
        HubRequests.changed(target);
        return null;
    }

    @Nullable
    public static Component allyAccept(ServerPlayer p, String token) {
        long now = p.serverLevel().getGameTime();
        CallRequests.Request r = ALLY_REQUESTS.peek(token, p.getUUID(), now).orElse(null);
        if (r == null) {
            HubRequests.changed(p);
            return error("expired");
        }
        ServerPlayer other = SireCall.player(p.server, r.caller());
        if (other == null) {
            return error("alpha_offline"); // only offline: the request stays open for the rest of its minute
        }
        if (ALLY_REQUESTS.take(token, p.getUUID(), now).isEmpty()) {
            HubRequests.changed(p);
            return error("expired");
        }
        HubRequests.changed(p);
        PackIndex index = PackIndex.get(p.serverLevel());
        GroupType mine = Groups.typeOf(p);
        GroupType theirs = Groups.typeOf(other);
        if (GroupGate.canLead(other).isPresent() || GroupGate.canLead(p).isPresent()) {
            return GroupText.error(mine, "leader_gone");
        }
        // both must still lead the groups the alliance was asked between
        if (!index.isLeader(other.getUUID()) || !index.isLeader(p.getUUID())) {
            return GroupText.error(mine, "stale_invite");
        }
        if (!index.ally(p.getUUID(), other.getUUID())) {
            return GroupText.error(mine, "refused");
        }
        p.sendSystemMessage(GroupText.of(mine, "ally_made", name(other)));
        other.sendSystemMessage(GroupText.of(theirs, "ally_made", name(p)));
        return null;
    }

    /** Either leader ends an alliance with the pack {@code target} is in. */
    @Nullable
    public static Component allyRemove(ServerPlayer leader, UUID target, String targetName) {
        PackIndex index = PackIndex.get(leader.serverLevel());
        GroupType type = Groups.typeOf(leader);
        if (GroupGate.canLead(leader).isPresent() || !index.isLeader(leader.getUUID())) {
            return GroupText.error(type, "not_leader");
        }
        if (!index.unally(leader.getUUID(), index.leaderOf(target))) {
            return GroupText.error(type, "ally_none", targetName);
        }
        leader.sendSystemMessage(GroupText.of(type, "ally_ended", targetName));
        return null;
    }

    /** An Alpha gives up the spark to fully heal a player within 4 blocks and cure their wolfsbane and hybrid venom (R93). The spark is destroyed: nobody inherits it. A second Alpha stays a Beta of the leader, a leader hands the pack to the strongest other Alpha and stays as its Beta, and with no other Alpha the pack dissolves and the giver is an Omega. */
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

    /**
     * "Protect my True Alpha path": while the path is open, no spark is forced on the player (a kill that would steal one destroys it; a pack
     * inheritance is not covered). On by default. It can always be changed; it only does anything while the path is open.
     */
    @Nullable
    public static Component protect(ServerPlayer p, boolean on) {
        WolfState w = PackAccess.wolf(p);
        if (w.lineage() == Lineage.NONE) {
            return error("protect_none"); // no werewolf, no path: nothing is changed
        }
        if (w.protectPath() != on) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().protectPath(on).build()).build());
        }
        p.sendSystemMessage(Component.translatable(KEY + (on ? "protect_on" : "protect_off")));
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
        if (!index.isLeader(alpha.getUUID())) {
            return error("not_leader");
        }
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
        // only someone who is in the pack already (invite them first, as a Packmate): the Emissary is a member, never pulled in by a command
        if (!index.pack(alpha.getUUID()).contains(target.getUUID())) {
            return error("emissary_not_member", name(target));
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

    public static int info(CommandSourceStack source, ServerPlayer p) {
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
