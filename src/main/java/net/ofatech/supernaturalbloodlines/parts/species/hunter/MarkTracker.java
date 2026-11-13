package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, MarkTracker::rememberAttacker);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> DOWNING_ATTACKER.remove(e.getEntity().getUUID()));
        // heard on a species change and on a markKills change (rules, debug event, anything that goes through BloodlineAccess)
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

    /**
     * Vampirism's dying state cancels the first lethal hit, and the real death comes much later (the timer, "give up")
     * from a source with no attacker, after the vanilla kill credit has run out. So the attacker of a death that was
     * cancelled while the victim went into the dying state is remembered here until the real death. In memory only,
     * dropped at the death and at logout; it expires after {@link #ATTACKER_MEMORY_TICKS} in case the victim was revived.
     */
    private record Attacker(UUID player, WeakReference<Entity> mob, long time) {
        /** A player is found again by UUID in the player list (a player who logged out and in is a new object); anything else by reference. */
        Entity resolve(MinecraftServer server) {
            if (player != null) {
                return HunterEvents.online(server).stream().filter(o -> o.getUUID().equals(player)).findFirst().orElse(null);
            }
            Entity e = mob.get();
            return e != null && !e.isRemoved() ? e : null;
        }
    }

    private static final Map<UUID, Attacker> DOWNING_ATTACKER = new HashMap<>();
    private static final long ATTACKER_MEMORY_TICKS = 12000;

    private static void rememberAttacker(LivingDeathEvent e) {
        Entity attacker = e.getSource().getEntity();
        if (e.isCanceled() && e.getEntity() instanceof ServerPlayer p && attacker != null && attacker != p
                && VampirismAPI.vampirePlayer(p).isDBNO()) {
            DOWNING_ATTACKER.put(p.getUUID(), attacker instanceof Player a
                    ? new Attacker(a.getUUID(), null, p.level().getGameTime())
                    : new Attacker(null, new WeakReference<>(attacker), p.level().getGameTime()));
        }
    }

    /** Who gets the credit: the damage source, else the vanilla kill credit, else the attacker that put the victim into the dying state. */
    private static Entity killerOf(LivingEntity victim, DamageSource source) {
        Entity killer = source.getEntity();
        if (killer == null) {
            killer = victim.getKillCredit();
        }
        Attacker remembered = victim instanceof ServerPlayer p ? DOWNING_ATTACKER.remove(p.getUUID()) : null;
        if (killer == null && remembered != null && victim.level().getGameTime() - remembered.time <= ATTACKER_MEMORY_TICKS) {
            killer = remembered.resolve(victim.level().getServer());
        }
        return killer;
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity killer = killerOf(victim, e.getSource());
        if (killer == null || killer == victim) {
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
        if (before == Species.POTENTIAL && BloodlineAccess.get(killer).species() == Species.HUNTER_OF_THE_FIVE) {
            HunterCurse.liftAll(killer.server);
        }
    }

    private static void killedMarkedHunter(ServerPlayer hunter, Entity killer) {
        BloodlineAccess.applyEvent(hunter, BloodlineEvent.KILLED_BY_PREY, SpeciesContext.of(hunter, null));
        if (killer instanceof ServerPlayer k) {
            HunterCurse.curse(k);
        }
        PotentialChooser.owe(hunter.server, hunter.getUUID());
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
