package net.ofatech.supernaturalbloodlines.parts.species.npc;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.hunter.IHunter;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority.Candidate;
import net.ofatech.supernaturalbloodlines.core.bloodline.TargetPriority.Tier;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * What an NPC werewolf of a spawned pack does (R144), from a tick event and a retarget event, with no goal injected: every 10 ticks it looks within
 * 32 blocks and hunts a vampire first, then a hunter, then another player, then prey; the wolves of one pack share a target for five seconds so they
 * hunt together; a worse target never takes over from a better one that is still in reach; a friend is never chosen. On a dark full-moon night it
 * is feral (a transient, never-saved attack and speed bonus). At dawn a wolf with no player near leaves. Wolf Spirits are looked after here too:
 * they leave when their time is up or their owner is gone.
 */
public final class NpcPackBrain {
    public static final double SIGHT = 32;
    static final int PERIOD = 10;
    static final int SHARE_TICKS = 100;
    static final double SHARE_RANGE = 48;
    private static final double DAWN_RANGE = 24;
    private static final int DARK = 7;
    private static final ResourceLocation FERAL_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "feral");
    private static final ResourceLocation FERAL_SPEED_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "feral_speed");
    public static final TagKey<EntityType<?>> PREY = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "werewolf_prey"));

    /** What a pack agreed to hunt, until a game time. */
    private record Shared(UUID target, long until) {
    }

    private static final Map<Long, Shared> BOARD = new ConcurrentHashMap<>();

    private NpcPackBrain() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.tickCount % PERIOD == m.getId() % PERIOD && m.hasData(SpeciesRegistries.NPC_MARK)) {
                tick(m);
            }
        });
        NeoForge.EVENT_BUS.addListener(NpcPackBrain::onRetarget);
        // a Wolf Spirit drops and gives nothing
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) -> {
            if (isSpirit(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent e) -> {
            if (isSpirit(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BOARD.clear());
    }

    public static boolean isSpirit(LivingEntity e) {
        return e instanceof Mob m && m.hasData(SpeciesRegistries.NPC_MARK) && m.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.SPIRIT;
    }

    /** The tier a living thing has for a hunting wolf: null when it is not hunted. */
    @Nullable
    public static Tier tierOf(LivingEntity e) {
        if (e instanceof Player p) {
            if (p.isSpectator() || p.isCreative()) {
                return null;
            }
            if (VampirismSupernatural.isVampire(p)) {
                return Tier.VAMPIRE;
            }
            return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.HUNTER_FACTION ? Tier.HUNTER : Tier.PLAYER;
        }
        if (e instanceof IVampire) {
            return Tier.VAMPIRE;
        }
        if (e instanceof IHunter) {
            return Tier.HUNTER;
        }
        return e.getType().is(PREY) ? Tier.PREY : null;
    }

    /** Whether the wolf's pack would never hurt {@code other}: a Wolf Spirit's owner and the owner's pack and allies. */
    static boolean isFriend(Mob wolf, LivingEntity other) {
        NpcMark mark = wolf.getData(SpeciesRegistries.NPC_MARK);
        return mark.kind() == NpcMark.Kind.SPIRIT && Friends.ofOwner(wolf.level(), mark.owner(), other);
    }

    static void tick(Mob m) {
        if (!(m.level() instanceof ServerLevel level)) {
            return;
        }
        NpcMark mark = m.getData(SpeciesRegistries.NPC_MARK);
        if (mark.kind() == NpcMark.Kind.SPIRIT) {
            tickSpirit(level, m, mark);
            return;
        }
        SpeciesConfig.NpcTuning t = SpeciesConfig.npc();
        if (t.dawnDespawn() && leavesNow(level, m, mark)) {
            m.discard();
            return;
        }
        feral(level, m, t);
        hunt(level, m, mark);
    }

    /** At dawn, or a night later than its own, a wild wolf with no player within 24 blocks leaves. */
    private static boolean leavesNow(ServerLevel level, Mob m, NpcMark mark) {
        long timeOfDay = Math.floorMod(level.getDayTime(), 24000L);
        boolean over = timeOfDay >= MoonRule.NIGHT_END || timeOfDay < MoonRule.NIGHT_START || NpcSpawnRules.nightOf(level.getDayTime()) != mark.night();
        return over && level.getNearestPlayer(m, DAWN_RANGE) == null;
    }

    private static void feral(ServerLevel level, Mob m, SpeciesConfig.NpcTuning t) {
        boolean on = NpcSpawnRules.feral(MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime()),
                level.getMaxLocalRawBrightness(m.blockPosition()));
        modifier(m.getAttribute(Attributes.ATTACK_DAMAGE), FERAL_ID, on ? t.feralAttack() - 1 : 0);
        modifier(m.getAttribute(Attributes.MOVEMENT_SPEED), FERAL_SPEED_ID, on ? t.feralSpeed() - 1 : 0);
    }

    private static void modifier(@Nullable AttributeInstance attr, ResourceLocation id, double amount) {
        if (attr == null) {
            return;
        }
        if (amount > 0) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            attr.removeModifier(id);
        }
    }

    /** Whether the wolf carries the feral bonus now (a test reads it). */
    public static boolean isFeral(Mob m) {
        AttributeInstance a = m.getAttribute(Attributes.ATTACK_DAMAGE);
        return a != null && a.getModifier(FERAL_ID) != null;
    }

    private static void hunt(ServerLevel level, Mob m, NpcMark mark) {
        List<Candidate<LivingEntity>> seen = new ArrayList<>();
        AABB box = m.getBoundingBox().inflate(SIGHT);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != m && x.isAlive())) {
            Tier tier = tierOf(e);
            if (tier != null) {
                seen.add(new Candidate<>(e, tier, m.distanceToSqr(e), isFriend(m, e)));
            }
        }
        long now = level.getGameTime();
        Shared shared = BOARD.get(mark.packId());
        if (shared != null && shared.until() > now && level.getEntity(shared.target()) instanceof LivingEntity s && s.isAlive() && s != m
                && m.distanceToSqr(s) <= SHARE_RANGE * SHARE_RANGE && tierOf(s) != null) {
            seen.add(new Candidate<>(s, tierOf(s), m.distanceToSqr(s), false));
        }
        Candidate<LivingEntity> best = TargetPriority.pick(seen);
        LivingEntity current = m.getTarget();
        Tier currentTier = current == null ? null : tierOf(current);
        boolean valid = current != null && current.isAlive() && currentTier != null && m.distanceToSqr(current) <= SIGHT * SIGHT;
        if (best != null && TargetPriority.switchTo(currentTier, valid, best.tier())) {
            m.setTarget(best.who());
            BOARD.put(mark.packId(), new Shared(best.who().getUUID(), now + SHARE_TICKS));
        } else if (valid && current.getUUID() != null) {
            BOARD.putIfAbsent(mark.packId(), new Shared(current.getUUID(), now + SHARE_TICKS));
        }
    }

    private static void tickSpirit(ServerLevel level, Mob m, NpcMark mark) {
        boolean ownerHere = mark.owner() != null && level.getServer().getPlayerList().getPlayer(mark.owner()) != null;
        if (level.getGameTime() >= mark.expires() || !ownerHere) {
            m.discard();
            return;
        }
        LivingEntity target = m.getTarget();
        if (target != null && isFriend(m, target)) {
            m.setTarget(null);
        }
    }

    /** A worse target never replaces a better one that is still in reach; a Wolf Spirit never turns on its owner's friends. */
    private static void onRetarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide || !m.hasData(SpeciesRegistries.NPC_MARK)) {
            return;
        }
        LivingEntity next = e.getNewAboutToBeSetTarget();
        if (next == null) {
            return;
        }
        NpcMark mark = m.getData(SpeciesRegistries.NPC_MARK);
        if (mark.kind() == NpcMark.Kind.SPIRIT) {
            if (isFriend(m, next)) {
                e.setCanceled(true);
            }
            return;
        }
        LivingEntity current = m.getTarget();
        Tier currentTier = current == null ? null : tierOf(current);
        Tier nextTier = tierOf(next);
        boolean valid = current != null && current.isAlive() && currentTier != null && m.distanceToSqr(current) <= SIGHT * SIGHT;
        if (nextTier != null && valid && current != next && !TargetPriority.switchTo(currentTier, true, nextTier)) {
            e.setCanceled(true);
        }
    }

    /** For tests: forget what packs agreed to hunt. */
    public static void reset() {
        BOARD.clear();
    }

    /** For tests: the Wolf Spirits and wild wolves tick without waiting for their turn of the ten. */
    public static void tickNow(Mob m) {
        if (m.hasData(SpeciesRegistries.NPC_MARK)) {
            tick(m);
        }
    }

    /** A player the pack can see: only for {@link ServerPlayer} callers that need the same filter as the hunt. */
    public static boolean hunted(ServerPlayer p) {
        return tierOf(p) != null;
    }
}
