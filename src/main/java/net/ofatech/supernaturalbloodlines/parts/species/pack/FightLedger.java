package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.damagesource.DamageSource;
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

    private static final Map<Key, Long> LAST = new ConcurrentHashMap<>();
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
            if (!e.getEntity().level().isClientSide) {
                SWINGS.put(e.getEntity().getUUID(), new Swing(e.getTarget().getUUID(), e.getEntity().level().getGameTime()));
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingIncomingDamageEvent.class, e -> {
            if (!e.getEntity().level().isClientSide && e.getContainer().getOriginalDamage() > 0) {
                onHit(e.getEntity(), e.getSource());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, e -> {
            UUID dead = e.getEntity().getUUID();
            MELEE.remove(dead);
            if (!(e.getEntity() instanceof Player)) {
                LAST.keySet().removeIf(k -> k.target().equals(dead));
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            LAST.clear();
            MELEE.clear();
            SWINGS.clear();
        });
    }

    private static void onHit(Entity target, DamageSource source) {
        Player p = deliberate(source);
        if (p == null || p == target) {
            return;
        }
        long now = PackIndex.now();
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

    private static void forget(UUID player) {
        LAST.keySet().removeIf(k -> k.attacker().equals(player) || k.target().equals(player));
        MELEE.remove(player);
        SWINGS.remove(player);
    }
}
