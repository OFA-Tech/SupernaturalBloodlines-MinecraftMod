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
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** The hub's view of other players from this part's indexes: names, ranks and online flags only. */
final class SpeciesHub {
    private SpeciesHub() {
    }

    static void register() {
        HubSocial.set(SpeciesHub::social);
    }

    static HubSocial.Social social(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        SirelineIndex sirelines = SirelineIndex.get(p.serverLevel());
        UUID sireId = BloodlineAccess.get(p).sire();
        Optional<String> sire = sireId == null ? Optional.empty()
                : sirelines.nameOf(sireId).or(() -> Optional.ofNullable(PackAccess.nameOf(server, sireId)));
        List<String> sired = sirelines.childrenOf(p.getUUID()).stream().map(SirelineIndex.Node::name).toList();
        List<HubSocial.Kin> line = SirelineTree.below(sirelines, p.getUUID()).stream().limit(MAX_LINE)
                .map(l -> new HubSocial.Kin(l.name(), l.depth())).toList();
        return new HubSocial.Social(sire, sired, ModList.get().isLoaded("werewolves") ? pack(p, server) : List.of(),
                FiveRoster.get(p.serverLevel()).size(), ancestors(server, sirelines, sireId), line, rites(server));
    }

    static final int MAX_ANCESTORS = 8;
    static final int MAX_LINE = 64;

    /** The sire's sire and up, nearest first; a cycle (a sire turned by their own descendant) ends the climb. */
    private static List<String> ancestors(MinecraftServer server, SirelineIndex sirelines, UUID sire) {
        List<String> out = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        UUID at = sire == null ? null : sirelines.sireOf(sire).orElse(null);
        while (at != null && out.size() < MAX_ANCESTORS && seen.add(at)) {
            UUID id = at;
            out.add(sirelines.nameOf(id).or(() -> Optional.ofNullable(PackAccess.nameOf(server, id))).orElse(""));
            at = sirelines.sireOf(id).orElse(null);
        }
        return out;
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
        // a Beta has an Alpha, an emissary serves one, and anyone else is the Alpha of their own pack (if it has anyone)
        UUID alpha = index.alphaOf(p.getUUID()).or(() -> index.emissaryAlpha(p.getUUID())).orElse(p.getUUID());
        List<UUID> betas = index.betasOf(alpha);
        Optional<UUID> emissary = index.emissaryOf(alpha);
        if (betas.isEmpty() && emissary.isEmpty()) {
            return List.of(); // no pack: a lone wolf is not an Alpha of anyone
        }
        List<HubSocial.Member> out = new ArrayList<>();
        out.add(member(server, alpha, "alpha"));
        betas.forEach(b -> out.add(member(server, b, "beta")));
        emissary.ifPresent(e -> out.add(member(server, e, "emissary")));
        return out;
    }

    private static HubSocial.Member member(MinecraftServer server, UUID id, String rank) {
        String name = PackAccess.nameOf(server, id);
        return new HubSocial.Member(name == null ? id.toString() : name, rank, server.getPlayerList().getPlayer(id) != null);
    }
}
