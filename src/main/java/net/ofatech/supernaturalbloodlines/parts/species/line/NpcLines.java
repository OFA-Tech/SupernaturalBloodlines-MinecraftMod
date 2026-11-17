package net.ofatech.supernaturalbloodlines.parts.species.line;

import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import org.jetbrains.annotations.Nullable;

/** Every Vampirism vampire creature, except bosses, belongs to the line of an NPC Original (a weighted pick) or of whoever sired it. */
public final class NpcLines {
    public static final TagKey<EntityType<?>> EXEMPT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "line_exempt"));
    private static volatile DoubleSupplier roll;

    private NpcLines() {
    }

    /** For tests: pins the roll in [0,1); null clears it. */
    public static void useRoll(@Nullable DoubleSupplier supplier) {
        roll = supplier;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NpcLines::onJoin);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityLeaveLevelEvent.class, NpcLines::onLeave);
    }

    public static Optional<NpcOriginal> pick(MinecraftServer server) {
        DoubleSupplier pinned = roll;
        double r = pinned != null ? pinned.getAsDouble() : server.overworld().getRandom().nextDouble();
        return LineRules.pickRoot(LineRules.parseWeights(SpeciesConfig.npcLineWeights()), LineIndex.get(server).deadNpcRoots(), r);
    }

    /**
     * An un-sired NPC vampire keeps its line on the creature alone (the {@code LINE} attachment): thousands of them would otherwise fill the
     * index. A creature a player or NPC sired is a member of the line and is in the index (so line death and the rites can reach it).
     */
    private static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player
                || !LineDeath.dies(mob) || mob.getType().is(EXEMPT)) {
            return;
        }
        SirelineIndex sirelines = SirelineIndex.get(level);
        UUID id = mob.getUUID();
        if (sirelines.isPendingMob(id)) {
            return; // owed a death: LineDeath.onJoin turns it away
        }
        MinecraftServer server = level.getServer();
        LineIndex index = LineIndex.get(server);
        boolean sired = mob.hasData(SpeciesRegistries.SIRE);
        Optional<UUID> known = index.rootOf(id);
        if (known.isPresent()) {
            Lines.assign(mob, known.get());
        } else if (mob.hasData(SpeciesRegistries.LINE)) {
            if (sired) {
                index.setRoot(id, mob.getData(SpeciesRegistries.LINE), false);
            }
        } else if (sired) {
            // a creature sired before lines existed: its line is whatever its sire's is
            Lines.climbFrom(server, sirelines, id, Optional.of(mob.getData(SpeciesRegistries.SIRE))).ifPresent(root -> Lines.join(server, root, mob));
        } else {
            pick(server).ifPresent(root -> Lines.assign(mob, root.uuid()));
        }
    }

    /** A sired creature that is destroyed (killed or discarded) leaves the index; one that only unloads or changes dimension stays. */
    private static void onLeave(EntityLeaveLevelEvent e) {
        Entity.RemovalReason why = e.getEntity().getRemovalReason();
        if (e.getLevel().isClientSide || e.getEntity() instanceof Player
                || (why != Entity.RemovalReason.DISCARDED && why != Entity.RemovalReason.KILLED)) {
            return;
        }
        LineIndex.get(e.getLevel().getServer()).remove(e.getEntity().getUUID());
    }
}
