package net.ofatech.supernaturalbloodlines.parts.species;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineTree;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

/** The hub's view of other players from this part's indexes: names, ranks and online flags only. */
public final class SpeciesHub {
    private SpeciesHub() {
    }

    static void register() {
        HubSocial.set(SpeciesHub::social);
        HubSocial.setNpcs(SpeciesHub::npcs);
        // with moon_bound_werewolves off nobody needs control, so there is no perk to show
        HubSocial.setMoonGift(p -> net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl.moonBound()
                && net.ofatech.supernaturalbloodlines.parts.species.pack.PackMoonGift.has(p));
        HubRequests.set(SpeciesHub::requests);
        net.ofatech.supernaturalbloodlines.shared.supernatural.HubGroups.set(net.ofatech.supernaturalbloodlines.parts.species.group.GroupViews::of);
        net.ofatech.supernaturalbloodlines.shared.supernatural.HubCoven.set(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenViews::of);
        net.ofatech.supernaturalbloodlines.shared.supernatural.HubClan.set(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanViews::of);
    }

    static HubSocial.Social social(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        SirelineIndex sirelines = SirelineIndex.get(p.serverLevel());
        UUID sireId = BloodlineAccess.get(p).sire();
        // a creature sire (or one whose name is not known) is "?" like a creature ancestor: an empty name, so the page knows there is a sire
        Optional<String> sire = sireId == null ? Optional.empty()
                : sirelines.isCreature(sireId) ? Optional.of("")
                : sirelines.nameOf(sireId).or(() -> Optional.ofNullable(PackAccess.nameOf(server, sireId))).or(() -> Optional.of(""));
        List<String> sired = sirelines.childrenOf(p.getUUID()).stream().map(SirelineIndex.Node::name).toList();
        List<HubSocial.Kin> line = SirelineTree.below(sirelines, p.getUUID()).stream().limit(MAX_LINE)
                .map(l -> new HubSocial.Kin(l.name(), l.depth())).toList();
        List<String> names = new ArrayList<>();
        List<Boolean> hidden = new ArrayList<>();
        ancestors(server, sirelines, sireId, names, hidden);
        List<HubSocial.Member> pack = pack(p, server);
        List<HubSocial.Person> people = new ArrayList<>();
        sire.filter(n -> !n.isEmpty() && sireId != null).ifPresent(n -> people.add(new HubSocial.Person(n, sireId)));
        sirelines.childrenOf(p.getUUID()).forEach(c -> people.add(new HubSocial.Person(c.name(), c.id())));
        return new HubSocial.Social(sire, sired, pack, FiveRoster.get(p.serverLevel()).size(), names, line, origin(p, hidden), people);
    }

    static final int MAX_LINE = 64;

    /** The pack, its requests and its duels exist only with the Werewolves mod. */
    private static boolean werewolves() {
        return ModList.get().isLoaded("werewolves");
    }

    /** The requests still open to the player, for the hub's Requests card: who asked, the answer's token and the ticks left. */
    static List<HubRequests.Request> requests(ServerPlayer p) {
        MinecraftServer server = p.server;
        long now = p.serverLevel().getGameTime();
        List<HubRequests.Request> out = new ArrayList<>();
        tokens(out, HubRequests.Kind.SIRE_CALL, SireCall.requests(), p, server, now);
        tokens(out, HubRequests.Kind.PACK_INVITE, PackCommand.requests(), p, server, now);
        tokens(out, HubRequests.Kind.ALLY_INVITE, PackCommand.allyRequests(), p, server, now);
        out.addAll(net.ofatech.supernaturalbloodlines.parts.species.group.clan.Contests.requestsFor(p));
        tokens(out, HubRequests.Kind.CIRCLE_CALL, net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink.calls(), p, server, now);
        out.addAll(net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenGovernance.cardsFor(p));
        if (werewolves()) {
            PackDuel.board().asked(p.getUUID(), now).ifPresent(d -> {
                String name = PackAccess.nameOf(server, d.challenger());
                out.add(new HubRequests.Request(HubRequests.Kind.CHALLENGE, d.challenger(), name == null ? "" : name, "", left(d.expires(), now)));
            });
            PackDuel.board().all().stream().filter(d -> d.involves(p.getUUID())).findFirst().ifPresent(d -> {
                UUID other = d.other(p.getUUID());
                boolean asked = d.phase() == net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard.Phase.ASKED;
                if (asked && d.leader().equals(p.getUUID())) {
                    return; // the one asked sees the challenge as a request
                }
                String name = PackAccess.nameOf(server, other);
                String token = d.paused() ? "paused" : asked ? "asked" : "on";
                long until = d.paused() ? d.awayEnd(d.pausedBy()) : d.expires();
                out.add(new HubRequests.Request(HubRequests.Kind.DUEL, other, name == null ? "" : name, token, left(until, now)));
            });
        }
        for (var choice : Compulsion.openChoices(p.getUUID(), now)) {
            out.add(new HubRequests.Request(HubRequests.Kind.COMPULSION, HubRequests.NOBODY, "", choice.getKey(), left(choice.getValue(), now)));
        }
        return out;
    }

