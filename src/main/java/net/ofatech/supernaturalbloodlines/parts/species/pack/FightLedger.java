package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Who deliberately damaged whom in the last 10 real seconds (R294), and who last landed a direct melee hit on each victim.
 * <p>A deliberate hit has the player as its source and is not thorns: their own melee hit, or their projectile, potion or TNT. A pet's damage has the pet as
 * its source, so it never counts; a hit on a tamed pet counts as a hit on its owner. Hits with no original damage (a snowball, an egg) do not count, while hits
 * a shield, Resistance or absorption soaked up do: the record is made as the damage comes in, before any reduction. Targets are players and villagers.
 * <p>A direct melee hit is a player-attack hit on the entity that player swung at this tick (a sweep hits other bodies, so it is not one). In memory only.
 */
public final class FightLedger {
    public static final long WINDOW_MILLIS = 10_000L;
    private static final int PRUNE_AT = 4096;

    private record Key(UUID attacker, UUID target) {
    }

    private record Hit(UUID by, long at) {
    }

    private record Swing(UUID target, long tick) {
    }

    private record Fight(long first, long last) {
    }

    private static final Map<Key, Long> LAST = new ConcurrentHashMap<>();
    private static final Map<Key, Fight> AGGRO = new ConcurrentHashMap<>();
    private static final Map<UUID, Hit> MELEE = new ConcurrentHashMap<>();
    private static final Map<UUID, Swing> SWINGS = new ConcurrentHashMap<>();
    private static boolean registered;

    private FightLedger() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            // a probe asks what a blow would meet without striking (a willing feed, a skill's check): never a swing
            if (!e.getEntity().level().isClientSide && !DamageProbe.active()) {
                SWINGS.put(e.getEntity().getUUID(), new Swing(e.getTarget().getUUID(), e.getEntity().level().getGameTime()));
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingIncomingDamageEvent.class, e -> {
            if (!e.getEntity().level().isClientSide && e.getContainer().getOriginalDamage() > 0 && !DamageProbe.active()) {
                onHit(e.getEntity(), e.getSource());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            UUID dead = e.getEntity().getUUID();
            MELEE.remove(dead);
            if (!(e.getEntity() instanceof Player)) {
                LAST.keySet().removeIf(k -> k.target().equals(dead));
                AGGRO.keySet().removeIf(k -> k.target().equals(dead));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            LAST.clear();
            AGGRO.clear();
            MELEE.clear();
            SWINGS.clear();
        });
    }

    private static void onHit(Entity target, DamageSource source) {
        long now = PackIndex.now();
        UUID victim = target instanceof Player || target instanceof Villager ? target.getUUID()
                : target instanceof OwnableEntity pet ? pet.getOwnerUUID() : null;
        // who started a fight: a player's own hits and their pets' bites both count here (R294 "defending a brother")
        UUID aggressor = deliberate(source) instanceof Player hitter ? hitter.getUUID()
                : source.getEntity() instanceof OwnableEntity pet ? pet.getOwnerUUID() : null;
        if (victim != null && aggressor != null && !aggressor.equals(victim)) {
            AGGRO.merge(new Key(aggressor, victim), new Fight(now, now), (old, fresh) -> now - old.last() > WINDOW_MILLIS ? fresh : new Fight(old.first(), now));
        }
        Player p = deliberate(source);
        if (p == null || p == target) {
            return;
        }
        if (target instanceof Player || target instanceof Villager) {
            record(p.getUUID(), target.getUUID(), now);
            Swing swing = SWINGS.get(p.getUUID());
            if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == p && swing != null && swing.target().equals(target.getUUID())
                    && swing.tick() == target.level().getGameTime()) {
                MELEE.put(target.getUUID(), new Hit(p.getUUID(), now));
            }
        } else if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && !pet.getOwnerUUID().equals(p.getUUID())) {
            record(p.getUUID(), pet.getOwnerUUID(), now);
        }
    }

    /** The player who meant this damage, or null (thorns, a pet, the world). */
    static Player deliberate(DamageSource s) {
        return !s.is(DamageTypes.THORNS) && s.getEntity() instanceof Player p ? p : null;
    }

    public static void record(UUID attacker, UUID target, long now) {
        if (LAST.size() >= PRUNE_AT) {
            LAST.values().removeIf(at -> now - at > WINDOW_MILLIS);
            AGGRO.values().removeIf(f -> now - f.last() > WINDOW_MILLIS);
            MELEE.values().removeIf(h -> now - h.at() > WINDOW_MILLIS);
        }
        LAST.put(new Key(attacker, target), now);
    }

    /** Whether {@code attacker} deliberately damaged {@code target} within the last 10 seconds. */
    public static boolean struck(UUID attacker, UUID target, long now) {
        Long at = LAST.get(new Key(attacker, target));
        return at != null && now - at <= WINDOW_MILLIS;
    }

    /** The player whose direct melee hit last landed on {@code victim} within the last 10 seconds, or null. */
    public static UUID lastMeleeStriker(UUID victim, long now) {
        Hit hit = MELEE.get(victim);
        return hit != null && now - hit.at() <= WINDOW_MILLIS ? hit.by() : null;
    }

    /**
     * Whether {@code brother} (or his pet) started the fight with {@code other}: he hit them in the last 10 seconds, and no later than their first hit on him in
     * that fight. A fight he started licenses no other brother to "defend" him.
     */
    public static boolean startedIt(UUID brother, UUID other, long now) {
        Fight mine = AGGRO.get(new Key(brother, other));
        if (mine == null || now - mine.last() > WINDOW_MILLIS) {
            return false;
        }
        Fight theirs = AGGRO.get(new Key(other, brother));
        return theirs == null || mine.first() <= theirs.first();
    }

    private static void forget(UUID player) {
        LAST.keySet().removeIf(k -> k.attacker().equals(player) || k.target().equals(player));
        AGGRO.keySet().removeIf(k -> k.attacker().equals(player) || k.target().equals(player));
        MELEE.remove(player);
        SWINGS.remove(player);
    }
}
