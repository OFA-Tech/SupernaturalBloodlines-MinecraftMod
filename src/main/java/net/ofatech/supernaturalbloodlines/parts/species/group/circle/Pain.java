package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import net.minecraft.server.level.ServerPlayer;

/**
 * Hurt that never kills: health is lowered directly (to 1 at the least) and the hurt animation and sound are played, so another mod's damage multiplier
 * or armour hook cannot turn a share of strain or the anchor's death into a death. The Admin option {@code link_death_lethal} is the only way a link kills.
 */
public final class Pain {
    private Pain() {
    }

    /** The health left after {@code damage}, never below 1 (a hit that would not leave a heart leaves one). */
    public static float left(float health, float damage) {
        return Math.max(1.0f, health - Math.max(0.0f, damage));
    }

    public static void hurt(ServerPlayer p, float damage) {
        float after = left(p.getHealth(), damage);
        if (after >= p.getHealth()) {
            return;
        }
        p.setHealth(after);
        p.level().broadcastEntityEvent(p, (byte) 2);
    }
}