    private static void tokens(List<HubRequests.Request> out, HubRequests.Kind kind, CallRequests requests, ServerPlayer p, MinecraftServer server, long now) {
        for (var open : requests.openFor(p.getUUID(), now)) {
            String name = PackAccess.nameOf(server, open.getValue().caller());
            out.add(new HubRequests.Request(kind, open.getValue().caller(), name == null ? "" : name, open.getKey(), left(open.getValue().expires(), now)));
        }
    }

    private static int left(long expires, long now) {
        return (int) Math.max(0, Math.min(expires - now, Integer.MAX_VALUE));
    }

    /**
     * The sire's sire and up, nearest first; a cycle (a sire turned by their own descendant) ends the climb. An ancestor recorded as a
     * creature is hidden: drawn as "?" and sent with no name.
     */
    private static void ancestors(MinecraftServer server, SirelineIndex sirelines, UUID sire, List<String> names, List<Boolean> hidden) {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        UUID at = sire == null ? null : sirelines.sireOf(sire).orElse(null);
        while (at != null && names.size() < HubSocial.MAX_ANCESTORS && seen.add(at)) {
            UUID id = at;
            boolean creature = sirelines.isCreature(id);
            names.add(creature ? "" : sirelines.nameOf(id).or(() -> Optional.ofNullable(PackAccess.nameOf(server, id))).orElse(""));
            hidden.add(creature);
            at = sirelines.sireOf(id).orElse(null);
        }
    }

    /** Where the player stands in a line (R120 to R127): the root only when they have discovered it, and never an undiscovered name. */
    private static HubSocial.Origin origin(ServerPlayer p, List<Boolean> ancestorHidden) {
        MinecraftServer server = p.server;
        LineIndex index = LineIndex.get(server);
        Optional<LineIndex.Freed> freed = index.freedOf(p.getUUID());
        boolean original = OriginalRules.isOriginal(BloodlineAccess.get(p).species());
        LineRules.State state = LineRules.state(original, Supernatural.isVampiric(p), freed.isPresent(), index.rootOf(p.getUUID()).isPresent());
        UUID known = BloodlineAccess.get(p).knownLine();
        boolean always = SpeciesConfig.originalVisibilityAlways();
        Optional<String> root = state == LineRules.State.IN_LINE ? Lines.visibleRoot(p).map(r -> rootName(server, r)) : Optional.empty();
        Optional<String> freedFrom = freed.filter(f -> LineRules.showsRoot(known, f.from(), always)).map(f -> rootName(server, f.from()));
        HubSocial.FreedKind kind = freed.filter(f -> state == LineRules.State.FREE).map(f -> f.kind() == LineIndex.Kind.IMPRISONED ? HubSocial.FreedKind.IMPRISONED : HubSocial.FreedKind.BROKEN_LINE)
                .orElse(HubSocial.FreedKind.NONE);
        return new HubSocial.Origin(state, root, state == LineRules.State.FREE ? freedFrom : Optional.empty(), kind, ancestorHidden);
    }

    /** An NPC Original goes as {@code npc:<id>} (the client translates it); a player's name as it is. */
    private static String rootName(MinecraftServer server, UUID root) {
        return NpcOriginal.byUuid(root).map(o -> "npc:" + o.id()).orElseGet(() -> Lines.rootName(server, root).getString());
    }

