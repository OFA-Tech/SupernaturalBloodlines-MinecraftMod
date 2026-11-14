package net.ofatech.supernaturalbloodlines.parts.supernatural.task;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HumanLikeBlood;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

/**
 * The detectors of the ladders: NeoForge and Vampirism events, our own listeners, and the signals other parts fire through
 * {@link TaskSignals}. Each one only builds a {@link TaskEvent} for {@link TaskTracker}. Nothing scans the world: the one timer
 * runs every {@link TaskTracker#PERIOD} ticks per online player.
 */
public final class Detectors {
    private static boolean registered;
    private static final Set<UUID> DYING = ConcurrentHashMap.newKeySet();

    private Detectors() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        // a level reached or lost, or a faction joined or left: the race may have changed too
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p) {
                TaskTracker.recheck(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Detectors::onDrinkBlood);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Detectors::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Detectors::onDamaged);
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                TaskTracker.emit(p, TaskEvent.of(Detector.ITEM_USE, 1, "item", BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()));
            }
        });
        // species, rank, the Ghoul flag and so on: a state task may have become true
        BloodlineAccess.addListener((p, change) -> TaskTracker.recheck(p));
        // a new rung is active: what was already true counts at once
        SupernaturalAccess.addListener((p, change) -> {
            if (!change.before().rungs().equals(change.after().rungs())) {
                TaskTracker.recheck(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                TaskTracker.recheck(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                if (p.tickCount % TaskTracker.PERIOD == 0) {
                    TaskTracker.periodic(p);
                }
                checkDyingEnded(p);
            }
        });
        // Vampirism has no event for the dying state, so a player is watched from the moment it starts (a cancelled death while
        // isDBNO) to the moment it ends: only those players are checked each tick, nobody else
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (LivingDeathEvent e) -> {
            if (e.isCanceled() && e.getEntity() instanceof ServerPlayer p && VampirismAPI.vampirePlayer(p).isDBNO()) {
                DYING.add(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> DYING.remove(e.getEntity().getUUID()));
        TaskSignals.addListener(Detectors::onSignal);
    }

    /** The dying state of a player in Vampirism's DBNO is over: if they are alive they came back (R32). */
    private static void checkDyingEnded(ServerPlayer p) {
        if (!DYING.isEmpty() && DYING.contains(p.getUUID()) && !VampirismAPI.vampirePlayer(p).isDBNO()) {
            DYING.remove(p.getUUID());
            if (p.isAlive() && p.getHealth() > 0) {
                TaskSignals.fire(p, "dying_survived", 1);
            }
        }
    }

    /** A vampire drank from someone whose blood is human-like. */
    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        if (e.getVampire().getRepresentingEntity() instanceof ServerPlayer p && source != null && HumanLikeBlood.test(source)) {
            TaskTracker.emit(p, TaskEvent.of(Detector.FEED, 1));
        }
    }

    /** The killer is a player: one event for each category the victim belongs to. */
    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || e.getEntity() == killer) {
            return;
        }
        LivingEntity victim = e.getEntity();
        var faction = VampirismAPI.factionRegistry().getFaction(victim);
        if (Supernatural.isPrey(victim)) {
            TaskTracker.emit(killer, TaskEvent.of(Detector.KILL, 1, "category", "prey"));
        }
        if (faction == VReference.VAMPIRE_FACTION) {
            TaskTracker.emit(killer, TaskEvent.of(Detector.KILL, 1, "category", "vampire"));
        }
        if (faction == VReference.HUNTER_FACTION) {
            TaskTracker.emit(killer, TaskEvent.of(Detector.KILL, 1, "category", "hunter"));
        }
        if (victim instanceof AbstractVillager) {
            TaskTracker.emit(killer, TaskEvent.of(Detector.KILL, 1, "category", "villager"));
        }
    }

    /** A player took a hit from a vampire or werewolf type and is still standing. */
    private static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getNewDamage() > 0 && p.isAlive() && p.getHealth() > 0
                && e.getSource().getEntity() instanceof LivingEntity attacker && attacker != p && Supernatural.isPrey(attacker)) {
            TaskTracker.emit(p, TaskEvent.of(Detector.SIGNAL, 1, "signal", "prey_hit_survived"));
        }
    }

    /** What the other parts fire: rites as {@code rite:<id>:<role>}, everything else is a plain signal. */
    private static void onSignal(Player player, String id, long amount, Map<String, String> attrs) {
        if (!(player instanceof ServerPlayer p)) {
            return;
        }
        Map<String, String> all = new HashMap<>(attrs);
        if (id.startsWith("rite:")) {
            int split = id.lastIndexOf(':');
            all.put("rite", id.substring("rite:".length(), split));
            all.put("role", id.substring(split + 1));
            TaskTracker.emit(p, new TaskEvent(Detector.RITE, all, amount));
        } else {
            all.put("signal", id);
            TaskTracker.emit(p, new TaskEvent(Detector.SIGNAL, all, amount));
        }
    }
}
