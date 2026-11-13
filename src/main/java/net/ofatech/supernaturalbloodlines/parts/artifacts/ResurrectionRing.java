package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R13: a worn Resurrection Ring with charges cancels the death of a human killed by something supernatural: full
 * health, Regeneration II for 10 s, one charge. A human is a player with no vampire or werewolf faction and a species
 * of NONE, POTENTIAL or HUNTER_OF_THE_FIVE.
 */
public final class ResurrectionRing {
    /**
     * NORMAL, on purpose. Vampirism's death handler runs at HIGHEST (its dying state) and OriginalRules at HIGH; both
     * cancel a death they save, and we do not receive cancelled events, so a death they saved costs no charge. It must
     * still run before TransitionHandler and GeneAwakening (LOW), or a victim we revive would still be counted as a
     * kill (a gene awakening for the killer, a werewolf's transition). Pinned by a unit test and a GameTest.
     */
    public static final EventPriority PRIORITY = EventPriority.NORMAL;

    private static final int REGENERATION_TICKS = 10 * 20;

    private ResurrectionRing() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PRIORITY, false, LivingDeathEvent.class, ResurrectionRing::onDeath);
    }

    private static void onDeath(LivingDeathEvent e) {
        Entity attacker = e.getSource().getEntity();
        if (!(e.getEntity() instanceof ServerPlayer p) || attacker == null || !Supernatural.is(attacker)) {
            return;
        }
        if (!speciesAllowed(BloodlineAccess.get(p).species()) || VampirismSupernatural.isVampire(p) || VampirismSupernatural.isWerewolf(p)) {
            return;
        }
        // read again here: the ring may have left the inventory since the damage was dealt
        Optional<ItemStack> ring = ArtifactSlots.findWorn(p, ArtifactItems.RESURRECTION_RING.get(), s -> charges(s) > 0);
        if (ring.isEmpty()) {
            return;
        }
        ItemStack stack = ring.get();
        stack.set(ArtifactItems.CHARGES.get(), after(charges(stack)));
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 1));
    }

    private static int charges(ItemStack ring) {
        return ring.getOrDefault(ArtifactItems.CHARGES.get(), 0);
    }

    static boolean speciesAllowed(Species s) {
        return s == Species.NONE || s == Species.POTENTIAL || s == Species.HUNTER_OF_THE_FIVE;
    }

    static int after(int charges) {
        return Math.max(0, charges - 1);
    }
}
