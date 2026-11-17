package net.ofatech.supernaturalbloodlines.parts.species.line;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesData;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * Keeps every vampire's line in the {@link LineIndex} (R120, R121, R127): a sired child inherits the line, a rite-made Original
 * roots their own, someone who stops being supernatural leaves, and a world from before is rebuilt from the recorded sires at login.
 */
public final class Lines {
    private static final int CLIMB_LIMIT = 64;

    private Lines() {
    }

    public static void register() {
        SireRecorder.addListener(Lines::onSired);
        BloodlineAccess.addListener(Lines::onBloodlineChange);
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getOldFaction() == VReference.VAMPIRE_FACTION && e.getCurrentFaction() != VReference.VAMPIRE_FACTION
                    && e.getPlayer().getPlayer() instanceof ServerPlayer p && !p.level().isClientSide) {
                leaveIfNotSupernatural(p);
            }
        });
        // after SireRecorder's own login sync, which makes the sire chain true before it is read
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                heal(p);
            }
        });
    }

    public static Optional<UUID> rootOf(MinecraftServer server, UUID id) {
        return LineIndex.get(server).rootOf(id);
    }

    /** An online player's species, else what the world recorded, else one of the six NPC Originals. */
    public static boolean isOriginalId(MinecraftServer server, UUID id) {
        ServerPlayer online = SireCall.player(server, id);
        if (online != null) {
            return OriginalRules.isOriginal(BloodlineAccess.get(online).species());
        }
        return SpeciesData.get(server).isOriginal(id) || NpcOriginal.byUuid(id).isPresent();
    }

    /** The line of a creature or player, in the index and (for a creature) mirrored on the entity. */
    public static void join(MinecraftServer server, UUID root, LivingEntity child) {
        LineIndex.get(server).setRoot(child.getUUID(), root, child instanceof ServerPlayer);
        if (!(child instanceof ServerPlayer)) {
            child.setData(SpeciesRegistries.LINE, root);
        }
    }

    private static void onSired(UUID sire, LivingEntity child) {
        MinecraftServer server = child.getServer();
        if (server == null || (child instanceof ServerPlayer p && OriginalRules.isOriginal(BloodlineAccess.get(p).species()))) {
            return;
        }
        LineRules.inheritedRoot(sire, isOriginalId(server, sire), rootOf(server, sire)).ifPresent(root -> join(server, root, child));
    }

    private static void onBloodlineChange(ServerPlayer p, BloodlineAccess.BloodlineChange change) {
        Species before = change.before().species();
        Species after = change.after().species();
        if (!OriginalRules.isOriginal(before) && OriginalRules.isOriginal(after)) {
            makeRoot(p);
        } else if (before != Species.NONE && after == Species.NONE) {
            leaveIfNotSupernatural(p);
        }
    }

    private static void leaveIfNotSupernatural(ServerPlayer p) {
        if (!Supernatural.is(p) && !OriginalRules.isOriginal(BloodlineAccess.get(p).species())) {
            LineIndex.get(p.server).remove(p.getUUID());
        }
    }

    /**
     * A player made Original is the root of their own line, even if an Original sired them. Everyone recorded below them who still
     * follows their previous root, or none, moves with them; an Original below keeps their own branch.
     */
    public static void makeRoot(ServerPlayer p) {
        MinecraftServer server = p.server;
        LineIndex index = LineIndex.get(server);
        SirelineIndex sirelines = SirelineIndex.get(p.serverLevel());
        Optional<UUID> previous = index.rootOf(p.getUUID());
        index.setRoot(p.getUUID(), p.getUUID(), true);
        Set<UUID> seen = new HashSet<>();
        seen.add(p.getUUID());
        Deque<UUID> work = new ArrayDeque<>();
        work.add(p.getUUID());
        while (!work.isEmpty()) {
            for (SirelineIndex.Node child : sirelines.childrenOf(work.poll())) {
                if (!seen.add(child.id()) || isOriginalId(server, child.id())) {
                    continue;
                }
                Optional<UUID> root = index.rootOf(child.id());
                if (root.isEmpty() || root.equals(previous) && index.freedOf(child.id()).isEmpty()) {
                    index.setRoot(child.id(), p.getUUID(), "minecraft:player".equals(child.entityType()));
                    mirror(server, child.id(), p.getUUID());
                    work.add(child.id());
                }
            }
        }
    }

    /** Writes the line onto a loaded creature (an unloaded one copies it from the index when it joins). */
    static void mirror(MinecraftServer server, UUID id, UUID root) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null && !(e instanceof ServerPlayer)) {
                e.setData(SpeciesRegistries.LINE, root);
                return;
            }
        }
    }

    /**
     * Login backfill: an Original with no entry roots their own line; a player with a recorded sire and no entry climbs the sires to
     * the first Original, or to the first ancestor who has a line, and joins it.
     */
    public static Optional<UUID> heal(ServerPlayer p) {
        MinecraftServer server = p.server;
        LineIndex index = LineIndex.get(server);
        if (index.rootOf(p.getUUID()).isPresent()) {
            return index.rootOf(p.getUUID());
        }
        if (OriginalRules.isOriginal(BloodlineAccess.get(p).species())) {
            makeRoot(p);
            return Optional.of(p.getUUID());
        }
        Optional<UUID> root = climb(server, SirelineIndex.get(p.serverLevel()), p.getUUID());
        root.ifPresent(r -> index.setRoot(p.getUUID(), r, true));
        return root;
    }

    /** The root found by climbing from {@code id}'s sire upwards; empty when no Original or member is above. Cycle-safe. */
    public static Optional<UUID> climb(MinecraftServer server, SirelineIndex sirelines, UUID id) {
        return climbFrom(server, sirelines, id, sirelines.sireOf(id));
    }

    /** {@link #climb} starting at a known sire (a creature keeps its sire on itself). */
    public static Optional<UUID> climbFrom(MinecraftServer server, SirelineIndex sirelines, UUID id, Optional<UUID> sire) {
        LineIndex index = LineIndex.get(server);
        Set<UUID> seen = new HashSet<>();
        seen.add(id);
        Optional<UUID> up = sire;
        for (int i = 0; i < CLIMB_LIMIT && up.isPresent() && seen.add(up.get()); i++) {
            UUID ancestor = up.get();
            if (isOriginalId(server, ancestor)) {
                return Optional.of(ancestor);
            }
            Optional<UUID> root = index.rootOf(ancestor);
            if (root.isPresent()) {
                return root;
            }
            up = sirelines.sireOf(ancestor);
        }
        return Optional.empty();
    }

    /** R123: the root this player sees, if any. */
    public static Optional<UUID> visibleRoot(ServerPlayer p) {
        return rootOf(p.server, p.getUUID())
                .filter(root -> LineRules.showsRoot(BloodlineAccess.get(p).knownLine(), root, SpeciesConfig.originalVisibilityAlways()));
    }

    /** The Rite of Revelation: the player learns the Original of their line. No line, nothing to learn. */
    public static void reveal(ServerPlayer p) {
        rootOf(p.server, p.getUUID()).ifPresent(root -> learn(p, root));
    }

    /** Remembers {@code root} as the Original this player has discovered. */
    public static void learn(ServerPlayer p, UUID root) {
        BloodlineState s = BloodlineAccess.get(p);
        if (!root.equals(s.knownLine())) {
            BloodlineAccess.set(p, s.toBuilder().knownLine(root).build());
        }
    }

    /** What to call an Original in a message: an NPC's lang name, or a player's profile name. */
    public static Component rootName(MinecraftServer server, UUID root) {
        Optional<NpcOriginal> npc = NpcOriginal.byUuid(root);
        if (npc.isPresent()) {
            return Component.translatable(npc.get().translationKey());
        }
        String name = SireRecorder.nameOfId(server, root);
        if (name == null) {
            name = SirelineIndex.get(server.overworld()).nameOf(root).orElse(root.toString());
        }
        return Component.literal(name);
    }

    /** The target leaves {@code from}'s line and heads their own; their sire bond and everyone they sired stay as they are. */
    public static void freeTarget(MinecraftServer server, UUID target, UUID from, LineIndex.Kind kind) {
        LineIndex.get(server).free(target, from, kind);
    }

    /**
     * Like {@link #freeTarget}, and everyone recorded below the target who is in {@code from}'s line follows the target
     * (an Original below keeps their own branch).
     */
    public static void freeBranch(MinecraftServer server, UUID target, UUID from) {
        LineIndex index = LineIndex.get(server);
        SirelineIndex sirelines = SirelineIndex.get(server.overworld());
        index.free(target, from, LineIndex.Kind.IMPRISONED);
        Set<UUID> seen = new HashSet<>();
        seen.add(target);
        Deque<UUID> work = new ArrayDeque<>();
        work.add(target);
        while (!work.isEmpty()) {
            for (SirelineIndex.Node child : sirelines.childrenOf(work.poll())) {
                if (!seen.add(child.id()) || isOriginalId(server, child.id())) {
                    continue;
                }
                if (index.rootOf(child.id()).filter(from::equals).isPresent()) {
                    index.setRoot(child.id(), target, "minecraft:player".equals(child.entityType()));
                    mirror(server, child.id(), target);
                    work.add(child.id());
                }
            }
        }
    }
}
