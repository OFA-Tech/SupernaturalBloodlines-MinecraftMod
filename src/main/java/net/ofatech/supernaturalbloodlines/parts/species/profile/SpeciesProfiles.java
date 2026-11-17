package net.ofatech.supernaturalbloodlines.parts.species.profile;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Relation;
import org.jetbrains.annotations.Nullable;

/**
 * The species part's answer to "what is the observer to this person, what have they discovered of them, and what is the value": from the
 * pack, sireline and line indexes only. The pure table ({@code Knowledge}) decides what may be read; this class only reads what it is asked.
 */
public final class SpeciesProfiles implements ProfileService.Source {
    private static final String PLAYER = "minecraft:player";

    private SpeciesProfiles() {}

    public static void register() {
        ProfileService.set(new SpeciesProfiles());
        // a gene or a line an observer discovers about themselves is a discovery of their pack too
        BloodlineAccess.addDiscoveryListener((p, change) -> {
            BloodlineState before = change.before();
            BloodlineState after = change.after();
            Set<Fact> learned = EnumSet.noneOf(Fact.class);
            if (!before.revealedGenes().containsAll(after.revealedGenes())) {
                learned.add(Fact.GENES);
            }
            if (after.knownLine() != null && !after.knownLine().equals(before.knownLine())) {
                learned.add(Fact.LINE);
            }
            if (!learned.isEmpty()) {
                ProfileKnowledge.get(p.serverLevel()).discover(packs(p), p.getUUID(), p.getUUID(), learned);
            }
        });
    }

    @Nullable
    private static PackIndex packs(ServerPlayer p) {
        return ModList.get().isLoaded("werewolves") ? PackIndex.get(p.serverLevel()) : null;
    }

    @Nullable
    private static ServerPlayer online(MinecraftServer server, UUID id) {
        return SireCall.player(server, id);
    }

    @Override
    public Relation relationOf(ServerPlayer observer, UUID subject) {
        UUID me = observer.getUUID();
        if (me.equals(subject)) {
            return Relation.SELF;
        }
        PackIndex packs = packs(observer);
        if (packs != null) {
            if (packs.samePackNow(me, subject)) {
                return Relation.SAME_PACK;
            }
            if ((packs.inPack(me) || packs.isLeader(me)) && packs.isLeader(subject) && packs.alliesOf(packs.leaderOf(me)).contains(subject)) {
                return Relation.ALLY_LEADER;
            }
        }
        SirelineIndex sirelines = SirelineIndex.get(observer.serverLevel());
        boolean inLine = sirelines.isDescendant(me, subject) || sirelines.isDescendant(subject, me);
        if (inLine) {
            if (NpcOriginal.byUuid(subject).isPresent()) {
                return Relation.NPC_ANCESTOR;
            }
            if (sirelines.entityTypeOf(subject).map(t -> !PLAYER.equals(t)).orElse(false)) {
                return Relation.NPC_CREATURE;
            }
            if (sirelines.sireOf(me).map(subject::equals).orElse(false)) {
                return Relation.DIRECT_SIRE;
            }
            if (sirelines.sireOf(subject).map(me::equals).orElse(false)) {
                return Relation.DIRECT_SIRED;
            }
            return Relation.OTHER_IN_LINE;
        }
        return online(observer.server, subject) != null ? Relation.STRANGER : Relation.STRANGER_OFFLINE;
    }

    @Override
    public Set<Fact> discovered(ServerPlayer observer, UUID subject, Relation relation) {
        Set<Fact> out = EnumSet.copyOf(ProfileKnowledge.get(observer.serverLevel()).known(packs(observer), observer.getUUID(), subject));
        if (relation == Relation.SELF) {
            // what the rules already show a player of themselves: genes they know, a root they have discovered
            BloodlineState view = BloodlineAccess.clientView(BloodlineAccess.get(observer), true);
            if (Genes.ALL.stream().anyMatch(g -> Genes.ofClient(view, g) != Genes.Knowledge.UNKNOWN)) {
                out.add(Fact.GENES);
            }
            if (Lines.visibleRoot(observer).isPresent()) {
                out.add(Fact.LINE);
            }
        }
        return out;
    }

