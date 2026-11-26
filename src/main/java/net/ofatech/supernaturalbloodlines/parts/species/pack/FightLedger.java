package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Who deliberately damaged whom in the last 10 real seconds (R294): a player's own melee hit, or a projectile, potion, TNT or other damage whose source is that
 * player. Thorns and pets never count: thorns is its own damage type, and a pet's source is the pet. Kept in memory only, keyed by player pair.
 */
public final class FightLedger {
    public static final long WINDOW_MILLIS = 10_000L;
    private static final int PRUNE_AT = 4096;

    private record Key(UUID attacker, UUID target) {
    }

    private static final Map<Key, Long> LAST = new ConcurrentHashMap<>();
    private static boolean registered;

    private FightLedger() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post e) -> {
            Player p = deliberate(e.getSource());
            if (p != null && p != e.getEntity() && e.getNewDamage() > 0 && !e.getEntity().level().isClientSide) {
                record(p.getUUID(), e.getEntity().getUUID(), PackIndex.now());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> LAST.clear());
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
}
