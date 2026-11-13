package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import java.util.UUID;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;

/**
 * R43: a hybrid cannot hurt their sire. Whatever the damage names as its cause is followed to the people behind it:
 * the owner of an arrow or a potion cloud, the owner of a tamed pet. If one of them is a hybrid type whose
 * {@code sire} is the one being hurt, the damage is cancelled.
 */
public final class SireBond {
    private static final int MAX_DEPTH = 4;

    private SireBond() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SireBond::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        UUID victim = e.getEntity().getUUID();
        if (bound(e.getSource().getEntity(), victim, 0) || bound(e.getSource().getDirectEntity(), victim, 0)) {
            e.setCanceled(true);
        }
    }

    private static boolean bound(Entity cause, UUID victim, int depth) {
        if (cause == null || depth > MAX_DEPTH) {
            return false;
        }
        if (cause instanceof Player p) {
            BloodlineState s = BloodlineAccess.get(p);
            return HybridTraits.isHybrid(s.species()) && victim.equals(s.sire());
        }
        Entity owner = cause instanceof Projectile projectile ? projectile.getOwner()
                : cause instanceof AreaEffectCloud cloud ? cloud.getOwner()
                : cause instanceof OwnableEntity pet ? pet.getOwner() : null;
        return bound(owner, victim, depth + 1);
    }
}