    @Override
    public Optional<List<String>> read(ServerPlayer observer, UUID subject, Relation relation, Fact fact) {
        MinecraftServer server = observer.server;
        ServerPlayer who = online(server, subject);
        PackIndex packs = packs(observer);
        return switch (fact) {
            case NAME -> Optional.ofNullable(nameOf(server, subject)).map(List::of);
            case SPECIES -> Optional.ofNullable(who).map(w -> List.of(BloodlineAccess.get(w).species().name()));
            case RANK -> relation == Relation.ALLY_LEADER ? Optional.of(List.of("ally")) : Optional.ofNullable(who).map(w -> List.of(rank(w, packs)));
            case LEVEL -> Optional.ofNullable(who).map(w -> List.of(Integer.toString(VampirismAPI.factionPlayerHandler(w).getCurrentLevel())));
            case PACK -> packs == null ? Optional.empty() : Optional.of(packs.inPack(subject) ? List.of(String.valueOf(nameOf(server, packs.leaderOf(subject)))) : List.of());
            case PRESENCE -> Optional.of(List.of(who != null ? "online" : "offline"));
            case SIRE -> Optional.of(SirelineIndex.get(observer.serverLevel()).sireOf(subject).map(id -> List.of(String.valueOf(nameOf(server, id)))).orElse(List.of()));
            case LINE -> line(observer, subject, relation);
            case GENES -> genes(observer, who, relation);
            case ANCHOR -> packs == null ? Optional.empty() : Optional.of(packs.anchorOf(subject).map(id -> List.of(String.valueOf(nameOf(server, id)))).orElse(List.of()));
            default -> Optional.empty();
        };
    }

    @Nullable
    private static String nameOf(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : SirelineIndex.get(server.overworld()).nameOf(id).orElse(null);
    }

    private static String rank(ServerPlayer w, @Nullable PackIndex packs) {
        WolfState wolf = PackAccess.wolf(w);
        if (wolf.lineage() != WolfState.Lineage.NONE) {
            return wolf.rank().name().toLowerCase(java.util.Locale.ROOT);
        }
        return packs != null && packs.inPack(w.getUUID()) ? "packmate" : "none";
    }

    private static Optional<List<String>> line(ServerPlayer observer, UUID subject, Relation relation) {
        MinecraftServer server = observer.server;
        LineIndex lines = LineIndex.get(server);
        if (relation == Relation.DIRECT_SIRE || relation == Relation.DIRECT_SIRED) {
            // the same line as yours, with no Original named
            Optional<UUID> mine = lines.rootOf(observer.getUUID());
            return mine.isPresent() && mine.equals(lines.rootOf(subject)) ? Optional.of(List.of("same_line")) : Optional.empty();
        }
        if (relation == Relation.SELF) {
            return Optional.of(Lines.visibleRoot(observer).map(r -> List.of(Lines.rootName(server, r).getString())).orElse(List.of()));
        }
        return Optional.of(lines.rootOf(subject).map(r -> List.of(Lines.rootName(server, r).getString())).orElse(List.of()));
    }

    private static Optional<List<String>> genes(ServerPlayer observer, @Nullable ServerPlayer who, Relation relation) {
        if (who == null) {
            return Optional.empty();
        }
        BloodlineState state = BloodlineAccess.get(who);
        if (relation == Relation.SELF) {
            BloodlineState view = BloodlineAccess.clientView(state, true);
            return Optional.of(Genes.ALL.stream().map(g -> g + ":" + Genes.ofClient(view, g).name().toLowerCase(java.util.Locale.ROOT)).toList());
        }
        return Optional.of(List.of(Genes.WEREWOLF + ":" + state.wolf().latentGene(), Genes.DOPPELGANGER + ":" + state.doppelgangerGene()));
    }
}

