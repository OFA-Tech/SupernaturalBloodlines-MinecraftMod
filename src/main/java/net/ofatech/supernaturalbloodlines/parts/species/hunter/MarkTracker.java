package net.ofatech.supernaturalbloodlines.parts.species.hunter;

import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import org.jetbrains.annotations.VisibleForTesting;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
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
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;

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
        // Mark Mastery (S-HUN-5) changes the rate per kill
        SupernaturalAccess.addListener((p, change) -> {
            if (!change.before().skills().equals(change.after().skills())) {
                sync(p);
            }
        });
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
    public static boolean isPrey(Entity e) {
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
    @VisibleForTesting
    public static Entity killerOf(LivingEntity victim, DamageSource source) {
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
        DamageSource source = e.getSource();
        // the world dealt it with no attacker (fire, lava, a fall, poison, drowning); the void and /kill are never anyone's
        boolean environmental = source.getEntity() == null && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
        Entity killer = killerOf(victim, source);
        // a sireline death is the Original's, not the credit of whoever hurt the victim last
        if (killer == victim || source.is(LineDeath.DAMAGE_TYPE) || killer == null && !environmental) {
            return;
        }
        // read before the mark is taken away: the species is gone after killedMarkedHunter
        boolean victimWasMarked = victim instanceof ServerPlayer marked && BloodlineAccess.get(marked).species() == Species.HUNTER_OF_THE_FIVE;
        if (killer instanceof ServerPlayer k && isPrey(victim) && !samePack(k, victim)) {
            killedPrey(k);
        }
        Resolved resolved = new Resolved(victim, killer, victimWasMarked, killer != null && source.getEntity() == killer, environmental);
        // the one who costs a marked hunter the mark is the death's culprit, as for the Code; a brother of the same brotherhood is no exception (R82, R295)
        Entity marker = culprit(resolved);
        if (marker != null && victimWasMarked && victim instanceof ServerPlayer v && isPrey(marker) && !(samePack(v, marker) && !sameBrotherhood(v, marker))) {
            killedMarkedHunter(v, marker);
        }
        for (Consumer<Resolved> hook : RESOLVED) {
            try {
                hook.accept(resolved);
            } catch (RuntimeException failed) {
                SupernaturalBloodlines.LOGGER.error("A hook of the kill feed failed", failed);
            }
        }
    }

    /**
     * The one a death is put on (R82, R294, R295): whoever dealt the killing blow (a mob too), or the online owner of the tamed pet that did; for a death the
     * world dealt (fire, lava, a fall, poison, wither, drowning), the player whose direct melee hit last landed on the victim in the last 10 seconds, never the
     * kill credit, which a later snowball can steal. The void, /kill and credit alone are nobody's, and so is a death by your own pet. The hunter's own gain
     * (killedPrey) stays on the credit.
     */
    public static Entity culprit(Resolved r) {
        Entity e = r.environmental() ? lastStriker(r.victim()) : r.killingBlow() ? ownerOrSelf(r.killer()) : null;
        return e == r.victim() ? null : e;
    }

    private static boolean sameBrotherhood(ServerPlayer a, Entity b) {
        var mine = net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodScope.of(a.serverLevel(), a.getUUID());
        var theirs = net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodScope.of(a.serverLevel(), b.getUUID());
        return mine != null && theirs != null && mine.group().equals(theirs.group());
    }

    /** The player whose direct melee hit last landed on {@code victim} in the last 10 seconds (FightLedger), if online, or null. */
    public static ServerPlayer lastStriker(LivingEntity victim) {
        UUID striker = net.ofatech.supernaturalbloodlines.parts.species.pack.FightLedger.lastMeleeStriker(victim.getUUID(),
                net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.now());
        return striker == null || victim.getServer() == null ? null : net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.player(victim.getServer(), striker);
    }

    /** The online owner of a tamed pet, or the entity itself (null stays null). */
    public static Entity ownerOrSelf(Entity e) {
        if (e instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwnerUUID() != null && e.getServer() != null) {
            ServerPlayer owner = net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.player(e.getServer(), pet.getOwnerUUID());
            return owner != null ? owner : e;
        }
        return e;
    }
    /**
     * One death that counted for somebody: who died, who gets the credit (the same attribution the mark uses), whether the victim was a Hunter of the Five,
     * whether the killer dealt the killing blow, and whether the world did with no attacker at all (fire, lava, a fall, poison, drowning; not the void or /kill). The killer (kill credit) can be null for such a death.
     */
    public record Resolved(LivingEntity victim, Entity killer, boolean victimWasMarked, boolean killingBlow, boolean environmental) {
    }

    private static final List<Consumer<Resolved>> RESOLVED = new CopyOnWriteArrayList<>();

    /** The kill feed: each hook sees every death the mark sees (a real death, never a cancelled one), after the mark and the curse have been handled, in the order added. */
    public static void onDeathResolved(Consumer<Resolved> hook) {
        RESOLVED.add(hook);
    }

    /**
     * Two players of one pack (plan 14), or who left it a moment ago: a kill between them gives no mark and no curse, in either direction. A marked hunter's
     * brotherhood is not such a group: a brother who kills him still costs the mark (R82, R295).
     */
    static boolean samePack(Entity a, Entity b) {
        return net.neoforged.fml.ModList.get().isLoaded("werewolves") && net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.samePack(a, b);
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
            // this curse ends only at an awakening (R83): a timed brotherhood curse the killer carried is replaced
            if (BloodlineAccess.get(k).hunterCurse()) {
                net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.get(k.serverLevel()).clearCurse(k.getUUID());
            }
        }
        PotentialChooser.owe(hunter.server, hunter.getUUID());
    }

    /** Mark Mastery (S-HUN-5) gives the bonus at {@link #CAP} kills (+25% attack, +12.5% speed); the rate per kill is that over the cap. */
    @VisibleForTesting
    public static double perKill(ServerPlayer p, String key, double base) {
        double atCap = LadderQuery.value(p, "S-HUN-5", key, Double.NaN);
        return Double.isNaN(atCap) ? base : atCap / CAP;
    }

    /** Idempotent: makes the perks match the player's mark. */
    public static void sync(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        int kills = s.species() == Species.HUNTER_OF_THE_FIVE ? Math.min(s.markKills(), CAP) : 0;
        apply(p, Attributes.ATTACK_DAMAGE, kills * perKill(p, "mark_attack", ATTACK_PER_KILL), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        apply(p, Attributes.MOVEMENT_SPEED, kills * perKill(p, "mark_other", SPEED_PER_KILL), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
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
