package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;

/**
 * Hunter's Mark (R80-R83): a prey kill by a marked hunter grows the mark, a marked hunter killed by prey loses it
 * and the world owes a potential, and the first kill of a potential completes the Five and lifts the curse.
 * The mark gives +1% attack damage and +0.5% speed per kill, capped at {@link #CAP} kills.
 */
public final class MarkTracker {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hunter_mark");
    public static final int CAP = 20;
    static final double ATTACK_PER_KILL = 0.01;
    static final double SPEED_PER_KILL = 0.005;

    private MarkTracker() {}

    public static void register() {
        // LOW with receiveCanceled=false: only a real death counts. Vampirism (HIGHEST), Original rules (HIGH) and the
        // Resurrection Ring (NORMAL) cancel a death they prevent, and this must not see those. LOWEST is the Ash Dagger's.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDeathEvent.class, MarkTracker::onDeath);
        // markKills changes do not notify listeners (only species changes do), so kills sync explicitly
        BloodlineAccess.addListener((p, change) -> sync(p));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfPlayer(e.getEntity()));
    }

    private static void syncIfPlayer(Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    /** R81: vampires and werewolf types. Species wins over faction, so a witch or siphoner is never prey. */
    static boolean isPrey(Entity e) {
        if (e instanceof Player p) {
            Species s = BloodlineAccess.get(p).species();
            if (s == Species.WITCH || s == Species.SIPHONER) {
                return false;
            }
        }
        return Supernatural.isPrey(e);
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Entity killer = e.getSource().getEntity();
        if (victim.level().isClientSide || killer == null || killer == victim) {
            return;
        }
        if (killer instanceof ServerPlayer k && isPrey(victim)) {
            killedPrey(k);
        }
        if (victim instanceof ServerPlayer v && BloodlineAccess.get(v).species() == Species.HUNTER_OF_THE_FIVE && isPrey(killer)) {
            killedMarkedHunter(v, killer);
        }
    }

    private static void killedPrey(ServerPlayer killer) {
        Species before = BloodlineAccess.get(killer).species();
        if (before != Species.HUNTER_OF_THE_FIVE && before != Species.POTENTIAL) {
            return;
        }
        BloodlineAccess.applyEvent(killer, BloodlineEvent.KILLED_PREY, SpeciesContext.of(killer, null));
        sync(killer);
        if (before == Species.POTENTIAL && BloodlineAccess.get(killer).species() == Species.HUNTER_OF_THE_FIVE) {
            HunterCurse.liftAll(killer.server);
        }
    }

    private static void killedMarkedHunter(ServerPlayer hunter, Entity killer) {
        BloodlineAccess.applyEvent(hunter, BloodlineEvent.KILLED_BY_PREY, SpeciesContext.of(hunter, null));
        if (killer instanceof ServerPlayer k) {
            HunterCurse.curse(k);
        }
        PotentialChooser.choose(hunter.server, hunter.getUUID());
    }

    /** Idempotent: makes the perks match the player's mark. */
    public static void sync(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        int kills = s.species() == Species.HUNTER_OF_THE_FIVE ? Math.min(s.markKills(), CAP) : 0;
        apply(p, Attributes.ATTACK_DAMAGE, kills * ATTACK_PER_KILL, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        apply(p, Attributes.MOVEMENT_SPEED, kills * SPEED_PER_KILL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (amount == 0) {
            if (current != null) {
                a.removeModifier(MODIFIER_ID);
            }
        } else if (current == null || current.amount() != amount) {
            a.removeModifier(MODIFIER_ID);
            a.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, operation));
        }
    }
}
