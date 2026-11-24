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
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Knowledge;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Visibility;
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
        PackIndex.onLeadershipMoved((old, next) -> withStorage(k -> k.movePool(old, next)));
        PackIndex.onDissolved(leader -> withStorage(k -> k.dropPool(leader)));
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

    private static void withStorage(java.util.function.Consumer<ProfileKnowledge> action) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            action.accept(ProfileKnowledge.get(server.overworld()));
        }
    }

    @Nullable
    private static PackIndex packs(ServerPlayer p) {
        return PackIndex.get(p.serverLevel());
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
        // what the Memory Claws taught: their own snapshot, theirs alone
        if (MemorySnapshots.get(observer.serverLevel()).of(observer.getUUID(), subject).isPresent()) {
            out.addAll(Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS));
        }
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
            case SPECIES -> relation == Relation.NPC_CREATURE
                    ? SirelineIndex.get(observer.serverLevel()).entityTypeOf(subject).map(type -> List.of(type.substring(type.indexOf(':') + 1)))
                    : Optional.ofNullable(who).map(w -> List.of(BloodlineAccess.get(w).species().name()));
            case RANK -> relation == Relation.ALLY_LEADER ? Optional.of(List.of("ally")) : Optional.ofNullable(who).map(w -> List.of(rank(w, packs)));
            case LEVEL -> Optional.ofNullable(who).map(w -> List.of(Integer.toString(VampirismAPI.factionPlayerHandler(w).getCurrentLevel())));
            case PACK_DAYS -> packs == null ? Optional.empty() : packs.inPackSince(subject).map(since -> List.of(Long.toString(daysSince(since))));
            case BONUS -> who == null || inOtherGroup(observer, packs, subject) ? Optional.empty() : RankEffects.bonusText(who).map(List::of);
            case PACK -> packs == null ? Optional.empty() : Optional.of(packs.inPack(subject) ? groupFact(observer, packs.leaderOf(subject)) : List.of());
            case PRESENCE -> Optional.of(List.of(who != null ? "online" : "offline"));
            case SIRE -> remembered(observer, subject, relation, Fact.SIRE, MemorySnapshots.Memory::sire).orElseGet(() -> sire(observer, subject));
            case LINE -> line(observer, subject, relation);
            case GENES -> remembered(observer, subject, relation, Fact.GENES, MemorySnapshots.Memory::genes).orElseGet(() -> genes(observer, who, relation));
            case ANCHOR -> packs == null ? Optional.empty() : Optional.of(packs.anchorOf(subject).map(id -> List.of(String.valueOf(nameOf(server, id)))).orElse(List.of()));
            default -> Optional.empty();
        };
    }

    /**
     * What the observer read with the Memory Claws, as it was then: present only when there is a snapshot and the observer has no live way to the fact: the relation
     * does not show it for free, and neither their own discoveries nor their pack's pool already give it to them (a pack-mate who joined later, a revelation). A
     * snapshot with nothing in it for this fact reads as "?".
     */
    private static Optional<Optional<List<String>>> remembered(ServerPlayer observer, UUID subject, Relation relation, Fact fact,
                                                               java.util.function.Function<MemorySnapshots.Memory, List<String>> pick) {
        if (Knowledge.visible(relation, fact) == Visibility.KNOWN
                || ProfileKnowledge.get(observer.serverLevel()).known(packs(observer), observer.getUUID(), subject).contains(fact)) {
            return Optional.empty();
        }
        return MemorySnapshots.get(observer.serverLevel()).of(observer.getUUID(), subject).map(m -> Optional.ofNullable(pick.apply(m)));
    }

    /** The Memory Claws were used: the observer is given what {@code subject} knows of their sire and genes now, and not a word more later. */
    public static void snapshot(ServerPlayer observer, ServerPlayer subject) {
        MemorySnapshots.get(observer.serverLevel()).put(observer.getUUID(), subject.getUUID(),
                new MemorySnapshots.Memory(sire(observer, subject.getUUID()).orElse(null), genes(observer, subject, Relation.STRANGER).orElse(null)));
    }

    /** Whole days from {@code since} (wall-clock millis) to now; never negative. */
    static long daysSince(long since) {
        return Math.max(0, (PackIndex.now() - since) / 86_400_000L);
    }

    /** The sire's name; "none" for no sire, and nothing (shown as "?") when the sire is an NPC Original or a creature: the same rule the hub's own Bloodline page uses. */
    private static Optional<List<String>> sire(ServerPlayer observer, UUID subject) {
        SirelineIndex sirelines = SirelineIndex.get(observer.serverLevel());
        Optional<UUID> sire = sirelines.sireOf(subject);
        if (sire.isEmpty()) {
            return Optional.of(List.of());
        }
        if (sirelines.isCreature(sire.get())) {
            return Optional.empty();
        }
        return Optional.of(List.of(String.valueOf(nameOf(observer.server, sire.get()))));
    }

    @Nullable
    private static String nameOf(MinecraftServer server, UUID id) {
        String n = PackAccess.nameOf(server, id);
        return n != null ? n : SirelineIndex.get(server.overworld()).nameOf(id).orElse(null);
    }

    /** The pack bonus belongs to a pack: somebody in a clan, a coven or a brotherhood has none to show. */
    private static boolean inOtherGroup(ServerPlayer observer, @Nullable PackIndex packs, UUID subject) {
        return packs != null && packs.inPack(subject)
                && net.ofatech.supernaturalbloodlines.parts.species.group.Groups.typeOfGroup(observer.serverLevel(), packs.leaderOf(subject)) != net.ofatech.supernaturalbloodlines.core.group.GroupType.PACK;
    }

    /** The leader's name, and the group's type when it is not a pack (a pack's fact is the name alone, as it always was). */
    private static List<String> groupFact(ServerPlayer observer, UUID leader) {
        String name = String.valueOf(nameOf(observer.server, leader));
        var type = net.ofatech.supernaturalbloodlines.parts.species.group.Groups.typeOfGroup(observer.serverLevel(), leader);
        return type == net.ofatech.supernaturalbloodlines.core.group.GroupType.PACK ? List.of(name) : List.of(name, type.id());
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
        Optional<UUID> root = lines.rootOf(subject);
        if (root.isEmpty()) {
            return Optional.of(List.of());
        }
        // the root a person has actually discovered of themselves, the same rule their own Bloodline page uses: where the index has moved the root on
        // (an Original who fell, a line freed) the new one stays a secret to them, so it must stay one to their pack too
        // shortcut: what a player has discovered of their line (knownLine) is read from their live data, so an offline packmate's line is "?" (the page says why),
        // upgrade by reading it from a saved copy of knownLine kept in the profile knowledge when they log out
        ServerPlayer who = online(server, subject);
        boolean shown = who != null && LineRules.showsRoot(BloodlineAccess.get(who).knownLine(), root.get(), SpeciesConfig.originalVisibilityAlways());
        return shown ? Optional.of(List.of(Lines.rootName(server, root.get()).getString())) : Optional.empty();
    }

    private static Optional<List<String>> genes(ServerPlayer observer, @Nullable ServerPlayer who, Relation relation) {
        if (who == null) {
            return Optional.empty();
        }
        // only the genes the subject has themselves discovered (Genes.forClient), for the subject and for a packmate alike: a gene nobody has found stays "unknown"
        BloodlineState view = BloodlineAccess.clientView(BloodlineAccess.get(who), true);
        return Optional.of(Genes.ALL.stream().map(g -> g + ":" + Genes.ofClient(view, g).name().toLowerCase(java.util.Locale.ROOT)).toList());
    }
}

