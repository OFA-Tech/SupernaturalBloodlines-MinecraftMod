package net.ofatech.supernaturalbloodlines.parts.species;

import java.util.ArrayList;
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
        return new HubSocial.Social(sire, sired, ModList.get().isLoaded("werewolves") ? pack(p, server) : List.of(),
                FiveRoster.get(p.serverLevel()).size());
    }

    private static List<HubSocial.Member> pack(ServerPlayer p, MinecraftServer server) {
        PackIndex index = PackIndex.get(p.serverLevel());
        UUID alpha = index.alphaOf(p.getUUID()).orElse(p.getUUID());
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
