package net.ofatech.supernaturalbloodlines.parts.species.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond.Order;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * What a pack member does (R148), from a tick event and the public navigation API, with no goal injected. FOLLOW keeps within 6 blocks of its owner and
 * comes to them at 40 (in the same dimension: it does not cross one); GUARD holds its post (never farther than 12 blocks) and fights what hurts its
 * owner; REST stays where it is and only fights back. A member whose owner is offline or dead rests where it stands; the bond stays. Nothing here
 * runs for a Tensura subordinate: it keeps Tensura's own orders. Every target passes {@link Friends} first: a member never turns on its owner, the pack,
 * an allied pack, or their members.
 */
public final class NpcOrders {
    public static final int PERIOD = 10;
    public static final double FOLLOW_NEAR = 6;
    public static final double TELEPORT_AT = 40;
    public static final double GUARD_LEASH = 12;
    public static final double SIGHT = 24;
    private static final int TOUCH_PERIOD = 200;
    private static final String KEY = "supernaturalbloodlines.npc.";

    /** Where a member that is resting with no post of its own stays: in memory only (the first place it rested). */
    private static final Map<UUID, BlockPos> ANCHOR = new ConcurrentHashMap<>();

    private NpcOrders() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.tickCount % PERIOD == m.getId() % PERIOD && m.hasData(SpeciesRegistries.NPC_BOND)) {
                tick(m);
            }
        });
        NeoForge.EVENT_BUS.addListener(NpcOrders::onRetarget);
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.hasData(SpeciesRegistries.NPC_BOND)) {
                fell(m);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ANCHOR.clear());
    }

    /** For tests: one pass now, without waiting for the turn of the ten. */
    public static void tickNow(Mob m) {
        if (m.hasData(SpeciesRegistries.NPC_BOND)) {
            tick(m);
        }
    }

    static void tick(Mob m) {
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        if (!bond.bonded() || !(m.level() instanceof ServerLevel level)) {
            return;
        }
        if (m.tickCount % TOUCH_PERIOD < PERIOD) {
            NpcMembers.touch(m);
        }
        LivingEntity target = m.getTarget();
        if (target != null && Friends.ofOwner(level, bond.owner(), target)) {
            m.setTarget(null);
        }
        if (!NpcRules.hasOurOrders(bond.source())) {
            return;
        }
        ServerPlayer owner = SireCall.player(level.getServer(), bond.owner());
        Order order = NpcMembers.effectiveOrder(level.getServer(), m);
        if (order != Order.REST) {
            ANCHOR.remove(m.getUUID());
        }
        switch (order) {
            case FOLLOW -> follow(m, owner);
            case GUARD -> guard(m, bond, owner);
            case REST -> rest(m, bond);
        }
        if (order != Order.REST) {
            defend(m, owner, level);
        }
    }

    private static void follow(Mob m, ServerPlayer owner) {
        if (owner == null || owner.level() != m.level()) {
            return;
        }
        double d2 = m.distanceToSqr(owner);
        if (d2 > TELEPORT_AT * TELEPORT_AT) {
            m.teleportTo(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5);
            m.getNavigation().stop();
        } else if (d2 > FOLLOW_NEAR * FOLLOW_NEAR) {
            m.getNavigation().moveTo(owner, 1.15);
        } else if (d2 < 9) {
            m.getNavigation().stop();
        }
    }

    private static BlockPos postOf(Mob m, NpcBond bond) {
        return bond.post() != 0 ? BlockPos.of(bond.post()) : ANCHOR.computeIfAbsent(m.getUUID(), id -> m.blockPosition());
    }

    private static void guard(Mob m, NpcBond bond, ServerPlayer owner) {
        BlockPos post = postOf(m, bond);
        if (m.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > GUARD_LEASH * GUARD_LEASH && m.getTarget() == null) {
            m.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.1);
        }
    }

    private static void rest(Mob m, NpcBond bond) {
        BlockPos post = postOf(m, bond);
        if (m.distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > 4) {
            m.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
        } else {
            m.getNavigation().stop();
        }
        // it only fights back: a target it picked itself is dropped unless something hurt it a moment ago
        boolean hurt = m.getLastHurtByMob() != null && m.tickCount - m.getLastHurtByMobTimestamp() < 100;
        if (!hurt) {
            m.setTarget(null);
        }
    }

    /** Takes up what recently hurt the owner, or what the owner is fighting, if it is near and not a friend. */
    private static void defend(Mob m, ServerPlayer owner, ServerLevel level) {
        if (owner == null || owner.level() != level || m.getTarget() != null) {
            return;
        }
        LivingEntity foe = owner.getLastHurtByMob();
        if (foe == null || owner.tickCount - owner.getLastHurtByMobTimestamp() > 200) {
            foe = owner.getLastHurtMob();
            if (foe != null && owner.tickCount - owner.getLastHurtMobTimestamp() > 200) {
                foe = null;
            }
        }
        if (foe != null && foe.isAlive() && foe != m && m.distanceToSqr(foe) <= SIGHT * SIGHT
                && !Friends.ofOwner(level, owner.getUUID(), foe)) {
            m.setTarget(foe);
        }
    }

    /** A member never takes a friend as a target: its owner, the owner's pack and allies, and their members. */
    private static void onRetarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide || !m.hasData(SpeciesRegistries.NPC_BOND) || e.getNewAboutToBeSetTarget() == null) {
            return;
        }
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        if (bond.bonded() && Friends.ofOwner(m.level(), bond.owner(), e.getNewAboutToBeSetTarget())) {
            e.setCanceled(true);
        }
    }

    /** A member that died is forgotten, and its owner is told (a notice for the next login when they are away). */
    static void fell(Mob m) {
        NpcBond bond = m.getData(SpeciesRegistries.NPC_BOND);
        ServerLevel level = (ServerLevel) m.level();
        NpcIndex.get(level).remove(m.getUUID());
        ANCHOR.remove(m.getUUID());
        if (!bond.bonded()) {
            return;
        }
        String name = m.getDisplayName().getString();
        ServerPlayer owner = SireCall.player(level.getServer(), bond.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable(KEY + "fell", name));
        } else {
            PackIndex.get(level).addNotice(bond.owner(), KEY + "fell", name);
        }
    }

    /** For tests: forget the places members rested at. */
    public static void reset() {
        ANCHOR.clear();
    }

    /** A new order forgets where the member rested before. */
    static void reset(Mob m) {
        ANCHOR.remove(m.getUUID());
    }
}