    /** The factory and its settings have no getters: read them back through Enchanted's own codec (a factory that cannot encode shows none). */
    public static JsonObject factoryOf(net.minecraft.core.HolderLookup.Provider registries, RiteType rite) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        return RiteType.CODEC.encodeStart(ops, rite).result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                .filter(j -> j.has("factory") && j.get("factory").isJsonObject()).map(j -> j.getAsJsonObject("factory")).orElse(new JsonObject());
    }

    /** {@code requires_full_moon} of a rite's factory where its codec defaults it to true and always writes it (Severance, the Alpha Trial): absent means true. */
    public static boolean requiresFullMoon(JsonObject factory) {
        return !factory.has("requires_full_moon") || !factory.get("requires_full_moon").isJsonPrimitive() || factory.get("requires_full_moon").getAsBoolean();
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    public static List<HubSocial.Member> pack(ServerPlayer p, MinecraftServer server) {
        PackIndex index = PackIndex.get(p.serverLevel());
        // a member has a leader, an emissary serves one, and anyone else is the leader of their own pack (if it has anyone)
        UUID alpha = index.alphaOf(p.getUUID()).or(() -> index.emissaryAlpha(p.getUUID())).orElse(p.getUUID());
        List<UUID> betas = index.betasOf(alpha);
        Optional<UUID> emissary = index.emissaryOf(alpha);
        if (betas.isEmpty() && emissary.isEmpty()) {
            return List.of(); // no pack: a lone wolf is not an Alpha of anyone
        }
        // the leader is told who is asking for the lead, so the hub can offer to accept or decline
        UUID asking = alpha.equals(p.getUUID())
                ? net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel.board().asked(p.getUUID(), p.serverLevel().getGameTime()).map(d -> d.challenger()).orElse(null) : null;
        List<HubSocial.Member> out = new ArrayList<>();
        out.add(member(server, alpha, "alpha" + sparks(index, alpha)));
        // the other Alphas are Alphas, a member who was no werewolf when last seen is a Packmate, the rest are Betas
        betas.forEach(b -> out.add(member(server, b, emissary.filter(b::equals).isPresent() ? "emissary"
                : b.equals(asking) ? "coalpha_challenger" + sparks(index, b) : PackLeadership.isAlpha(index.strengthOf(b)) ? "coalpha" + sparks(index, b) : index.strengthOf(b) < 0 ? "packmate" : "beta")));
        emissary.filter(e -> !betas.contains(e)).ifPresent(e -> out.add(member(server, e, "emissary")));
        // the leaders of the packs this one is allied with (not their members)
        index.alliesOf(alpha).forEach(a -> out.add(member(server, a, "ally")));
        return out;
    }

    /** ":3" for an Alpha with three sparks, nothing for one. */
    private static String sparks(PackIndex index, UUID id) {
        int n = index.sparksOf(id);
        return n > 1 ? ":" + n : "";
    }

    private static HubSocial.Member member(MinecraftServer server, UUID id, String rank) {
        String name = PackAccess.nameOf(server, id);
        return new HubSocial.Member(name == null ? id.toString() : name, rank, server.getPlayerList().getPlayer(id) != null, Optional.of(id));
    }

    /** The NPC members of the whole pack the viewer is in (a lone werewolf has their own), as the Pack page and the NPC profile show them. */
    static List<HubSocial.Npc> npcs(ServerPlayer p) {
        if (!werewolves()) {
            return List.of();
        }
        var level = p.serverLevel();
        PackIndex packs = PackIndex.get(level);
        List<HubSocial.Npc> out = new ArrayList<>();
        for (var e : net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers.ofPack(level, packs.pack(p.getUUID()))) {
            var mob = net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers.live(p.server, e.npc());
            var bond = mob.flatMap(net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers::bondOf);
            boolean loaded = mob.isPresent();
            boolean here = loaded && mob.get().level() == p.level();
            String ownerName = PackAccess.nameOf(p.server, e.owner());
            boolean canOrder = net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules.mayOrder(e.owner().equals(p.getUUID()), packs.isLeader(p.getUUID()) && packs.samePackNow(p.getUUID(), e.owner()));
            boolean tensura = e.source() == net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Source.TENSURA;
            long since = bond.map(net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond::since).orElse(-1L);
            out.add(new HubSocial.Npc(e.token(), e.name(), e.type(), tensura ? "subordinate" : "brother",
                    tensura ? "" : bond.map(b -> b.order().name().toLowerCase(java.util.Locale.ROOT)).orElse("rest"),
                    bond.map(net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond::hunt).orElse(false), bond.map(net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond::sort).orElse(false),
                    loaded, here ? (int) Math.round(mob.get().distanceTo(p)) : -1, loaded ? Math.round(100f * mob.get().getHealth() / Math.max(1f, mob.get().getMaxHealth())) : -1,
                    since < 0 ? -1 : (int) Math.max(0, (level.getGameTime() - since) / 24000L), ownerName == null ? "" : ownerName, canOrder, e.owner().equals(p.getUUID()),
                    net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers.overCap(level, e.npc()) ? 0
                            : (int) Math.round(100 * net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules.weight(e.source(), SpeciesConfig.members().weightBrother(), SpeciesConfig.members().weightTensura()))));
        }
        return out;
    }
}