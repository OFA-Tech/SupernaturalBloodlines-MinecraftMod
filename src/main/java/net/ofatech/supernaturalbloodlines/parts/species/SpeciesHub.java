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
final class SpeciesHub {
    private SpeciesHub() {
    }

    static void register() {
        HubSocial.set(SpeciesHub::social);
        HubRequests.set(SpeciesHub::requests);
    }

    static HubSocial.Social social(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        SirelineIndex sirelines = SirelineIndex.get(p.serverLevel());
        UUID sireId = BloodlineAccess.get(p).sire();
        // a creature sire is "?" like a creature ancestor: an empty name
        Optional<String> sire = sireId == null ? Optional.empty()
                : isCreature(sirelines, sireId) ? Optional.of("")
                : sirelines.nameOf(sireId).or(() -> Optional.ofNullable(PackAccess.nameOf(server, sireId)));
        List<String> sired = sirelines.childrenOf(p.getUUID()).stream().map(SirelineIndex.Node::name).toList();
        List<HubSocial.Kin> line = SirelineTree.below(sirelines, p.getUUID()).stream().limit(MAX_LINE)
                .map(l -> new HubSocial.Kin(l.name(), l.depth())).toList();
        List<String> names = new ArrayList<>();
        List<Boolean> hidden = new ArrayList<>();
        ancestors(server, sirelines, sireId, names, hidden);
        List<HubSocial.Member> pack = ModList.get().isLoaded("werewolves") ? pack(p, server) : List.of();
        List<HubSocial.Person> people = new ArrayList<>();
        sire.filter(n -> !n.isEmpty() && sireId != null).ifPresent(n -> people.add(new HubSocial.Person(n, sireId)));
        sirelines.childrenOf(p.getUUID()).forEach(c -> people.add(new HubSocial.Person(c.name(), c.id())));
        return new HubSocial.Social(sire, sired, pack, FiveRoster.get(p.serverLevel()).size(), names, line, rites(server), origin(p, hidden), people);
    }

    static final int MAX_LINE = 64;

    /** The requests still open to the player, for the hub's Requests card: who asked, the answer's token and the ticks left. */
    static List<HubRequests.Request> requests(ServerPlayer p) {
        MinecraftServer server = p.server;
        long now = p.serverLevel().getGameTime();
        List<HubRequests.Request> out = new ArrayList<>();
        tokens(out, HubRequests.Kind.SIRE_CALL, SireCall.requests(), p, server, now);
        if (ModList.get().isLoaded("werewolves")) {
            tokens(out, HubRequests.Kind.PACK_INVITE, PackCommand.requests(), p, server, now);
            tokens(out, HubRequests.Kind.ALLY_INVITE, PackCommand.allyRequests(), p, server, now);
            PackDuel.board().asked(p.getUUID(), now).ifPresent(d -> {
                String name = PackAccess.nameOf(server, d.challenger());
                out.add(new HubRequests.Request(HubRequests.Kind.CHALLENGE, d.challenger(), name == null ? "" : name, "", left(d.expires(), now)));
            });
        }
        if (ModList.get().isLoaded("werewolves")) {
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
            boolean creature = isCreature(sirelines, id);
            names.add(creature ? "" : sirelines.nameOf(id).or(() -> Optional.ofNullable(PackAccess.nameOf(server, id))).orElse(""));
            hidden.add(creature);
            at = sirelines.sireOf(id).orElse(null);
        }
    }

    /** Recorded as a creature, or one of the six NPC Originals. The top of a chain has no link of its own and no recorded type: it is taken for a player, as before. */
    private static boolean isCreature(SirelineIndex sirelines, UUID id) {
        return sirelines.isCreature(id);
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

    /** This mod's rites the server loaded (their part and mods are on), read back through Enchanted's own codec. */
    private static List<HubSocial.Rite> rites(MinecraftServer server) {
        List<HubSocial.Rite> out = new ArrayList<>();
        var registry = server.registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY);
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals(SupernaturalBloodlines.MODID)) {
                continue;
            }
            RiteType rite = entry.getValue();
            List<String> items = rite.getItems().stream().map(s -> s.getCount() + " " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem())).toList();
            List<String> shapes = rite.getShapes().keySet().stream().map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("?")).sorted().toList();
            // the factory and its settings have no getters: read them back through Enchanted's own codec (a factory that cannot encode shows none)
            JsonObject factory = RiteType.CODEC.encodeStart(ops, rite).result().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                    .filter(j -> j.has("factory") && j.get("factory").isJsonObject()).map(j -> j.getAsJsonObject("factory")).orElse(new JsonObject());
            out.add(new HubSocial.Rite(id.toString(), rite.getPower(), items, shapes, string(factory, "type"), string(factory, "faction")));
        }
        out.sort(java.util.Comparator.comparing(HubSocial.Rite::id));
        return out;
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static List<HubSocial.Member> pack(ServerPlayer p, MinecraftServer server) {
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
}
