package net.ofatech.supernaturalbloodlines.parts.species.line;

import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
        // last, so a death something else cancelled is not counted
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, NpcLines::onDeath);
    }

    public static Optional<NpcOriginal> pick(MinecraftServer server) {
        DoubleSupplier pinned = roll;
        double r = pinned != null ? pinned.getAsDouble() : server.overworld().getRandom().nextDouble();
        return LineRules.pickRoot(LineRules.parseWeights(SpeciesConfig.npcLineWeights()), LineIndex.get(server).deadNpcRoots(), r);
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof LivingEntity mob) || mob instanceof Player
                || !(mob instanceof IVampire || mob instanceof IConvertedCreature<?>) || mob.getType().is(EXEMPT)) {
            return;
        }
        MinecraftServer server = level.getServer();
        LineIndex index = LineIndex.get(server);
        UUID id = mob.getUUID();
        Optional<UUID> known = index.rootOf(id);
        if (known.isPresent()) {
            mob.setData(SpeciesRegistries.LINE, known.get());
        } else if (mob.hasData(SpeciesRegistries.LINE)) {
            index.setRoot(id, mob.getData(SpeciesRegistries.LINE), false);
        } else if (mob.hasData(SpeciesRegistries.SIRE)) {
            // a creature sired before lines existed: its line is whatever its sire's is
            Lines.climbFrom(server, SirelineIndex.get(level), id, Optional.of(mob.getData(SpeciesRegistries.SIRE)))
                    .ifPresent(root -> Lines.join(server, root, mob));
        } else {
            pick(server).ifPresent(root -> Lines.join(server, root.uuid(), mob));
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!e.getEntity().level().isClientSide && !(e.getEntity() instanceof Player) && e.getEntity().hasData(SpeciesRegistries.LINE)) {
            LineIndex.get(e.getEntity().getServer()).remove(e.getEntity().getUUID());
        }
    }
}
